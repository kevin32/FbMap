package com.example.kevin.fbmap.mapRoute;

import android.graphics.Color;
import android.os.AsyncTask;

import com.example.kevin.fbmap.MainActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is being used to draw path between Saint petersburg and Haapsalu
 */

class DrawPathToMap extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;
        try {
            jObject = new JSONObject(jsonData[0]);
            PathJSONParser parser = new PathJSONParser();
            routes = parser.parse(jObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routes;
    }

    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
        ArrayList<LatLng> points;
        PolylineOptions polyLineOptions = null;

        // traversing through routes
        for (int i = 0; i < routes.size(); i++) {
            points = new ArrayList<>();
            polyLineOptions = new PolylineOptions();
            List<HashMap<String, String>> path = routes.get(i);

            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);
                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }
            polyLineOptions.addAll(points);
            polyLineOptions.width(8);
            polyLineOptions.color(Color.BLUE);
        }

        //Add path to the map
        MainActivity.mMap.addPolyline(polyLineOptions);

    }
}
