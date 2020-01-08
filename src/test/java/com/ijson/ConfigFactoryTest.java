package com.ijson;

import com.ijson.config.ConfigFactory;

/**
 * Created by cuiyongxu on 17/8/26.
 */
public class ConfigFactoryTest {

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Thread.sleep(1000);
            ConfigFactory.getConfig("fs-workflow-rest-proxy", (value) -> {
                System.out.println(">>>>>>>>>>>>" + value.getString());
            });
        }

    }

}
