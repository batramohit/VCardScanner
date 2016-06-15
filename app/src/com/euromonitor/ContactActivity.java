package com.euromonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

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
    public void onResume() {
        // Create list adapter
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        ((ListView) findViewById(R.id.contacts_list)).setAdapter(listAdapter);

        super.onResume();
    }

    @Override
    public void onResume(RestClient client) {
        // Keeping reference to rest client
        this.client = client;

        try {
            fetchContacts();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void fetchContacts() throws UnsupportedEncodingException {
        sendRequest("SELECT Name, Phone FROM Contact");
    }

    private void sendRequest(String soql) throws UnsupportedEncodingException {
        RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);

        client.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    listAdapter.clear();
                    JSONArray records = result.asJSONObject().getJSONArray("records");
                    for (int i = 0; i < records.length(); i++) {
                        String name = records.getJSONObject(i).getString("Name");
                        String phone = records.getJSONObject(i).getString("Phone") != "null" ? records.getJSONObject(i).getString("Phone") : "";
                        listAdapter.add(name + "\n" + phone);
                    }
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(ContactActivity.this,
                        ContactActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.contact_menu_scrolling, menu);
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

        else if(id == R.id.action_image_scan)
        {
            Intent intent = new Intent(this, RecognizeActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public void onLogoutClick() {
        SalesforceSDKManager.getInstance().logout(this);
    }
}
