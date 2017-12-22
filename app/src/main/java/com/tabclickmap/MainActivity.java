package com.tabclickmap;


import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<LatLng> MarkerPoints;
    private LocationRequest mLocationRequest;
    private double current_latitude = 0.0;
    private double current_longitude = 0.0;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private ImageView btn_current_location;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_current_location=findViewById(R.id.btn_current_location);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        MarkerPoints = new ArrayList<>();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btn_current_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    getMyLocation();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            buildGoogleApiClient();
                            //     mMap.setMyLocationEnabled(true);
                            //   mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                } catch (Exception e) {
                }

            }
        });
    }

    private void getMyLocation() {
        LatLng latLng = new LatLng(current_latitude, current_longitude);
        String str_getloc = getAddress(latLng);
        mMap.clear();
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.pick_location))
        .anchor(0.5f, 0.5f)
       .title(str_getloc);
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
    }

    //--------------------------------------------Map Ready------------------------
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // googleMap.clear();
        MapStyleOptions style = MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style);
        this.mMap.setMapStyle(style);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                // mMap.setMyLocationEnabled(false);
                // mMap.getUiSettings().setMyLocationButtonEnabled(false);

            }
        } else {
            buildGoogleApiClient();
            //  mMap.setMyLocationEnabled(false);
            // mMap.getUiSettings().setMyLocationButtonEnabled(false);

        }


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
                if (MarkerPoints.size() > 1) {
                    MarkerPoints.clear();
                    mMap.clear();
                }
                MarkerPoints.add(point);
                //  MarkerOptions options = new MarkerOptions();
                // options.position(point);
                if (MarkerPoints.size() == 1) {
                    // options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (MarkerPoints.size() == 2) {
                    // options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }//mMap.addMarker(options);
                if (MarkerPoints.size() >= 2) {
                    LatLng origin = MarkerPoints.get(0);
                    LatLng dest = MarkerPoints.get(1);

                    try {
                        setPolyline(origin, dest);
                    } catch (Exception e) {
                    }




                }
            }
        });

    }

    private void setPolyline(LatLng curent, LatLng dis) {


        if (mMap != null) {
            mMap.clear();
            MarkerPoints.clear();
        }
        String url = getUrl(curent, dis);
        Log.d("onMapClick", url.toString());

        try {
            ParseData(url);
        }catch (Exception e){}//-------------------
        placeMarkerOnMap1(curent);
        placeMarkerOnMap2(dis);
        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLng(curent));
        // mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
    }

    protected void placeMarkerOnMap1(LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions().position(location);
        String str_getlocc = getAddress(location);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_location_pin)).draggable(true).anchor(0.5f, 0.5f);
        mMap.addMarker(markerOptions).setTitle(str_getlocc);
    }

    protected void placeMarkerOnMap2(LatLng location) {
        MarkerOptions markerOptions = new MarkerOptions().position(location);
        String str_getloc = getAddress(location);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drop_location_pin)).draggable(true).anchor(0.5f, 0.5f);
        mMap.addMarker(markerOptions).setTitle(str_getloc);

    }

    private String getAddress(LatLng location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String addresstxt = "";
        try {
            List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            if (null != addresses && !addresses.isEmpty()) {
                addresstxt = "" + addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresstxt == null) {
            return addresstxt = "";
        }
        return addresstxt;
    }

    private String getUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&mode=driving&key=AIzaSyC-xQ2NJX_QoyLjZQJw8DWnJQwqnJvmTI4";
        return url;
    }


    private void ParseData(String url) {
        Ion.with(this)
                .load(url)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "txt_Netork_error", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new ParserTask().execute(String.valueOf(result));
                    }});
    }


private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<List<HashMap<String, String>>> routes = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            Log.d("ParserTask", jsonData[0].toString());
            DataParser parser = new DataParser();
            Log.d("ParserTask", parser.toString());

            // Starts parsing data
            routes = parser.parse(jObject);
            Log.d("ParserTask", "Executing routes");
            Log.d("ParserTask", routes.toString());

        } catch (Exception e) {
            Log.d("ParserTask", e.toString());
            e.printStackTrace();
        }
        return routes;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<List<HashMap<String, String>>> result) {
        ArrayList<LatLng> points;
        PolylineOptions lineOptions = null;

        if(result !=null) {
            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(15);
                lineOptions.color(Color.parseColor("#2cbfc9"));

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
                moveToBounds(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }
}

    private void moveToBounds(PolylineOptions p) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        List<LatLng> arr = p.getPoints();
        for (int i = 0; i < arr.size(); i++) {
            builder.include(arr.get(i));
        }
        LatLngBounds bounds = builder.build();
        mMap.setPadding(100, 100, 100, 100);
        CameraUpdate myLocation = CameraUpdateFactory.newLatLngBounds(bounds, 10);
        mMap.moveCamera(myLocation);

    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //edtPickUpLocation.setText("");
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
            mMap.clear();
        }
        current_latitude = location.getLatitude();
        current_longitude = location.getLongitude();


        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {return;
        }
        mMap.setMyLocationEnabled(false);
        String str_getloc = getAddress(latLng);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.pick_location))
                .anchor(0.5f, 0.5f)
        .title(str_getloc);
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;


    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "Don't Permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

}