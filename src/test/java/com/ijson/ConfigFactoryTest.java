package com.ijson;

import com.ijson.config.ConfigFactory;
import org.junit.Test;

/**
 * Created by cuiyongxu on 17/8/26.
 */
public class ConfigFactoryTest {


    //private static ILogger log = ILogger.getLogger(ConfigFactoryTest.class);

    @Test
    public void getConfigName() {
        ConfigFactory.getConfig("ddddddd").getName();
    }


    @Test
    public void getConfigContent() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1000);
            ConfigFactory.getConfig("fs-workflow-rest-proxy", (value) -> {
                System.out.println(">>>>>>>>>>>>"+value.getString());
            });
        }
    }

}
