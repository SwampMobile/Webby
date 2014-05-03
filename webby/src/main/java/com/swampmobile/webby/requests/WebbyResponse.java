package com.swampmobile.webby.requests;

import android.net.Uri;

import com.google.gson.JsonElement;

/**
 * Represents the result of REST communication.
 */
public class WebbyResponse
{
    private Uri resourceId;
    private int statusCode;
    private String statusPhrase;
    private JsonElement response;
    private boolean isFromCache;
    private Exception error;

    public WebbyResponse(Uri resourceId, int statusCode, String statusPhrase, JsonElement response, Exception error)
    {
        this(resourceId, statusCode, statusPhrase, response, false);
        this.error = error;
    }

    public WebbyResponse(Uri resourceId, int statusCode, String statusPhrase, JsonElement response, boolean isFromCache)
    {
        this.resourceId = resourceId;
        this.statusCode = statusCode;
        this.statusPhrase = statusPhrase;
        this.response = response;
        this.isFromCache = isFromCache;
    }

    public Uri getResourceId()
    {
        return resourceId;
    }

    public int getResponseStatusCode()
    {
        return statusCode;
    }

    public String getResponseStatusPhrase()
    {
        return statusPhrase;
    }

    public JsonElement getResponse()
    {
        return response;
    }

    public boolean isFromCache()
    {
        return isFromCache;
    }

    public boolean wasRequestSuccessful()
    {
        return error == null;
    }

    public Exception getError()
    {
        return error;
    }
}
