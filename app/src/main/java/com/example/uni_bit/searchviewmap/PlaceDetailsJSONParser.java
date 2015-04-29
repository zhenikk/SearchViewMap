package com.example.uni_bit.searchviewmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Uni-Bit on 29.04.2015.
 */
public class PlaceDetailsJSONParser {
    public List<HashMap<String, String>> parse(JSONObject jObject) {
        Double lat = (double) 0;
        Double lng = (double) 0;
        String formattedAddress = "";

        HashMap<String, String> hm = new HashMap<>();
        List<HashMap<String, String>> list = new ArrayList<>();
        try {
            lat = (double) jObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location").get("lat");
            lng = (double) jObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location").get("lng");
            formattedAddress = (String)jObject.getJSONObject("result").get("formatted_address");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        hm.put("lat",Double.toString(lat));
        hm.put("lng",Double.toString(lng));
        hm.put("formatted_address",formattedAddress);
        list.add(hm);
        return list;

    }

}
