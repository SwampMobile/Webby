package com.swampmobile.webby.examples.activities;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.squareup.otto.Subscribe;
import com.swampmobile.webby.WebbyManager;
import com.swampmobile.webby.requests.WebbyResponse;
import com.swampmobile.webby.examples.R;
import com.swampmobile.webby.examples.requests.TestWebbyRequest;

public class WebbyActivity extends ActionBarActivity
{
    private TextView webbyStatusTextView;
    private Button testWebbyButton;

    private WebbyManager webby;
    private Uri activeRequestUri;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webby);

        webbyStatusTextView = (TextView) findViewById(R.id.textview_webby_status);

        testWebbyButton = (Button) findViewById(R.id.button_test_webby);
        testWebbyButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                webbyStatusTextView.setText("Running request...");

                // Instantiate a WebbyRequest object to retrieve whatever data you want.
                TestWebbyRequest request = new TestWebbyRequest();

                // Be sure to save the URI of your request so you can determine which WebbyEvent
                // response applies to you.
                activeRequestUri = request.getUri();

                // Add your request to be processed by Webby.
                webby.addRequest(request);
            }
        });

        webby = new WebbyManager(this, this);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // You must tell the WebbyManager when to start
        webby.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        // You must tell the WebbyManager when to stop
        webby.onStop();
    }

    /**
     * To listen for WebbyEvents and pick out the one that you care about, use the Otto @Subscribe
     * annotation to listen for all WebbyEvents.  You don't have to worry about registering with a
     * Bus, that was taken care of by the WebbyManager when you constructed it.
     *
     * @param event
     */
    @Subscribe
    public void onWebbyEvent(WebbyResponse event)
    {
        if (event.getResourceId().equals(activeRequestUri))
        {
            if(!event.wasRequestSuccessful())
            {
                // Update the view on the Main thread
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        webbyStatusTextView.setText("Error communicating with server.");
                    }
                });
            }
            else
            {
                // This WebbyEvent is the response to our request
                final JsonElement response = event.getResponse();

                // Update the view on the Main thread
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        webbyStatusTextView.setText(response.toString());
                    }
                });
            }
        }
    }

}
