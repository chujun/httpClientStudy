package com.jun.chu.java.pool;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.IdleConnectionEvictor;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Created by jun.chu on 16/8/23.
 * 支持http,https协议
 */
public class PoolingHttpClientManager {
    final static Logger logger = Logger.getLogger(PoolingHttpClientManager.class);
    private static PoolingHttpClientConnectionManager cm = null;
    private CloseableHttpClient client;

    /**
     * 密钥库路径,和tomcat中配置的https中的keystoreFile相同
     */
    private String keyStorePath = "./src/main/resources/crt/tomcat.keystore";

    /**
     * 密钥库秘钥,和tomcat中配置的https中的keystorePass相同
     */
    private String keyStorePass = "tomcat";

    //守护线程清理关闭闲置连接
    private IdleConnectionEvictor idleConnectionEvictor = null;

    private void initPoolingHttpClientManager(boolean isSSLAuth) {
        //默认
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();

        //ConnectionSocketFactory sslsf = SSLConnectionSocketFactory.getSocketFactory();
        ConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(createSSLContext(isSSLAuth));
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", plainsf)
                .register("https", sslsf)
                .build();
        cm = new PoolingHttpClientConnectionManager(
                registry);
    }

    /**
     * TODO:cj to be tested
     *
     * @param isSSLAuth
     * @return
     */
    private ConnectionSocketFactory createConnectionSocketFactory(boolean isSSLAuth) {
        return isSSLAuth ? new SSLConnectionSocketFactory(createSSLContextWithSSLAuth()) :
                new SSLConnectionSocketFactory(createSSLContextWithoutSSLAuth());

    }

    private SSLContext createSSLContext(boolean isSSLAuth) {
        return isSSLAuth ? createSSLContextWithSSLAuth() : createSSLContextWithoutSSLAuth();
    }

    /**
     * 创建绕过证书验证的SSLContext
     * @return
     */
    private SSLContext createSSLContextWithoutSSLAuth() {
        SSLContext sslContext = null;
        try {
            //SSL,SSLv3
            sslContext = SSLContext.getInstance("SSLv3");
            sslContext.init(null, new TrustManager[]{new MyX509TrustManager()}, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("初始化SSLContext失败", e);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            throw new RuntimeException("初始化SSLContext失败", e);
        }
        return sslContext;

    }

    /**
     * 创建证书自验证的SSLContext
     *
     * @return
     */
    private SSLContext createSSLContextWithSSLAuth() {
        FileInputStream fileInputStream = null;
        try {
            KeyStore trustedStore = KeyStore.getInstance(KeyStore.getDefaultType());
            fileInputStream = new FileInputStream(keyStorePath);
            //相信自己的CA和所有自签名的证书
            trustedStore.load(fileInputStream, keyStorePass.toCharArray());
            return SSLContexts.custom().loadTrustMaterial(trustedStore, new TrustSelfSignedStrategy()).build();
        } catch (KeyStoreException e) {
            e.printStackTrace();
            throw new RuntimeException("create keyStore failed", e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(keyStorePath, e);
        } catch (CertificateException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (null != fileInputStream) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public PoolingHttpClientManager(int maxConnTotal, int soTimeout, int connectionTimeout) {
        this(maxConnTotal, soTimeout, connectionTimeout, false);
    }

    public PoolingHttpClientManager(int maxConnTotal, int soTimeout, int connectionTimeout, boolean isSSLAuth) {
        initPoolingHttpClientManager(isSSLAuth);
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
        //TODO:cj to be confirmed needed 清理空闲连接守护进程启动
        startIdleConnectionEvictor();
    }

    private void startIdleConnectionEvictor() {
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

    /**
     * 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
     */
    static class MyX509TrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            return;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            return;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
