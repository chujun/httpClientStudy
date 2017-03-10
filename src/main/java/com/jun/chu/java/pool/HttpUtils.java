package com.jun.chu.java.pool;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author liuyu.lu
 * @since Mar 9, 2016
 */
public class HttpUtils {

    final static Logger logger = Logger.getLogger(HttpUtils.class);


    private static final String APPLICATION_JSON = "application/json";

    public static String doPostWithClient(CloseableHttpClient httpClient, String url, Map<String, Object> paramsMap)
            throws ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
        List<NameValuePair> params = new ArrayList<NameValuePair>(paramsMap.size());
        paramsMap.forEach((key, value) -> {
            params.add(new BasicNameValuePair(key, value.toString()));
        });
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                return EntityUtils.toString(response.getEntity());
            } else {
                return null;
            }
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    //logger.error("关闭Http出错url={},jsonParam={},msg={}", url, JsonUtils.toJson(paramsMap), e);
                } finally {
                    response = null;
                }
            }
        }
    }

    /**
     * http请求参数为json格式
     *
     * @param client
     * @param url
     * @param jsonParam
     * @param headers
     * @return
     */
    public static String doPostWithJsonRequestParams(CloseableHttpClient client, String url, String jsonParam, Header[] headers) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setProtocolVersion(HttpVersion.HTTP_1_1);
        if (null != headers && headers.length > 0) {
            //自定义请求头Header
            httpPost.setHeaders(headers);
        }
        //Json格式
        httpPost.addHeader(HTTP.CONTENT_TYPE, APPLICATION_JSON);
        // 设置请求参数
        StringEntity entity = new StringEntity(jsonParam, "utf-8");
        httpPost.setEntity(entity);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                return EntityUtils.toString(response.getEntity());
            } else {
                EntityUtils.consume(response.getEntity());
                logger.error(String.format("处理Http结果出错url=%s,jsonParam=%s", url, jsonParam));
                //throw MessageConstants.http_status_error;
                throw new RuntimeException("处理Http结果出错url");
            }
        } catch (IOException e) {
            logger.error(String.format("请求Http接口出错url=%s,jsonParam=%s", url, jsonParam), e);
            //throw MessageConstants.http_request_error;
            throw new RuntimeException("请求Http接口出错");
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    logger.error(String.format("关闭Http出错url=%s,jsonParam=%s,msg=%s", url, jsonParam, e.getMessage()), e);
                } finally {
                    response = null;
                }
            }
        }

    }

    /**
     * http get请求
     *
     * @param client
     * @param url
     * @param headers
     * @return
     */
    public static String doGetWithRequestParams(CloseableHttpClient client, String url, Header[] headers) {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setProtocolVersion(HttpVersion.HTTP_1_1);
        if (null != headers && headers.length > 0) {
            //自定义请求头Header
            httpGet.setHeaders(headers);
        }
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpGet);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                return EntityUtils.toString(response.getEntity());
            } else {
                EntityUtils.consume(response.getEntity());
                logger.error(String.format("处理Http结果出错url=%s", url));
                //throw MessageConstants.http_status_error;
                throw new RuntimeException("处理Http结果出错url");
            }
        } catch (IOException e) {
            logger.error(String.format("请求Http接口出错url=%s", url), e);
            //throw MessageConstants.http_request_error;
            throw new RuntimeException("请求Http接口出错url");
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    logger.error(String.format("关闭Http出错url=%s,msg=%s", url, e.getMessage()), e);
                } finally {
                    response = null;
                }
            }
        }
    }
}
