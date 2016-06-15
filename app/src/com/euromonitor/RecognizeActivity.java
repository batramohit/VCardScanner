package com.euromonitor;

import android.app.Activity;
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

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RecognizeActivity extends Activity {

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
    private EditText mEditEmailText;
    private EditText mEditNameText;
    private Spinner mSpinner;
    private Spinner lSpinner;
    private EditText mEditAddressText;
    private VisionServiceClient client;
    private AutoCompleteTextView mailingAddress;
    ArrayList<String> item=new ArrayList<>();
    ArrayAdapter<String> adapter;
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
    public boolean onCreateOptionsMenu(Menu menu)
    {

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    public void uploadContact(View v)
    {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
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
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);
                String pattern = "[0-9]+";
                String preDirections = "S W|SW|S E|SE|N W|NW|N E|NE|N|E|W|S";
                String suffixes = "ALLEY|ALLEE|ALY|ALLEY|ALLY|ALY|ANEX|ANEX|ANX|ANNEX|ANNX|ANX|ARCADE|ARC|ARC|ARCADE|AVENUE|AV|AVE|AVE|AVEN|AVENU|AVENUE|AVN|AVNUE|BAYOU|BAYOO|BYU|BAYOU|BEACH|BCH|BCH|BEACH|BEND|BEND|BND|BND|BLUFF|BLF|BLF|BLUF|BLUFF|BLUFFS|BLUFFS|BLFS|BOTTOM|BOT|BTM|BTM|BOTTM|BOTTOM|BOULEVARD|BLVD|BLVD|BOUL|BOULEVARD|BOULV|BRANCH|BR|BR|BRNCH|BRANCH|BRIDGE|BRDGE|BRG|BRG|BRIDGE|BROOK|BRK|BRK|BROOK|BROOKS|BROOKS|BRKS|BURG|BURG|BG|BURGS|BURGS|BGS|BYPASS|BYP|BYP|BYPA|BYPAS|BYPASS|BYPS|CAMP|CAMP|CP|CP|CMP|CANYON|CANYN|CYN|CANYON|CNYN|CAPE|CAPE|CPE|CPE|CAUSEWAY|CAUSEWAY|CSWY|CAUSWA|CSWY|CENTER|CEN|CTR|CENT|CENTER|CENTR|CENTRE|CNTER|CNTR|CTR|CENTERS|CENTERS|CTRS|CIRCLE|CIR|CIR|CIRC|CIRCL|CIRCLE|CRCL|CRCLE|CIRCLES|CIRCLES|CIRS|CLIFF|CLF|CLF|CLIFF|CLIFFS|CLFS|CLFS|CLIFFS|CLUB|CLB|CLB|CLUB|COMMON|COMMON|CMN|COMMONS|COMMONS|CMNS|CORNER|COR|COR|CORNER|CORNERS|CORNERS|CORS|CORS|COURSE|COURSE|CRSE|CRSE|COURT|COURT|CT|CT|COURTS|COURTS|CTS|CTS|COVE|COVE|CV|CV|COVES|COVES|CVS|CREEK|CREEK|CRK|CRK|CRESCENT|CRESCENT|CRES|CRES|CRSENT|CRSNT|CREST|CREST|CRST|CROSSING|CROSSING|XING|CRSSNG|XING|CROSSROAD|CROSSROAD|XRD|CROSSROADS|CROSSROADS|XRDS|CURVE|CURVE|CURV|DALE|DALE|DL|DL|DAM|DAM|DM|DM|DIVIDE|DIV|DV|DIVIDE|DV|DVD|DRIVE|DR|DR|DRIV|DRIVE|DRV|DRIVES|DRIVES|DRS|ESTATE|EST|EST|ESTATE|ESTATES|ESTATES|ESTS|ESTS|EXPRESSWAY|EXP|EXPY|EXPR|EXPRESS|EXPRESSWAY|EXPW|EXPY|EXTENSION|EXT|EXT|EXTENSION|EXTN|EXTNSN|EXTENSIONS|EXTS|EXTS|FALL|FALL|FALL|FALLS|FALLS|FLS|FLS|FERRY|FERRY|FRY|FRRY|FRY|FIELD|FIELD|FLD|FLD|FIELDS|FIELDS|FLDS|FLDS|FLAT|FLAT|FLT|FLT|FLATS|FLATS|FLTS|FLTS|FORD|FORD|FRD|FRD|FORDS|FORDS|FRDS|FOREST|FOREST|FRST|FORESTS|FRST|FORGE|FORG|FRG|FORGE|FRG|FORGES|FORGES|FRGS|FORK|FORK|FRK|FRK|FORKS|FORKS|FRKS|FRKS|FORT|FORT|FT|FRT|FT|FREEWAY|FREEWAY|FWY|FREEWY|FRWAY|FRWY|FWY|GARDEN|GARDEN|GDN|GARDN|GRDEN|GRDN|GARDENS|GARDENS|GDNS|GDNS|GRDNS|GATEWAY|GATEWAY|GTWY|GATEWY|GATWAY|GTWAY|GTWY|GLEN|GLEN|GLN|GLN|GLENS|GLENS|GLNS|GREEN|GREEN|GRN|GRN|GREENS|GREENS|GRNS|GROVE|GROV|GRV|GROVE|GRV|GROVES|GROVES|GRVS|HARBOR|HARB|HBR|HARBOR|HARBR|HBR|HRBOR|HARBORS|HARBORS|HBRS|HAVEN|HAVEN|HVN|HVN|HEIGHTS|HT|HTS|HTS|HIGHWAY|HIGHWAY|HWY|HIGHWY|HIWAY|HIWY|HWAY|HWY|HILL|HILL|HL|HL|HILLS|HILLS|HLS|HLS|HOLLOW|HLLW|HOLW|HOLLOW|HOLLOWS|HOLW|HOLWS|INLET|INLT|INLT|ISLAND|IS|IS|ISLAND|ISLND|ISLANDS|ISLANDS|ISS|ISLNDS|ISS|ISLE|ISLE|ISLE|ISLES|JUNCTION|JCT|JCT|JCTION|JCTN|JUNCTION|JUNCTN|JUNCTON|JUNCTIONS|JCTNS|JCTS|JCTS|JUNCTIONS|KEY|KEY|KY|KY|KEYS|KEYS|KYS|KYS|KNOLL|KNL|KNL|KNOL|KNOLL|KNOLLS|KNLS|KNLS|KNOLLS|LAKE|LK|LK|LAKE|LAKES|LKS|LKS|LAKES|LAND|LAND|LAND|LANDING|LANDING|LNDG|LNDG|LNDNG|LANE|LANE|LN|LN|LIGHT|LGT|LGT|LIGHT|LIGHTS|LIGHTS|LGTS|LOAF|LF|LF|LOAF|LOCK|LCK|LCK|LOCK|LOCKS|LCKS|LCKS|LOCKS|LODGE|LDG|LDG|LDGE|LODG|LODGE|LOOP|LOOP|LOOP|LOOPS|MALL|MALL|MALL|MANOR|MNR|MNR|MANOR|MANORS|MANORS|MNRS|MNRS|MEADOW|MEADOW|MDW|MEADOWS|MDW|MDWS|MDWS|MEADOWS|MEDOWS|MEWS|MEWS|MEWS|MILL|MILL|ML|MILLS|MILLS|MLS|MISSION|MISSN|MSN|MSSN|MOTORWAY|MOTORWAY|MTWY|MOUNT|MNT|MT|MT|MOUNT|MOUNTAIN|MNTAIN|MTN|MNTN|MOUNTAIN|MOUNTIN|MTIN|MTN|MOUNTAINS|MNTNS|MTNS|MOUNTAINS|NECK|NCK|NCK|NECK|ORCHARD|ORCH|ORCH|ORCHARD|ORCHRD|OVAL|OVAL|OVAL|OVL|OVERPASS|OVERPASS|OPAS|PARK|PARK|PARK|PRK|PARKS|PARKS|PARK|PARKWAY|PARKWAY|PKWY|PARKWY|PKWAY|PKWY|PKY|PARKWAYS|PARKWAYS|PKWY|PKWYS|PASS|PASS|PASS|PASSAGE|PASSAGE|PSGE|PATH|PATH|PATH|PATHS|PIKE|PIKE|PIKE|PIKES|PINE|PINE|PNE|PINES|PINES|PNES|PNES|PLACE|PL|PL|PLAIN|PLAIN|PLN|PLN|PLAINS|PLAINS|PLNS|PLNS|PLAZA|PLAZA|PLZ|PLZ|PLZA|POINT|POINT|PT|PT|POINTS|POINTS|PTS|PTS|PORT|PORT|PRT|PRT|PORTS|PORTS|PRTS|PRTS|PRAIRIE|PR|PR|PRAIRIE|PRR|RADIAL|RAD|RADL|RADIAL|RADIEL|RADL|RAMP|RAMP|RAMP|RANCH|RANCH|RNCH|RANCHES|RNCH|RNCHS|RAPID|RAPID|RPD|RPD|RAPIDS|RAPIDS|RPDS|RPDS|REST|REST|RST|RST|RIDGE|RDG|RDG|RDGE|RIDGE|RIDGES|RDGS|RDGS|RIDGES|RIVER|RIV|RIV|RIVER|RVR|RIVR|ROAD|RD|RD|ROAD|ROADS|ROADS|RDS|RDS|ROUTE|ROUTE|RTE|ROW|ROW|ROW|RUE|RUE|RUE|RUN|RUN|RUN|SHOAL|SHL|SHL|SHOAL|SHOALS|SHLS|SHLS|SHOALS|SHORE|SHOAR|SHR|SHORE|SHR|SHORES|SHOARS|SHRS|SHORES|SHRS|SKYWAY|SKYWAY|SKWY|SPRING|SPG|SPG|SPNG|SPRING|SPRNG|SPRINGS|SPGS|SPGS|SPNGS|SPRINGS|SPRNGS|SPUR|SPUR|SPUR|SPURS|SPURS|SPUR|SQUARE|SQ|SQ|SQR|SQRE|SQU|SQUARE|SQUARES|SQRS|SQS|SQUARES|STATION|STA|STA|STATION|STATN|STN|STRAVENUE|STRA|STRA|STRAV|STRAVEN|STRAVENUE|STRAVN|STRVN|STRVNUE|STREAM|STREAM|STRM|STREME|STRM|STREET|STREET|ST|STRT|ST|STR|STREETS|STREETS|STS|SUMMIT|SMT|SMT|SUMIT|SUMITT|SUMMIT|TERRACE|TER|TER|TERR|TERRACE|THROUGHWAY|THROUGHWAY|TRWY|TRACE|TRACE|TRCE|TRACES|TRCE|TRACK|TRACK|TRAK|TRACKS|TRAK|TRK|TRKS|TRAFFICWAY|TRAFFICWAY|TRFY|TRAIL|TRAIL|TRL|TRAILS|TRL|TRLS|TRAILER|TRAILER|TRLR|TRLR|TRLRS|TUNNEL|TUNEL|TUNL|TUNL|TUNLS|TUNNEL|TUNNELS|TUNNL|TURNPIKE|TRNPK|TPKE|TURNPIKE|TURNPK|UNDERPASS|UNDERPASS|UPAS|UNION|UN|UN|UNION|UNIONS|UNIONS|UNS|VALLEY|VALLEY|VLY|VALLY|VLLY|VLY|VALLEYS|VALLEYS|VLYS|VLYS|VIADUCT|VDCT|VIA|VIA|VIADCT|VIADUCT|VIEW|VIEW|VW|VW|VIEWS|VIEWS|VWS|VWS|VILLAGE|VILL|VLG|VILLAG|VILLAGE|VILLG|VILLIAGE|VLG|VILLAGES|VILLAGES|VLGS|VLGS|VILLE|VILLE|VL|VL|VISTA|VIS|VIS|VIST|VISTA|VST|VSTA|WALK|WALK|WALK|WALKS|WALKS|WALK|WALL|WALL|WALL|WAY|WY|WAY|WAY|WAYS|WAYS|WAYS|WELL|WELL|WL|WELLS|WELLS|WLS";
                String unitDesignators =
                        "APARTMENT|APT|BUILDING|BLDG|FLOOR|FL|SUITE|STE|UNIT|UNIT|ROOM|RM|DEPARTMENT|DEPT|SPC";
                String formatting="\\\\d+\\\\s+([a-zA-Z]+|[a-zA-Z]+\\\\s[a-zA-Z]+)";


                //Validate phone numbers of format "1234567890"
                String firstMobilePattern="\"\\\\d{10}\"";
                //Validate phone numbers with -,.or spaces
                String secondMobilePattern="\"\\\\d{3}[-\\\\.\\\\s]\\\\d{3}[-\\\\.\\\\s]\\\\d{4}\"";
                //Validate phone number with extension length from 3 to 5
                String thirdMobilePattern="\"\\\\d{3}-\\\\d{3}-\\\\d{4}\\\\s(x|(ext))\\\\d{3,5}\"";
                //Validate phone number where area code is in braces
                String fourthMobilePattern="\"\\\\(\\\\d{3}\\\\)-\\\\d{3}-\\\\d{4}\"";
                String pattern1 = "^[\\p{L} .'-]+$";
                String emailPattern = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                        + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
                String namePattern= "[A-Z][a-z]+( [A-Z][a-z]+)";

                String result = "";
                String result1 = "";
                String result2 = "";
                String result3="";
                for (Region reg : r.regions)
                {
                    for (Line line : reg.lines)
                    {
                        for (Word word : line.words)
                        {

                            if (word.text.matches("[+]?[0-9]{10}")||word.text.matches("^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$") || word.text.matches("[0-9]{10}") || word.text.matches("[+]?[0-9]{2}[-][0-9]{10}")|| word.text.matches("[+]?[0-9]{12}"))
                            {
                                result += word.text + " ";
                            }
                            if (word.text.matches(emailPattern))
                            {
                                result1 = word.text + " ";
                            }
                            if (word.text.matches(pattern1))
                            {
                                result2 =word.text + " ";
                            }
                            name.add(result2);


                        }
                        result += "";
                        result1 += "";
                        result3 +="";

                    }
                    result += "";
                    result1 += "";
                    result3 +="";

                }

                mEditText.setText(result);
                mEditEmailText.setText(result1);
             //   mEditAddressText.setText(result3);
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





            }
            mButtonSelectImage.setEnabled(true);
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
        public View getView(int position, View convertView, ViewGroup parent) {
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
}