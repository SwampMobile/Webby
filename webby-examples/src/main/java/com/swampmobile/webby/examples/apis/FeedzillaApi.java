package com.swampmobile.webby.examples.apis;

import retrofit.client.Response;
import retrofit.http.GET;

/**
 * Example api interface that Retrofit will use to communicate with a server.  Webby works
 * exclusively with Retrofit communication.
 */
public interface FeedzillaApi
{
    public static final String FEEDZILLA_URL = "http://api.feedzilla.com";

    @GET("/v1/categories.json")
    Response getCategories();
}
