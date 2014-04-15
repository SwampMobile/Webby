package com.swampmobile.webby.examples.activities;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.swampmobile.webby.examples.R;
import com.swampmobile.webby.examples.fragments.WebbyFragment;

public class WebbyFragmentActivity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webby_fragment);

        if(savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.container, new WebbyFragment())
                    .commit();
        }
    }

}
