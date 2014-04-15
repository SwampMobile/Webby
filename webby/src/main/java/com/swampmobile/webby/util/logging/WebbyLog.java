package com.swampmobile.webby.util.logging;

import android.util.Log;

/**
 * Created by Matt on 4/14/14.
 */
public class WebbyLog
{
    private static LogLevel currentLogLevel = LogLevel.VERBOSE;

    public static enum LogLevel
    {
        VERBOSE(50),
        DEBUG(40),
        INFO(30),
        WARN(20),
        ERROR(10),
        WTF(0);

        private int logLevel;

        private LogLevel(int logLevel)
        {
            this.logLevel = logLevel;
        }

        public boolean shouldLog()
        {
            return currentLogLevel.logLevel >= this.logLevel;
        }
    }

    public static void setLogLevel(LogLevel logLevel)
    {
        currentLogLevel = logLevel;
    }

    public static void v(String tag, String msg)
    {
        if(LogLevel.VERBOSE.shouldLog())
            Log.v(tag, msg);
    }

    public static void d(String tag, String msg)
    {
        if(LogLevel.DEBUG.shouldLog())
            Log.d(tag, msg);
    }

    public static void i(String tag, String msg)
    {
        if(LogLevel.INFO.shouldLog())
            Log.i(tag, msg);
    }

    public static void w(String tag, String msg)
    {
        if(LogLevel.WARN.shouldLog())
            Log.w(tag, msg);
    }

    public static void e(String tag, String msg)
    {
        if(LogLevel.ERROR.shouldLog())
            Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable e)
    {
        if(LogLevel.ERROR.shouldLog())
            Log.e(tag, msg, e);
    }

    public static void wtf(String tag, String msg)
    {
        if(LogLevel.WTF.shouldLog())
            Log.wtf(tag, msg);
    }

}
