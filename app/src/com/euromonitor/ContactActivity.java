package com.euromonitor;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.ui.SalesforceActivity;

/**
 * Created by Mohit.Batra on 15-Jun-16.
 */
public class ContactActivity extends SalesforceActivity {

    private RestClient client;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Setup view
        setContentView(R.layout.activity_contacts);
    }


    @Override
    public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client;
    }
}
