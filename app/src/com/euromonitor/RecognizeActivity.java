package com.euromonitor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.ui.SalesforceActivity;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RecognizeActivity extends SalesforceActivity {

    private RestClient restClient;
    private String accountId;

    // Flag to indicate which task is to be performed.
    private static final int REQUEST_SELECT_IMAGE = 0;

    // The button to select an image
    private Button mButtonSelectImage;

    // The URI of the image selected to detect.
    private Uri mImageUri;

    // The image selected to detect.
    private Bitmap mBitmap;

    // The edit to show status and result.
    private EditText mEditText;
    private EditText mEditPhoneText;
    private EditText mEditEmailText;
    private EditText mEditFaxText;
    private EditText mEditNameText;
    private Spinner mSpinner;
    private Spinner lSpinner;
    private EditText mEditAddressText;
    private VisionServiceClient client;
    private AutoCompleteTextView mailingAddress;
    private Spinner jobSpinner;
    ArrayList<String> item=new ArrayList<>();
    ArrayList<String> allValues=new ArrayList<>();
    ArrayList<String> allLines=new ArrayList<>();
    ArrayAdapter<String> adapter;
    String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
            + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    String urlPattern = "^(http:\\/\\/|https:\\/\\/)?(www.)?([a-zA-Z0-9]+).[a-zA-Z0-9]*.[a-z]{3}.?([a-z]+)?$";

    @Override
    public void onResume(RestClient client) {
        this.restClient = client;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);
        if (client == null)
        {
            client = new VisionServiceRestClient(getString(R.string.subscription_key));
        }

        mButtonSelectImage = (Button) findViewById(R.id.buttonSelectImage);
        mEditText = (EditText) findViewById(R.id.editTextResult);
        mEditPhoneText = (EditText) findViewById(R.id.editPhoneResult);
        mEditFaxText = (EditText)findViewById(R.id.editFaxResult);
        mEditEmailText = (EditText) findViewById(R.id.editTextEmailResult);
        mailingAddress=(AutoCompleteTextView)findViewById(R.id.mailingAddress); Country country=new Country();
        item =country.getAllCountryName();
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,item);
        mailingAddress.setAdapter(adapter);
        mailingAddress.setThreshold(1);
      //  mEditAddressText=(EditText) findViewById(R.id.editTextAddressResult);
        //  mEditNameText = (EditText) findViewById(R.id.editTextNameResult);

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


    // Called when the "Select Image" button is clicked.
    public void selectImage(View view)
    {
        mEditText.setText("");
        Intent intent;
        intent = new Intent(RecognizeActivity.this, SelectImageActivity.class);
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    // Called when image selection is done.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("AnalyzeActivity", "onActivityResult");
        switch (requestCode) {
            case REQUEST_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    // If image is selected successfully, set the image URI and bitmap.
                    mImageUri = data.getData();
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, getContentResolver());
                    if (mBitmap != null) {
                        // Show the image on screen.
                        ImageView imageView = (ImageView) findViewById(R.id.selectedImage);
                        imageView.setImageBitmap(mBitmap);
                        // Add detection log.
                        Log.d("AnalyzeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                                + "x" + mBitmap.getHeight());
                        doRecognize();
                    }
                }
                break;
            default:
                break;
        }
    }

    public void doRecognize()
    {
        mButtonSelectImage.setEnabled(false);
        mEditText.setText("Analyzing...");
        mEditEmailText.setText("Analyzing...");
        mEditPhoneText.setText("Analyzing...");
        mEditFaxText.setText("Analyzing...");


        try
        {

            new doRequest().execute();
        }
        catch (Exception e)
        {
            mEditText.setText("Error encountered. Exception is: " + e.toString());
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        OCR ocr;
        ocr = this.client.recognizeText(inputStream, LanguageCodes.AutoDetect, true);
        String result = gson.toJson(ocr);
        Log.d("result", result);
        return result;
    }

    public void uploadContact(View v) throws IOException {
        HashMap<String, Object> contactFildes = new HashMap<String, Object>();
        String email = ((EditText)findViewById(R.id.editTextEmailResult)).getText().toString();
        String lastname=lSpinner.getSelectedItem().toString();
        String name=mSpinner.getSelectedItem().toString();
        contactFildes.put("LastName", lastname);
        contactFildes.put("FirstName", name);
        contactFildes.put("Title", "SE");
        contactFildes.put("Phone", "8050624933");
        contactFildes.put("MobilePhone", "8050624933");
        contactFildes.put("Fax", "8050624933");
        contactFildes.put("Email", email);
        contactFildes.put("MailingCountry", "India");
        contactFildes.put("MailingStreet", "Raj Kumar Road");
        contactFildes.put("MailingCity", "Bangalore");
        contactFildes.put("MailingPostalCode", "260052");
        contactFildes.put("Website__c", "www.euromonitor.com");
        createContact(contactFildes, "ABC");

//        Intent intent = new Intent(this, MainActivity.class);
//        startActivity(intent);
    }

    private void createContact(final HashMap<String,Object> contact, final String accountName) throws UnsupportedEncodingException {
        String soql = "SELECT Id,Name FROM Account WHERE Name = \'" + accountName + "\'";
        RestRequest restRequest = RestRequest.getRequestForQuery(getString(R.string.api_version), soql);

        restClient.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    JSONArray records = result.asJSONObject().getJSONArray("records");
                    if(records != null && records.length() > 0) {
                        String accountId = records.getJSONObject(0).getString("Id");
                        contact.put("AccountId", accountId);
                        createContact(contact);
                    }
                    else {
                        createAccount(contact, accountName);
                    }
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(RecognizeActivity.this,
                        RecognizeActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                        Toast.LENGTH_LONG).show();
            }


        });
    }

    private void createAccount(final HashMap<String, Object> contactField, String accountName) throws IOException{
        Map<String, Object> accountFields = new HashMap<String, Object>();
        accountFields.put("Name", accountName);

        RestRequest restRequest = RestRequest.getRequestForCreate(getString(R.string.api_version), "Account", accountFields);
        restClient.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    String accountId = result.asJSONObject().getString("id");
                    contactField.put("AccountId", accountId);
                    createContact(contactField);
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(RecognizeActivity.this,
                        RecognizeActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createContact(HashMap<String, Object> contactField) throws IOException{
        RestRequest restRequest = RestRequest.getRequestForCreate(getString(R.string.api_version), "Contact", contactField);
        restClient.sendAsync(restRequest, new RestClient.AsyncRequestCallback() {
            @Override
            public void onSuccess(RestRequest request, RestResponse result) {
                try {
                    Toast.makeText(RecognizeActivity.this,
                            "Contact Created Successfully",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(RecognizeActivity.this,
                        RecognizeActivity.this.getString(SalesforceSDKManager.getInstance().getSalesforceR().stringGenericError(), exception.toString()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }



    private class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public doRequest() {
        }

        @Override
        protected String doInBackground(String... args)
        {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }


        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            if (e != null)
            {
                mEditText.setText("Error: " + e.getMessage());
                this.e = null;
            }
            else
            {

                ArrayList<String> name =new ArrayList<String>();
                ArrayList<String> job=new ArrayList<>();
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);
                String pattern1 = "^[\\p{L} .'-]+$";
                String namePattern= "[A-Z][a-z]+( [A-Z][a-z]+)";

                String result = "";
                String result1 = "";
                String result2 = "";
                String result3="";
                allLines.clear();
                allValues.clear();
                for (Region reg : r.regions)
                {
                    for (Line line : reg.lines)
                    {
                        String lineData = new String();
                        for (Word word : line.words)
                        {
                            lineData += word.text + " ";
                            allValues.add(word.text);
                            if (word.text.matches(emailPattern))
                            {
                                result1 = word.text + " ";
                            }
//                            if (word.text.matches(pattern1))
//                            {
//                                result2 =word.text + " ";
//                            }
//                            name.add(result2);
//
//                            if(word.text.equals(item))
//                            {
//                                mailingAddress.setText(word.text);
//                            }
//                             result3=word.text + " ";
                        }
                        allLines.add(lineData);

//                        result += "";
//                        result1 += "";
//                        result3 +="";
//                        job.add(result3);

                    }

//                    result += "";
//                    result1 += "";
//                    result3 +="";

                }
                mEditText.setText(getMobileNumber());
                mEditPhoneText.setText(getPhoneNumber());
                mEditFaxText.setText(getFaxNumber());
                mEditEmailText.setText(result1);
                mSpinner = (Spinner) findViewById(R.id.name);
                RecognizeActivity.NameAdapter nameAdapter = new RecognizeActivity.NameAdapter(name);
                mSpinner.setAdapter(nameAdapter);
                mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                    {

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
                lSpinner=(Spinner)findViewById(R.id.lastname);
                lSpinner.setAdapter(nameAdapter);
                   jobSpinner =(Spinner)findViewById(R.id.job);
                RecognizeActivity.JobTitle jobAdapter =new RecognizeActivity.JobTitle(job);
                jobSpinner.setAdapter(jobAdapter);

            }
            mButtonSelectImage.setEnabled(true);
        }

    }

    private String getMobileNumber()
    {
        String number = new String();
        for(String value : allValues)
        {
            int i = 0;
            if(value.toLowerCase().contains("mobile") || value.toLowerCase().contains("m") || value.toLowerCase().contains("mob") || value.toLowerCase().contains("cell"))
            {
                int index = allValues.indexOf(value);
                if((index + 1) < allValues.size() && IsInteger(allValues.get(index + 1)))
                {
                    number = allValues.get(index + 1) + " ";
                    i = index + 2;
                }
                else
                {
                    continue;
                }
                while (!allValues.get(i).contains("+") || IsInteger(allValues.get(i)))
                {

                    if (allValues.get(i).matches("[+]?[0-9]{10}") || allValues.get(i).matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$") || allValues.get(i).matches("[0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{2}[-][0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{12}") || allValues.get(i).matches("^(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*$"))
                    {
                        number += allValues.get(i) + " ";
                    }
                    i++;

                    if(i >= allValues.size() || allValues.get(i).toLowerCase().contains("phone") || allValues.get(i).toLowerCase().contains("tel") || allValues.get(i).toLowerCase().contains("direct") || allValues.get(i).toLowerCase().contains("ph")
                            || allValues.get(i).toLowerCase().contains("t") || allValues.get(i).toLowerCase().contains("fax") || allValues.get(i).toLowerCase().contains("f"))
                        break;
                }
            }
        }
        return number;
    }

    private String getPhoneNumber()
    {
        String number = new String();
        for(String value : allValues)
        {
            int i = 0;
            if(value.toLowerCase().contains("phone") || value.toLowerCase().contains("tel") || value.toLowerCase().contains("direct") || value.toLowerCase().contains("ph") || value.toLowerCase().contains("t"))
            {
                int index = allValues.indexOf(value);
                if((index + 1) < allValues.size() && IsInteger(allValues.get(index + 1)))
                {
                    number = allValues.get(index + 1) + " ";
                    i = index + 2;
                }
                else
                {
                    continue;
                }
                while (!allValues.get(i).contains("+") || IsInteger(allValues.get(i)))
                {

                    if (allValues.get(i).matches("[+]?[0-9]{10}") || allValues.get(i).matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$") || allValues.get(i).matches("[0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{2}[-][0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{12}") || allValues.get(i).matches("^(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*$"))
                    {
                        number += allValues.get(i) + " ";
                    }
                    i++;

                    if(i >= allValues.size() || allValues.get(i).toLowerCase().contains("mobile") || allValues.get(i).toLowerCase().contains("m")
                            || allValues.get(i).toLowerCase().contains("mob") || allValues.get(i).toLowerCase().contains("cell") || allValues.get(i).toLowerCase().contains("fax")
                            || allValues.get(i).toLowerCase().contains("f"))
                        break;
                }
            }
        }
        return number;
    }

    private String getFaxNumber()
    {
        String number = new String();
        for(String value : allValues)
        {
            int i = 0;
            if(value.toLowerCase().contains("fax") || value.toLowerCase().contains("f"))
            {
                int index = allValues.indexOf(value);
                if((index + 1) < allValues.size() && IsInteger(allValues.get(index + 1)))
                {
                    number = allValues.get(index + 1) + " ";
                    i = index + 2;
                }
                else
                {
                    continue;
                }
                while (!allValues.get(i).contains("+") || IsInteger(allValues.get(i)))
                {

                    if (allValues.get(i).matches("[+]?[0-9]{10}") || allValues.get(i).matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$") || allValues.get(i).matches("[0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{2}[-][0-9]{10}") || allValues.get(i).matches("[+]?[0-9]{12}") || allValues.get(i).matches("^(\\(?\\+?[0-9]*\\)?)?[0-9_\\- \\(\\)]*$"))
                    {
                        number += allValues.get(i) + " ";
                    }
                    i++;

                    if(i >= allValues.size() || allValues.get(i).toLowerCase().contains("mobile") || allValues.get(i).toLowerCase().contains("m")
                            || allValues.get(i).toLowerCase().contains("mob") || allValues.get(i).toLowerCase().contains("cell") || allValues.get(i).toLowerCase().contains("phone")
                            || allValues.get(i).toLowerCase().contains("tel") || allValues.get(i).toLowerCase().contains("direct") || allValues.get(i).toLowerCase().contains("ph") || allValues.get(i).toLowerCase().contains("t") )
                        break;
                }
            }
        }
        return number;
    }

    private boolean IsInteger(String value)
    {
        try{
            Integer.parseInt(value);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public class NameAdapter extends BaseAdapter implements SpinnerAdapter
    {
        ArrayList<String> names = new ArrayList<>();
        public NameAdapter(ArrayList<String> names)
        {
            this.names=names;
        }
        @Override
        public int getCount()
        {
            return names.size();
        }

        @Override
        public Object getItem(int position)
        {
            return names.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return Long.valueOf(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View nameView;
            if (convertView != null)
            {
                nameView = convertView;
            }
            else
            {
                nameView = getLayoutInflater().inflate(R.layout.name_item, parent, false);
            }
            TextView nameItem = (TextView) nameView.findViewById(R.id.nameItem);
            nameItem.setText(names.get(position));
            return nameView;
        }
    }

    public class JobTitle extends BaseAdapter implements SpinnerAdapter
    {
        ArrayList<String> job = new ArrayList<>();
        public JobTitle(ArrayList<String> job)
        {
            this.job=job;
        }
        @Override
        public int getCount() {
            return job.size();
        }

        @Override
        public Object getItem(int position) {
            return job.get(position);
        }

        @Override
        public long getItemId(int position) {
            return Long.valueOf(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View jobView;
            if (convertView != null)
            {
                jobView = convertView;
            }
            else
            {
                jobView = getLayoutInflater().inflate(R.layout.name_item, parent, false);
            }

            TextView nameItem = (TextView) jobView.findViewById(R.id.nameItem);
            nameItem.setText(job.get(position));
            return jobView;
        }
    }
}