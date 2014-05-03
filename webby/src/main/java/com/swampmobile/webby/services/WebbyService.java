package com.swampmobile.webby.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.swampmobile.webby.Webby;
import com.swampmobile.webby.requests.WebbyRequest;
import com.swampmobile.webby.requests.WebbyResponse;
import com.swampmobile.webby.util.cache.DataCache;
import com.swampmobile.webby.util.cache.DataCache.CacheReadException;
import com.swampmobile.webby.util.cache.DataCache.CacheWriteException;
import com.swampmobile.webby.util.cache.FlatFileDataCache;
import com.swampmobile.webby.util.logging.WebbyLog;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebbyService extends Service
{
    private static final String TAG = "WebbyService";

    private final int MAX_EXECUTION_THREADS = 3;

    private boolean isCreated = false;

    private WebbyServiceBinder webbyServiceBinder;

    private LinkedHashSet<WebbyRequest> requestQueue; // chose LinkedHashSet because it prevents duplicates but also preserves insertion order (like a queue + set)
    private Object requestLock = new Object();

    private RequestConsumerThread requestConsumerThread;
    private ExecutorService executorService;

    private DataCache cache;
    private Object cacheLock = new Object();

    public WebbyService()
    {
        webbyServiceBinder = new WebbyServiceBinder(this);

        // Setup queue of requests
        requestQueue = new LinkedHashSet<WebbyRequest>();

        // Setup consumer thread to process new requests from queue
        requestConsumerThread = new RequestConsumerThread();
        executorService = Executors.newFixedThreadPool(MAX_EXECUTION_THREADS);
    }

    /**
     * Adds a WebbyRequest to the queue of pending requests.  If the given request is already
     * queue'd, then the request is not added to the queue for a 2nd time.
     *
     * @param request
     */
    public void addRequest(WebbyRequest request)
    {
        synchronized(requestLock)
        {
            WebbyLog.d(TAG, "Adding request to Webby queue.");
            WebbyLog.d(TAG, " - was request already in queue? " + (requestQueue.contains(request) ? "YES" : "No"));

            // Queue up the request
            requestQueue.add(request);

            requestLock.notifyAll();
        }
    }

    @Override
    public void onCreate()
    {
        WebbyLog.i(TAG, "onCreate()");

        super.onCreate();

        isCreated = true;

        cache = new FlatFileDataCache(getApplicationContext());

        requestConsumerThread = new RequestConsumerThread();
        requestConsumerThread.start();
    }

    @Override
    public void onDestroy()
    {
        WebbyLog.i(TAG, "onDestroy()");

        isCreated = false;

        synchronized(requestLock)
        {
            requestLock.notifyAll(); // Allow any threads monitoring requests to kill themselves
        }

        requestConsumerThread = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return webbyServiceBinder;
    }


    public static class WebbyServiceBinder extends Binder
    {
        private WebbyService service;

        public WebbyServiceBinder(WebbyService service)
        {
            this.service = service;
        }

        public WebbyService getService()
        {
            return service;
        }
    }


    /**
     * Consumer thread.
     *
     * Pulls requests out of the WebbyService's requestQueue and submits them for
     * execution.  The RequestConsumerThread is alive as long as the WebbyService
     * and waits until new requests are submitted.
     *
     * @author Matt
     *
     */
    private class RequestConsumerThread extends Thread
    {
        @Override
        public void run()
        {
            while(WebbyService.this.isCreated)
            {
                synchronized(requestLock)
                {
                    // Send each queue'd request to the executor service to run
                    Iterator<WebbyRequest> iterator = requestQueue.iterator();
                    WebbyRequest request;
                    WebbyRequestContainerThread requestContainer;
                    if(requestQueue.size() > 0)
                    {
                        request = iterator.next();
                        iterator.remove();
                        requestContainer = new WebbyRequestContainerThread( request );

                        WebbyLog.d(TAG, "Submitting request for execution.");
                        executorService.execute(requestContainer);
                    }

                    requestLock.notifyAll();

                    // Wait until someone adds more requests to the queue
                    try {
                        if(requestQueue.size() == 0)
                            requestLock.wait();
                    } catch (InterruptedException e) {
                        // don't care, just loop around again
                    }
                }
            }
        }
    }

    /**
     * All WebbyRequests are wrapped in a WebbyRequestContainerThread.  This container will
     * execute its WebbyRequest, write results to a cache, and then alert any listeners of
     * the WebbyRequest result.
     *
     * @author Matt
     *
     */
    private class WebbyRequestContainerThread extends Thread
    {
        private WebbyRequest request;

        public WebbyRequestContainerThread(WebbyRequest request)
        {
            this.request = request;
        }

        @Override
        public void run()
        {
            executeRequest();

            writeDataToCache();

            broadcastWebbyEvent();
        }

        private void executeRequest()
        {
            // If this service is still in existence
            if(isCreated)
            {
                // If we want a cached value, try to load from cache
                WebbyLog.d(TAG, "Trying to retrieve resource from cache.");
                if(cache.containsItem(request.getUri()) && cache.isYoungerThan(request.getUri(), request.getRefreshDuration()))
                {
                    WebbyLog.d(TAG, "Cache has resource, obtaining.");
                    JsonParser parser = new JsonParser();
                    JsonElement data;

                    // Read data from cache
                    try {
                        synchronized(cacheLock)
                        {
                            data = parser.parse( cache.readFromCacheSync(request.getUri()) );
                            request.setData(data);
                            request.setIsDataFromCache(true);
                        }
                    } catch (JsonSyntaxException e) {
                        WebbyLog.e(TAG, "Error reading data from cache.", e);
                    } catch (CacheReadException e) {
                        WebbyLog.e(TAG, "Error reading data from cache.", e);
                    }
                }
                else
                {
                    WebbyLog.d(TAG, "Item not yet in cache.");
                }

                // If we didn't read data from cache, execute request
                if(request.getData() == null)
                {
                    WebbyLog.d(TAG, "Running a request");
                    request.run();
                    request.setIsDataFromCache(false);
                    WebbyLog.d(TAG, "Request has completed. Successful? " + request.wasSuccessful());
                    if(!request.wasSuccessful())
                    {
                        WebbyLog.d(TAG, " - Exception: " + request.getException());
                        request.getException().printStackTrace();
                    }
                }
            }
        }

        private void writeDataToCache()
        {
            // If this Service is still in existence, and our data is fresh from the server,
            // then write this data to the cache.
            if(isCreated && request.wasSuccessful() && !request.isDataFromCache())
            {
                synchronized(cacheLock)
                {
                    WebbyLog.d(TAG, "Writing item to cache");
                    try {
                        cache.writeToCacheSync(request.getUri(), request.getData().toString());
                    } catch (CacheWriteException e) {
                        WebbyLog.e(TAG, "Could not write web service resource to cache.", e);
                    }
                }
            }
        }

        private void broadcastWebbyEvent()
        {
            // If this service is still in existence
            if(isCreated)
            {
                synchronized(requestLock)
                {
                    WebbyResponse event;
                    if(request.wasSuccessful())
                    {
                        event = new WebbyResponse(request.getUri(), request.getStatusCode(), request.getStatusPhrase(), request.getData(), request.isDataFromCache());
                    }
                    else
                    {
                        event = new WebbyResponse(request.getUri(), request.getStatusCode(), request.getStatusPhrase(), request.getData(), request.getException());
                    }
                    Webby.getBus().post(event);
                }
            }
        }
    }
}
