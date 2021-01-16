package com.ijson.config.base;


import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.ijson.config.base.ConfigConstants.UTF8;

/**
 * @author cuiyongxu
 */
public class Config extends Properties {

    public static final Logger log = LoggerFactory.getLogger(Config.class);

    private boolean parsed = false;
    private byte[] content;

    public synchronized byte[] getContent() {
        if (content == null) {
            Map<String, String> m = getAll();
            if (m.isEmpty()) {
                content = new byte[0];
            } else {
                StringBuilder sbd = new StringBuilder();
                for (Map.Entry<String, String> i : m.entrySet()) {
                    sbd.append(i.getKey()).append('=').append(i.getValue()).append('\n');
                }
                content = sbd.toString().getBytes(UTF8);
            }
        }
        return content;
    }

    public void copyOf(String s) {
        this.content = s.getBytes(UTF8);
        parsed = false;
    }

    public void copyOf(byte[] content) {
        this.content = content;
        parsed = false;
    }

    @Override
    public void copyOf(Map<String, String> m) {
        super.copyOf(m);
        resetContent();
    }

    @Override
    public void copyOf(java.util.Properties props) {
        super.copyOf(props);
        resetContent();
    }

    @Override
    public Properties putAll(Map<String, String> items) {
        super.putAll(items);
        resetContent();
        return this;
    }

    @Override
    public Properties putAll(java.util.Properties props) {
        super.putAll(props);
        resetContent();
        return this;
    }

    private void resetContent() {
        parsed = true;
        this.content = null;
    }

    private synchronized void parse() {
        if (!parsed) {
            Map<String, String> m = Maps.newLinkedHashMap();
            final byte[] bytes = content;
            if (bytes != null) {
                String txt = new String(bytes, UTF8);
                for (String i : lines(txt, true)) {
                    int pos = i.indexOf('=');
                    if (pos != -1) {
                        String k = i.substring(0, pos).trim();
                        int next = pos + 1;
                        if (next < i.length()) {
                            try {
                                m.put(k, unEscapeJava(i.substring(next).trim()));
                            } catch (Exception e) {
                                log.error("cannot escape:{}, content={}", i, content);
                            }
                        } else {
                            m.put(k, "");
                        }
                    }
                }
                super.copyOf(m);
            }
            parsed = true;
        }
    }

    /**
     * copyFrom StringEscapeUtils.unescapeJava
     * @param value
     * @return
     */
    private String unEscapeJava(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        StringBuilder buf = null;
        int len = value.length();
        int len1 = len - 1;
        for (int i = 0; i < len; i++) {
            char ch = value.charAt(i);
            if (ch == '\\' && i < len1) {
                int j = i;
                i++;
                ch = value.charAt(i);
                switch (ch) {
                    case '\\':
                        ch = '\\';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'u':
                    case 'U':
                        ch = (char) Integer.parseInt(value.substring(i + 1, i + 5), 16);
                        i = i + 4;
                        break;
                    default:
                        j--;
                }
                if (buf == null) {
                    buf = new StringBuilder(len);
                    if (j > 0) {
                        buf.append(value.substring(0, j));
                    }
                }
                buf.append(ch);
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        if (buf != null) {
            return buf.toString();
        }
        return value;
    }

    @Override
    public String get(String key) {
        if (!parsed) {
            parse();
        }
        return super.get(key);
    }

    @Override
    public Map<String, String> getAll() {
        if (!parsed) {
            parse();
        }
        return super.getAll();
    }

    public String getString() {
        return new String(getContent(), UTF8);
    }

    public String getString(Charset charset) {
        return new String(getContent(), charset);
    }

    public List<String> getLines() {
        return getLines(UTF8, true);
    }

    public List<String> getLines(Charset charset) {
        return lines(new String(getContent(), charset), true);
    }

    public List<String> getLines(Charset charset, boolean removeComment) {
        return lines(new String(getContent(), charset), removeComment);
    }

    private List<String> lines(String s, boolean removeComment) {
        List<String> raw = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(s);
        if (!removeComment) {
            return raw;
        }

        List<String> clean = Lists.newArrayList();
        for (String i : raw) {
            if (i.charAt(0) == '#' || i.startsWith("//")) {
                continue;
            }
            clean.add(i);
        }
        return clean;
    }
}
