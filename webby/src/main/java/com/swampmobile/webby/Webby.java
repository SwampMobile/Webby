package com.swampmobile.webby;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import retrofit.RestAdapter;

/**
 * Webby system configuration object.
 */
public class Webby
{
    private static RestAdapter.LogLevel retrofitLogLevel = RestAdapter.LogLevel.NONE;
    private static final Bus bus = new Bus(ThreadEnforcer.ANY);

    public static RestAdapter.LogLevel getRetrofitLogLevel()
    {
        return retrofitLogLevel;
    }

    public static void setRetrofitLoggingLevel(RestAdapter.LogLevel retrofitLogLevel)
    {
        Webby.retrofitLogLevel = retrofitLogLevel;
    }

    public static Bus getBus()
    {
        return bus;
    }
}
