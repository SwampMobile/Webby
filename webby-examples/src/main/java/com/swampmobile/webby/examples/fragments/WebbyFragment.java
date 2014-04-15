package com.swampmobile.webby.examples.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.JsonElement;
import com.squareup.otto.Subscribe;
import com.swampmobile.webby.WebbyManager;
import com.swampmobile.webby.requests.WebbyResponse;
import com.swampmobile.webby.examples.R;
import com.swampmobile.webby.examples.requests.TestWebbyRequest;

/**
 * Webby works the same in Fragments as it does in Activities.  The only difference is that you need
 * to wait to get a reference to context to instantiate the WebbyManager.
 */
public class WebbyFragment extends Fragment
{
    private TextView webbyStatusTextView;
    private Button testWebbyButton;

    private WebbyManager webby;
    private Uri activeRequestUri;

    private Handler handler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_webby, null);

        webbyStatusTextView = (TextView) view.findViewById(R.id.textview_webby_status);

        testWebbyButton = (Button) view.findViewById(R.id.button_test_webby);
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

        handler = new Handler();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstateState)
    {
        super.onActivityCreated(savedInstateState);

        webby = new WebbyManager(getActivity().getApplicationContext(), this);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        webby.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
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
                handler.post(new Runnable()
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
                handler.post(new Runnable()
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
