package spw.audiorecordlib.util;

import android.util.Log;

/**
 * Created by spw on 2016/5/31.
 */
public class Logger {

    private static boolean debug = true;

    public static void setDubug(boolean debug){
        Logger.debug = debug;
    }
    public static final String DEFAULT_LOG_TAG = "audio_recorder_ex";

    public static void d(String msg){
        d(DEFAULT_LOG_TAG,msg);
    }

    public static void d(String tag,String msg){
        if(debug){
            Log.d(tag, msg);
        }
    }

    public static void i(String msg){
         i(DEFAULT_LOG_TAG,msg);
    }

    public static void i(String tag,String msg){
        if(debug){
            Log.i(tag,msg);
        }
    }

    public static void e(String msg){
        e(DEFAULT_LOG_TAG,msg);
    }


    public static void e(Throwable tr){
        e(DEFAULT_LOG_TAG,tr);
    }

    public static void e(String tag,String msg){
        if(debug){
            Log.e(tag, msg);
        }
    }

    public static void e(String tag,Throwable tr){
        if(debug){
            Log.e(tag,DEFAULT_LOG_TAG,tr);
        }
    }
}
