package com.jun.chu.java.pool;

import org.apache.http.pool.PoolStats;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Created by chujun on 2017/3/10.
 */
public class PoolingHttpClientManagerTest {
    final static Logger logger = Logger.getLogger(PoolingHttpClientManagerTest.class);
    private static final PoolingHttpClientManager cm = new PoolingHttpClientManager(100, 3000, 2000);

    @Test
    public void case01() {
        PoolStats totalStats = cm.getTotalStats();
        logger.info(totalStats);
    }

    @Test
    public void case02() {
        startMonitorThread();
        int count = 300;
        for (int i = 0; i < count; i++) {
            sendGetRequest();
        }
    }

    private void sendGetRequest() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String baiduPage = "https://www.baidu.com/";
        HttpUtils.doGetWithRequestParams(cm.getHttpClient(), baiduPage, null);
    }

    private void startMonitorThread() {
        new Thread(() -> {
            while (true) {
                logger.info(cm.getTotalStats());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
