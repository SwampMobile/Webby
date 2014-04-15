package com.swampmobile.webby.requests;

import android.net.Uri;

import com.google.gson.JsonElement;

/**
 * Represents the result of REST communication.
 */
public class WebbyResponse
{
    private Uri resourceId;
    private JsonElement response;
    private boolean isFromCache;
    private Exception error;

    public WebbyResponse(Uri resourceId, Exception error)
    {
        this(resourceId, null, false);
        this.error = error;
    }

    public WebbyResponse(Uri resourceId, JsonElement response, boolean isFromCache)
    {
        this.resourceId = resourceId;
        this.response = response;
        this.isFromCache = isFromCache;
    }

    public Uri getResourceId()
    {
        return resourceId;
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
