package com.swampmobile.webby;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.swampmobile.webby.requests.WebbyRequest;
import com.swampmobile.webby.services.WebbyService;
import com.swampmobile.webby.util.logging.WebbyLog;

import java.util.LinkedHashSet;

/**
 * WebbyManager is the middleman between a client which wants to submit REST requests and the {@link
 * com.swampmobile.webby.services.WebbyService} that is responsible for executing those requests.
 * <p/>
 * A WebbyManager must be started and stopped as needed using {@code onStart()} and {@code onStop()}
 * respectively.  These methods are intended for use with Activitys and Fragments which have
 * analogous methods, but any object can call these methods as desired.
 * <p/>
 * When the WebbyManager is started, it will connect the client to the {@link com.squareup.otto.Bus}
 * that dispatches {@link com.swampmobile.webby.requests.WebbyResponse} events.  When WebbyManager
 * is stopped, those events will not be send to the client.
 */
public class WebbyManager
{
    private static final String TAG = "WebbyManager";

    private Context context; // should this be weak?
    private Object busListener; // should this be weak?

    private boolean webbyStarted = false;

    private WebbyService webbyService;
    private ServiceConnection serviceConnection = new WebbyManagerServiceConnection();
    private boolean isBound = false;

    // While service is binding, we need to aggregate requests that are being added.
    // Once the service is bound, we will send over these requests.
    private LinkedHashSet<WebbyRequest> unsentRequestQueue; // chose LinkedHashSet because it prevents duplicates but also preserves insertion order (like a queue)


    public WebbyManager(Context context, Object busListener)
    {
        this.context = context;
        this.busListener = busListener;

        unsentRequestQueue = new LinkedHashSet<WebbyRequest>();
    }

    public synchronized void onStart()
    {
        if (webbyStarted)
            return;

        WebbyLog.d(TAG, "onStart()");
        webbyStarted = true;

        Webby.getBus().register(busListener);

        Intent webbyServiceIntent = new Intent(context, WebbyService.class);
        context.startService(webbyServiceIntent);
        context.bindService(webbyServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public synchronized void onStop()
    {
        if (!webbyStarted)
            return;

        WebbyLog.d(TAG, "onStop()");
        webbyStarted = false;

        Webby.getBus().unregister(busListener);

        if (isBound)
            context.unbindService(serviceConnection);
    }

    public void addRequest(WebbyRequest request)
    {
        if (!webbyStarted)
            return;

        if (webbyService != null && isBound)
        {
            WebbyLog.d(TAG, "Adding request to service");
            webbyService.addRequest(request);
        }
        else
        {
            WebbyLog.d(TAG, "Queue'ing request until service is bound");
            unsentRequestQueue.add(request);
        }
    }

    /**
     * If requests were accumulated while waiting to bind to the WebbyService, those requests are
     * now sent to the service.
     */
    private void sendUnsentRequests()
    {
        if (!isBound)
            return;

        for (WebbyRequest req : unsentRequestQueue)
        {
            webbyService.addRequest(req);
        }
    }

    private class WebbyManagerServiceConnection implements ServiceConnection
    {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder)
        {
            WebbyLog.d(TAG, "WebbyService bound to WebbyManager");

            webbyService = ((WebbyService.WebbyServiceBinder) binder).getService();
            isBound = true;
            sendUnsentRequests();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            WebbyLog.d(TAG, "WebbyService unbound to WebbyManager");

            webbyService = null;
            isBound = false;
        }

    }
}
