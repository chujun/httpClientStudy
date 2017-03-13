package com.jun.chu.java.pool;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.IdleConnectionEvictor;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by IFT8 on 16/8/23.
 */
public class PoolingHttpClientManager {
    final static Logger logger = Logger.getLogger(PoolingHttpClientManager.class);
    private static PoolingHttpClientConnectionManager cm = null;
    private CloseableHttpClient client;

    //守护线程清理关闭闲置连接
    private IdleConnectionEvictor idleConnectionEvictor = null;

    private void init() {
        //默认
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory
                .getSocketFactory();
        LayeredConnectionSocketFactory sslsf = SSLConnectionSocketFactory
                .getSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        cm = new PoolingHttpClientConnectionManager(
                registry);
    }

    public PoolingHttpClientManager(int maxConnTotal, int soTimeout, int connectionTimeout) {
        init();
        //设置连接数
        setCMMaxConnTotal(maxConnTotal);

        //默认配置
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                //服务器返回数据(response)的时间
                .setSocketTimeout(soTimeout)
                //连接上服务器(握手成功)的时间
                .setConnectTimeout(connectionTimeout)
                //从连接池中获取连接的超时时间，超过该时间未拿到可用连接
                .setConnectionRequestTimeout(connectionTimeout)
                .build();

        client = getHttpClient(defaultRequestConfig);
        //守护进程启动
        idleConnectionEvictor = new IdleConnectionEvictor(cm, 5, TimeUnit.SECONDS);
        idleConnectionEvictor.start();
    }

    public CloseableHttpClient getHttpClient() {
        return client;
    }

    public PoolStats getTotalStats() {
        return cm.getTotalStats();
    }

    public void shutDown() {
        cm.shutdown();
    }


    private void setCMMaxConnTotal(int maxConnTotal) {
        //设置连接数
        cm.setMaxTotal(maxConnTotal);
        //路由最大连接
        cm.setDefaultMaxPerRoute(maxConnTotal);
    }

    private CloseableHttpClient getHttpClient(RequestConfig requestConfig) {
        //Socket Config
        SocketConfig defaultSocketConfig = SocketConfig.custom()
                .setSoKeepAlive(true)
                .setTcpNoDelay(true)
                .setSoReuseAddress(true)
                .build();

        //HTTPv1.1 在请求时候设置

        return HttpClients.custom()
                .setConnectionManager(cm)
                //请求配置
                .setDefaultRequestConfig(requestConfig)
                //socket配置
                .setDefaultSocketConfig(defaultSocketConfig)
                .build();
    }


}
