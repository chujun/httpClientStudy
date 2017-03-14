package com.jun.chu.java.pool;

import org.apache.http.pool.PoolStats;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by chujun on 2017/3/10.
 */
public class PoolingHttpClientManagerTest {
    final static Logger logger = Logger.getLogger(PoolingHttpClientManagerTest.class);
    private static final PoolingHttpClientManager cm = new PoolingHttpClientManager(10, 3000, 2000);

    @Test
    public void case01() {
        PoolStats totalStats = cm.getTotalStats();
        logger.info(totalStats);
    }

    @Test
    public void case02_multi_thread_test() {
        //1.启动监视线程
        startMonitorThread();
        //2.启动多线程
        int count = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < count; i++) {
            executorService.execute(() -> {
                sendGetRequest();
            });
        }

        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendGetRequest() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String response = HttpUtils.doGetWithRequestParams(cm.getHttpClient(), getRandomGetRequest(), null);
        System.out.println(response);
    }

    private static String getRandomGetRequest() {
        String[] pages = new String[]{
                "https://www.baidu.com/",
                "http://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/connmgmt.html#d5e431",
                "http://192.168.8.28:28106/#ytyy"
        };
        return pages[new Random().nextInt(pages.length)];
    }

    private void startMonitorThread() {
        new Thread(() -> {
            while (true) {
                logger.info(cm.getTotalStats());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
