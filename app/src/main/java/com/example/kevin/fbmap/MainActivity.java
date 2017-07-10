package com.example.kevin.fbmap;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.kevin.fbmap.fbSearchQuery.Categories;
import com.example.kevin.fbmap.mapRoute.DownloadPathJSON;
import com.example.kevin.fbmap.mapRoute.MapHttpConnection;
import com.example.kevin.myapplication.R;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.places.PlaceManager;
import com.facebook.places.model.PlaceFields;
import com.facebook.places.model.PlaceSearchRequestParams;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    /**
     * Made it static because we only have one instance of the map.
     * It can be used to pass between different classes.
     */
    public static GoogleMap mMap;

    /**
     * Used to store restaurant markers.
     * Used because whenever we clean the map.
     * We don't want to remove route, "starting point" marker and "ending point" marker
     */
    HashMap<String, Marker> restaurantMarkersHashMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * The Places Graph SDK can be accessed using either a User Access Token or a Client Token.
         * A User Access Token will require users to login to Facebook, but a Client Token won't.
         * I don't feel to bother users to login with FB.
         * Because we can make our requests using client token, we only need to get "likes", which is a public information. (KISS principle)
         * There might be some kind of requests limits, but that is not the case at the moment.
         * @see <a href="https://developers.facebook.com/docs/places/access-tokens">Limits</a>
         */
        FacebookSdk.setClientToken(getResources().getString(R.string.fb_client_token));

        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Show info to the user
        Toast.makeText(this, getResources().getString(R.string.add_marker_text), Toast.LENGTH_LONG).show();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add marker to St. Petersburg
        LatLng saintPetersburgLatLng = new LatLng(59.934275, 30.335133);
        mMap.addMarker(new MarkerOptions().position(saintPetersburgLatLng).title(getResources().getString(R.string.marker_start)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add marker to Haapsalu
        LatLng haapsaluLatLng = new LatLng(58.939320, 23.541190);
        mMap.addMarker(new MarkerOptions().position(haapsaluLatLng).title(getResources().getString(R.string.marker_end)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        //Set camera position
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(58.885575, 25.563598), 9.5f));

        //Build route URL and get JSON object to draw route to the map
        String url = MapHttpConnection.getMapsApiDirectionsUrl(saintPetersburgLatLng, haapsaluLatLng);
        new DownloadPathJSON().execute(url);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                //Remove "My location" and restaurant markers from the map
                for (Marker marker : restaurantMarkersHashMap.values())
                    marker.remove();
                restaurantMarkersHashMap.clear();

                //Add "My location" marker to map
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(getResources().getString(R.string.marker_my_location)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                restaurantMarkersHashMap.put(marker.getId(), marker);

                //Create location object based our location
                final Location location = new Location("");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);

                //Building the request
                PlaceSearchRequestParams.Builder builder = new PlaceSearchRequestParams.Builder();
                builder.setLimit(15); //5 as some extra space
                builder.addField(PlaceFields.NAME);
                builder.addField(PlaceFields.LOCATION);
                builder.addField(PlaceFields.ENGAGEMENT);
                builder.addCategory(Categories.FOOD_BEVERAGE);

                /**
                 * One redundant comment!
                 * I usually avoid hardcoding strings,
                 * but it is very unlikely that these kind of strings
                 * like "location, date, engagement etc" will never change!
                 */
                //Handle the response
                final GraphRequest request = PlaceManager.newPlaceSearchRequestForLocation(builder.build(), location);
                request.setCallback(new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        JSONArray responseJSONArray;
                        try {
                            responseJSONArray = new JSONObject(response.getRawResponse()).getJSONArray("data");
                            if (responseJSONArray.length() == 0)
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_no_markers), Toast.LENGTH_LONG).show();

                            int nearbyRestaurantsLimit = 10;
                            for (int i = 0; i < nearbyRestaurantsLimit; i++) {
                                JSONObject temp = responseJSONArray.getJSONObject(i);
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(temp.getJSONObject("location").getDouble("latitude"), temp.getJSONObject("location").getDouble("longitude")))
                                        .title(temp.getString("name"))
                                        .snippet("Likes : " + temp.getJSONObject("engagement").getInt("count")));

                                //Adding marker to list
                                restaurantMarkersHashMap.put(marker.getId(), marker);


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                request.executeAsync();
            }
        });
    }
}