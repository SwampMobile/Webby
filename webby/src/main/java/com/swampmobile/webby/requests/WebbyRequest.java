package com.swampmobile.webby.requests;

import android.net.Uri;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.swampmobile.webby.Webby;
import com.swampmobile.webby.util.logging.WebbyLog;
import com.swampmobile.webby.util.time.Duration;

import java.io.InputStreamReader;

import retrofit.RestAdapter;
import retrofit.client.Response;

/**
 * Base class for REST webservice requests.  A request contains a URI unique to the resource being
 * requested.  A request also encapsulates the logic necessary to execute a given request.
 * <p/>
 * Implementing classes have 3 primary responsibilities:
 * <p/>
 * - define a URI that will be unique between different resources but identical for the same
 * resource (to avoid duplicate requests and for caching purposes)
 * <p/>
 * - specify the Interface class to be used in a Retrofit RestAdapter
 * <p/>
 * - make the desired webservice call in the doWebserviceCall() method
 * <p/>
 * Note: There is no special scheme for the URI, it can be anything you want so long as it is
 * different for different resources.
 *
 * @param <T>
 */
public abstract class WebbyRequest<T extends Object> implements Runnable
{
    private static final String TAG = "WebbyRequest";

    private Uri uri;
    private String endpoint;
    private Class<T> restAdapterClass;
    private boolean resultFromCache;
    private Duration refreshDuration;

    private int statusCode;
    private String statusPhrase;
    private JsonElement data;

    private Exception loadException;

    public WebbyRequest(Uri uri, String endpoint, Class<T> restAdapterClass)
    {
        this(uri, endpoint, restAdapterClass, Duration.IMMEDIATELY);
    }

    public WebbyRequest(Uri uri, String endpoint, Class<T> restAdapterClass, Duration maxCacheAge)
    {
        this.uri = uri;
        this.endpoint = endpoint;
        this.restAdapterClass = restAdapterClass;
        this.refreshDuration = maxCacheAge;
    }

    /**
     * Implementation of Runnable so that this request can be easily run by Threads and Thread
     * pools.
     */
    public void run()
    {
        try
        {
            data = loadInBackground();
        }
        catch (Exception e)
        {
            loadException = e;
        }
    }

    public Duration getRefreshDuration()
    {
        return refreshDuration;
    }

    public boolean isDataFromCache()
    {
        return resultFromCache;
    }

    public void setIsDataFromCache(boolean resultFromCache)
    {
        this.resultFromCache = resultFromCache;
    }

    /**
     * Returns true if this request executed without throwing an exception.  Returns false if an
     * exception was thrown.  The request exception can be retrieved via getException().
     *
     * @return
     */
    public boolean wasSuccessful()
    {
        return loadException == null;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusPhrase()
    {
        return statusPhrase;
    }

    /**
     * Returns the JsonElement data retrieved by a successful request.  Returns null if the request
     * was unsuccessful.
     *
     * @return
     */
    public JsonElement getData()
    {
        return data;
    }

    public void setData(JsonElement data)
    {
        this.data = data;
    }

    /**
     * Returns the Exception that arose during the request, if the request was unsuccessful.  If the
     * request was successful, returns null.
     *
     * @return
     */
    public Exception getException()
    {
        return loadException;
    }

    public Uri getUri()
    {
        return uri;
    }

    /**
     * Overriden so that requests can be stuck in hashmaps, hashsets, etc without duplication.  A
     * request is defined by its URI and instance variable values.
     * <p/>
     * Note: Hashes need to be unique by the set of all request properties.  Therefore 2 requests
     * for the same data that have different cache expiration dates must still produce different
     * hash codes.
     */
    @Override
    public int hashCode()
    {
        return (uri + "_" + refreshDuration.getValue()).hashCode();
    }

    /**
     * Overriden so that requests can be stuck in hashmaps, hashsets, etc without duplication.  A
     * request is defined by its URI and instance variable values.
     */
    @Override
    public boolean equals(Object other)
    {
        if (this == other)
            return true;

        if (other == null)
            return false;

        if (!(other instanceof WebbyRequest))
            return false;

        WebbyRequest otherReq = (WebbyRequest) other;

        if (otherReq.hashCode() != hashCode())
            return false;

        return true;
    }

    /**
     * Makes call to web service and parses result as JSON.  Returns JsonElement corresponding to
     * return data.
     *
     * @return
     * @throws Exception
     */
    private JsonElement loadInBackground() throws Exception
    {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setLogLevel(Webby.getRetrofitLogLevel())
                .build();

        T webservice = restAdapter.create(restAdapterClass);

        Response response = null;
        JsonElement data = null;
        try
        {
            response = doWebServiceCall(webservice);

            JsonParser parser = new JsonParser();
            data = parser.parse(new JsonReader(new InputStreamReader(response.getBody().in())));
        }
        catch(Exception e)
        {
            throw e;
        }
        finally
        {
            if(response != null)
            {
                // there was an issue, but it looks like we still have a valid http response
                statusCode = response.getStatus();
                statusPhrase = response.getReason();
                WebbyLog.d(TAG, "Response status, code: " + statusCode + ", reason: " + statusPhrase);
            }

            return data;
        }
    }

    /**
     * Implemented by subclass.  This method uses the provided webservice to make a network call to
     * the server.
     *
     * @param webservice
     * @return
     */
    abstract public Response doWebServiceCall(T webservice);
}
