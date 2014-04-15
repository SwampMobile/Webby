package com.swampmobile.webby.examples.activities;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;

import com.swampmobile.webby.examples.R;

public class MainActivity extends ActionBarActivity
{
    private Button webbyActivityButton;
    private Button webbyFragmentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webbyActivityButton = (Button)findViewById(R.id.button_webby_activity);
        webbyActivityButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, WebbyActivity.class);
                startActivity(intent);
            }
        });

        webbyFragmentButton = (Button)findViewById(R.id.button_webby_fragment);
        webbyFragmentButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, WebbyFragmentActivity.class);
                startActivity(intent);
            }
        });
    }

}
