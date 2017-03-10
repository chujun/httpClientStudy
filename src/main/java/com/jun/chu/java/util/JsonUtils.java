package com.jun.chu.java.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

/**
 * Created by chujun on 2017/3/10.
 */
public class JsonUtils {
    Gson gson = new Gson();

    /**
     * 对象转化为json对象
     *
     * @param t
     * @param <T>
     * @return
     */
    public <T> String toJson(T t) {
        return gson.toJson(t);
    }

    /**
     * json字符串转化为指定类型对象
     *
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T fromJson(String str, Class<T> clazz) {
        return gson.fromJson(str, clazz);
    }

    /**
     * json字符串创转化为指定类型对象列表
     *
     * @param jsonStr
     * @param tClass
     * @param <T>
     * @return
     */
    public <T> List<T> fromJsonList(String jsonStr, Class<T> tClass) {
        return gson.fromJson(jsonStr, new TypeToken<List<T>>() {
        }.getType());
    }
}
