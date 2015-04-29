package com.example.uni_bit.searchviewmap;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Created by Uni-Bit on 29.04.2015.
 */
public class PlaceProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.uni_bit.searchviewmap.PlaceProvider";

    public static final Uri SEARCH_URI = Uri.parse("content://" + AUTHORITY + "/search");
    public static final Uri DETAILS_URI = Uri.parse("content://" + AUTHORITY + "/details");

    private static final int SEARCH = 1;
    private static final int SUGGESTIONS = 2;
    private static final int DETAILS = 3;

    String mKey = "AIzaSyAbKniRRRvY_WDv1pMPsv-g7sTN35Vygjs";//MyBrowserKeyForThisApp

    private static final UriMatcher mUriMatcher = buildUriMatcher();

    private static UriMatcher buildUriMatcher() {
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "search", SEARCH);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SUGGESTIONS);
        uriMatcher.addURI(AUTHORITY, "details", DETAILS);
        return uriMatcher;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor c = null;

        PlaceJSONParcer parcer = new PlaceJSONParcer();
        PlaceDetailsJSONParser detailsParcer = new PlaceDetailsJSONParser();

        String jsonString = "";
        String jsonPlaceDetails = "";

        List<HashMap<String, String>> list = null;
        List<HashMap<String, String>> detailsList = null;

        MatrixCursor mCursor = null;

        switch (mUriMatcher.match(uri)) {
            case SEARCH:
                // Defining a cursor object with columns description, lat and lng
                mCursor = new MatrixCursor(new String[]{"description", "lat", "lng"});
                // Create a parser object to parse places in JSON format
                parcer = new PlaceJSONParcer();
                // Create a parser object to parse place details in JSON format
                detailsParcer = new PlaceDetailsJSONParser();
                jsonString = getPlaces(selectionArgs);

                try {
                    // Parse the places ( JSON => List )
                    list = parcer.parse(new JSONObject(jsonString));
                    // Creating cursor object with places
                    for (int i = 0; i < list.size(); i++) {
                        HashMap<String, String> hMap = (HashMap<String, String>) list.get(i);
                        //Adding place details to cursor
                        mCursor.addRow(new String[]{Integer.toString(i), hMap.get("description"), hMap.get("reference")});
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                c = mCursor;
                break;
            case DETAILS:
                // Defining a cursor object with columns description, lat and lng
                mCursor = new MatrixCursor(new String[]{"description", "lat", "lng"});
                detailsParcer = new PlaceDetailsJSONParser();
                jsonPlaceDetails = getPlaceDetails(selectionArgs[0]);

                try {
                    detailsList = detailsParcer.parse(new JSONObject(jsonPlaceDetails));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                for(int j=0;j<detailsList.size();j++)
                {
                    HashMap<String,String> hMapDetails = detailsList.get(j);
                    mCursor.addRow(new String[]{hMapDetails.get("formatted_address"), hMapDetails.get("lat") , hMapDetails.get("lng") });
                }
                c=mCursor;
                break;
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
    private String getPlaceDetailsUrl(String ref){

        // reference of place
        String reference = "reference="+ref;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = reference+"&"+sensor+"&"+mKey;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/details/"+output+"?"+parameters;

        return url;
    }
    private String getPlacesUrl(String qry){

        try {
            qry = "input=" + URLEncoder.encode(qry, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }

        // Sensor enabled
        String sensor = "sensor=false";

        // place type to be searched
        String types = "types=geocode";

        // Building the parameters to the web service
        String parameters = qry+"&"+types+"&"+sensor+"&"+mKey;

        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;
        return url;
    }
    private String getPlaces(String[] params){
        // For storing data from web service
        String data = "";
        String url = getPlacesUrl(params[0]);
        try{
            // Fetching the data from web service in background
            data = downloadUrl(url);
        }catch(Exception e){
            Log.d("Background Task", e.toString());
        }
        return data;
    }

    private String getPlaceDetails(String reference){
        String data = "";
        String url = getPlaceDetailsUrl(reference);
        try {
            data = downloadUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
}
