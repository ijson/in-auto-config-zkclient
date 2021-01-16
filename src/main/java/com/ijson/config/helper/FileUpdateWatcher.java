package com.ijson.config.helper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ijson.config.api.IFileListener;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

import static java.nio.charset.CoderResult.OVERFLOW;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * @author *
 */
public class FileUpdateWatcher implements Runnable, AutoCloseable {


    public static final Logger log = LoggerFactory.getLogger(FileUpdateWatcher.class);


    /**
     * 同一个目录下会包含多个文件,每个文件又有多个listener
     */
    private final Map<Path, Multimap<Path, IFileListener>> watches = Maps.newConcurrentMap();
    private final Map<Path, Long> masks = Maps.newConcurrentMap();
    private WatchService watchService;
    private Thread thread;


    private FileUpdateWatcher() {
            try {
                watchService = FileSystems.getDefault().newWatchService();
            } catch (IOException e) {
                log.error("cannot build watchService ", e);
            }
        }

        public static FileUpdateWatcher getInstance () {
            return LazyHolder.INSTANCE;
        }

        public void mask (Path path){
            masks.put(path, System.currentTimeMillis());
        }

        public void watch (Path path, IFileListener listener){
            Path parent = path.getParent();
            Multimap<Path, IFileListener> files = watches.get(parent);
            if (files == null) {
                try {
                    WatchEvent.Kind[] events = {ENTRY_MODIFY, ENTRY_DELETE};
                    parent.register(watchService, events, SensitivityWatchEventModifier.HIGH);
                    log.info("monitor directory {}", parent);
                } catch (IOException e) {
                    log.error("cannot register path:{}  {}", parent, e);
                }
                files = ArrayListMultimap.create();
                watches.put(parent, files);
            }
            log.debug("watch {}, {}", path, listener);
            files.put(path, listener);
        }

        public void start () {
            if (thread == null) {
                thread = new Thread(this, "LocalFileUpdateWatcher");
                thread.setDaemon(true);
                thread.start();
            }
        }

        @Override
        public void run () {
            while (true) {
                WatchKey key = null;
                try {
                    key = watchService.poll(3, TimeUnit.SECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind kind = event.kind();
                            if (kind == OVERFLOW) {
                                continue;
                            }
                            Path base = (Path) key.watchable();
                            WatchEvent<Path> ev = cast(event);
                            Path context = ev.context();
                            Path child = base.resolve(context);
                            log.info("{}, {}", kind, child);
                            //屏蔽只剩小1秒钟,避免误封禁
                            Long stamp = masks.remove(child);
                            if (stamp != null && System.currentTimeMillis() - stamp < 1000) {
                                log.info("mask {}", child);
                                continue;
                            }

                            //屏蔽一会,避免频繁加载
                            mask(child);
                            Collection<IFileListener> listeners = watches.get(base).get(child);
                            if (listeners == null || listeners.isEmpty()) {
                                continue;
                            }
                            //配置文件内容都不大,所以这里就读出来,不用每个listener再分别读取了
                            byte[] content = new byte[0];
                            if (child.toFile().exists()) {
                                //在linux环境下修改文件会触发多次,而且首次可能读取不到文件内容,所以等待一段时间再读数据
                                Thread.sleep(200);
                                content = Files.readAllBytes(child);
                            }
                            for (IFileListener i : listeners) {
                                i.changed(child, content);
                            }
                        }
                    }
                } catch (InterruptedException x) {
                    log.info("{} was interrupted, now EXIT", Thread.currentThread().getName());
                    try {
                        watchService.close();
                    } catch (IOException ignored) {
                    }
                    log.info(FileUpdateWatcher.class.getSimpleName() + " exited");
                    return;
                } catch (Exception e) {
                    log.error("watches: {}  {}", watches.keySet(), e);
                } finally {
                    if (key != null) {
                        key.reset();
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private <T > WatchEvent < T > cast(WatchEvent < ? > event){
            return (WatchEvent<T>) event;
        }

        @Override
        public void close () throws Exception {
            if (thread != null) {
                thread.interrupt();
            }
        }

        private static final class LazyHolder {
            private static final FileUpdateWatcher INSTANCE = create();

            private static FileUpdateWatcher create() {
                FileUpdateWatcher watcher = new FileUpdateWatcher();
                // 增加jvm的退出回调功能
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.warn("jvm exit, now try stop {} ", FileUpdateWatcher.class.getSimpleName());
                    try {
                        INSTANCE.close();
                    } catch (Exception e) {
                        log.error("cannot stop {}  {}", FileUpdateWatcher.class.getSimpleName(), e);
                    }
                }));
                watcher.start();
                return watcher;
            }
        }
    }
