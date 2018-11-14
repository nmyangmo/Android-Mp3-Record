package com.yangmo.mp3recorder.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.yangmo.mp3recorder.Mp3RecorderUtil;

/**
 * Created by Administrator on 2017/4/27 0027.
 */

public class SP {
    public static final String CONFIG_FILE = "mp3recoder";
    /**
     * @param key
     *        关键字
     * @param value
     *       对应的值
     */
    public static void setBoolean(String key,boolean value){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        sp.edit().putBoolean(key, value).apply();//提交保存键值对
    }

    /**

     * @param key
     *        关键字
     * @param defValue
     *        设置的默认值
     * @return
     */
    public static boolean getBoolean(String key,boolean defValue){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        return sp.getBoolean(key, defValue);
    }

    /**
     * @param key
     *        关键字
     * @param value
     *       对应的值
     */
    public static void setString(String key,String value){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).apply();//提交保存键值对

    }

    /**

     * @param key
     *        关键字
     * @param defValue
     *        设置的默认值
     * @return
     */
    public static String getString(String key,String defValue){


        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        return sp.getString(key, defValue);
    }


    public static Long getLong(String key,Long defValue){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        return sp.getLong(key, defValue);
    }

    public static void setLong(String key,Long value){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        sp.edit().putLong(key, value).apply();//提交保存键值对
    }

    public static int getInt(String key,int defValue){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        return sp.getInt(key, defValue);
    }

    public static void setInt(String key,int value){
        SharedPreferences sp = Mp3RecorderUtil.getContext().getSharedPreferences(CONFIG_FILE, Context.MODE_PRIVATE);
        sp.edit().putInt(key, value).apply();//提交保存键值对
    }

}
