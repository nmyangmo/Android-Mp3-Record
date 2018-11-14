package com.yangmo.mp3recorder;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Created by Administrator on 2017/4/22.
 */

public class Mp3RecorderUtil {

    public static Context getContext() {
        return context;
    }

    private static  Context context;

     static boolean isDebug() {
        return isDebug;
    }

    private static  boolean isDebug;
    private static Handler mainHandler;

    public static void init(Context context ,boolean isDebug){
        Mp3RecorderUtil.context = context;
        Mp3RecorderUtil.isDebug = isDebug;
        mainHandler = getMainHandler();
    }

     static Handler getMainHandler() {
        if(mainHandler ==null){
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

     static void postTaskSafely(Runnable runnable){
        getMainHandler().post(runnable);
    }

     static void showDebugToast(final String msg){
        if(!isDebug){
            return;
        }
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,msg,Toast.LENGTH_SHORT).show();
            }
        });
    }






}
