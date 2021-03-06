package com.euromonitor;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.ui.SalesforceActivity;

/**
 * Created by Mohit.Batra on 10-Jun-16.
 */
public class MainActivity extends SalesforceActivity {

    private RestClient client;

    @Override
    public void onResume(RestClient client) {
        this.client = client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        if (getString(R.string.subscription_key).startsWith("Please"))
        {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.add_subscription_key_tip_title))
                    .setMessage(getString(R.string.add_subscription_key_tip))
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            onLogoutClick();
            return true;
        }

        else if(id == R.id.action_accounts)
        {
            Intent intent = new Intent(this, AccountActivity.class);
            startActivity(intent);
        }

        else if(id == R.id.action_contacts)
        {
            Intent intent = new Intent(this, ContactActivity.class);
            startActivity(intent);
        }



        return super.onOptionsItemSelected(item);
    }

    public void onLogoutClick() {
        SalesforceSDKManager.getInstance().logout(this);
    }


    public void activityRecognize(View v)
    {
        Intent intent = new Intent(this, RecognizeActivity.class);
        startActivity(intent);
    }
}
