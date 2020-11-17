package com.ijson;

import com.ijson.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cuiyongxu on 17/8/26.
 */
public class ConfigFactoryTest {

    public static final Logger log = LoggerFactory.getLogger(ConfigFactoryTest.class);


    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            //Thread.sleep(1000);
            ConfigFactory.getConfig("fs-workflow-rest-proxy", (value) -> {
                log.info("{}" , value.getString());
            });
        }

    }

}
