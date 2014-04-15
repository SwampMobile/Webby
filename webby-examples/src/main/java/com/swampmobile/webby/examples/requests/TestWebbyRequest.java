package com.swampmobile.webby.examples.requests;

import android.net.Uri;

import com.swampmobile.webby.examples.apis.FeedzillaApi;
import com.swampmobile.webby.requests.WebbyRequest;
import com.swampmobile.webby.util.time.Duration;

import retrofit.client.Response;

/**
 * A WebbyRequest must provide at least 2 pieces of functionality: - a constructor which
 * parameterizes the call to the super constructor - an implementation of doWebServiceCall where you
 * call the desired REST endpoint for this request.
 * <p/>
 * The URI which defines this request should be unique to the extent that two URIs are equals if and
 * only if the data they are retrieving is the same.  Webby throws away duplicate requests as
 * determined by URI comparison.
 */
public class TestWebbyRequest extends WebbyRequest<FeedzillaApi>
{
    private static final Uri BASE_URI = new Uri.Builder().scheme("feedzilla").path("categories").build();

    public TestWebbyRequest()
    {
        super(
                BASE_URI.buildUpon().appendQueryParameter("timestamp", System.currentTimeMillis() + "").build(),
                FeedzillaApi.FEEDZILLA_URL,
                FeedzillaApi.class,
                Duration.IMMEDIATELY
        );
    }

    @Override
    public Response doWebServiceCall(FeedzillaApi webservice)
    {
        return webservice.getCategories();
    }
}
