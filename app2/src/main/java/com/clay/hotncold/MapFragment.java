package com.clay.hotncold;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

/**
 * A fragment that launches other parts of the demo application.
 */
public class MapFragment extends Fragment implements LocationListener, SensorEventListener {

    MapView mapView;
    GoogleMap map;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    double latitude; // latitude
    double longitude; // longitude
    protected LocationManager locationManager;

    static Marker[] markers;
    static boolean cont;

    static String id;
    static String friendid;
    public static boolean isFirst;

    public static boolean isFirst() {
        return isFirst;
    }

    public static void setIsFirst(boolean isFirst) {
        MapFragment.isFirst = isFirst;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = (MapView) v.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        id = AccessToken.getCurrentAccessToken().getUserId();

        startMap();

        return v;
    }

    public void startMap()
    {
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        if (!isGPSEnabled && !isNetworkEnabled) {
            showSettingsAlert();
        }


        map = mapView.getMap();
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.setMyLocationEnabled(true);

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                friendid = marker.getTitle();
                User us = DBHandler.getUser(friendid);
                assert us != null;
                Toast toast = Toast.makeText(getContext(), us.getUsername() + " " + us.getSurname()
                        + " clicked.", Toast.LENGTH_SHORT);
                toast.show();

                Intent intent = new Intent(getContext(), FriendProfileActivity.class);
                intent.putExtra("id", us.getFacebookID());
                startActivity(intent);

                return false;
            }
        });

        latitude = 39.92;
        longitude = 32.86;

        UserLoc loc = new UserLoc();
        loc.setId(id);
        loc.setLat(latitude);
        loc.setLon(longitude);
        loc.setTime(0);
        loc.setSpeed(0);

        DBHandler.locationInsert(loc);

        LatLng PERTH = new LatLng(latitude, longitude);
		/*Marker perth = map.addMarker(new MarkerOptions()
				.position(PERTH)
				.draggable(true));*/

        MapsInitializer.initialize(this.getActivity());

        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate= CameraUpdateFactory.newLatLngZoom(PERTH, 10);
        map.animateCamera(cameraUpdate);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                ArrayList<String> friends = DBHandler.getFriendIds(AccessToken.getCurrentAccessToken().getUserId());

                markers = new Marker[friends.size()];
                cont=true;

                for(int i=0; i<friends.size(); i++)
                {
                    UserLoc u = DBHandler.getFriendLoc(friends.get(i));
                    Log.d("Map", u.getId());
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    MapFragment.setIsFirst(true);
                    b.putInt("i",i);
                    b.putString("id", friends.get(i));
                    b.putString("lat", u.getLat() + "");
                    b.putString("lon", u.getLon() + "");
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }

                while(cont)
                {
                    for(int i=0; i<friends.size(); i++)
                    {
                        UserLoc u = DBHandler.getFriendLoc(friends.get(i));
                        Message msgObj = handler.obtainMessage();
                        Bundle b = new Bundle();

                        Log.d("Map", u.getId());
                        MapFragment.setIsFirst(false);
                        b.putInt("i",i);
                        b.putString("id", friends.get(i));
                        b.putString("lat", u.getLat() + "");
                        b.putString("lon", u.getLon() + "");
                        msgObj.setData(b);
                        handler.sendMessage(msgObj);
                    }
                }
            }

            private final android.os.Handler handler = new android.os.Handler() {

                @Override
                public void handleMessage(Message msg) {
                    Bundle b = msg.getData();
                    int i = b.getInt("i");
                    String id= b.getString("id");
                    String lat = b.getString("lat");
                    String lon = b.getString("lon");
                    if(MapFragment.isFirst()) {
                        LatLng PERTH = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                        markers[i] = map.addMarker(new MarkerOptions()
                                .position(PERTH)
                                .draggable(true)
                                .title(b.getString("id")));
                        if(markers[i].getPosition().latitude==0)
                            markers[i].setVisible(false);
                    }

                    else
                    {
                        if(markers[i]!=null)
                            markers[i].setPosition(new LatLng(Double.parseDouble(lat), Double.parseDouble(lon)));
                    }
                }
            };
        };

        Thread t = new Thread(r);
        t.start();
    }

    @Override
    public void onResume() {
        setIsFirst(true);
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public void showSettingsAlert(){
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());

        alertDialog.setTitle("GPS is settings");

        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getContext().startActivity(intent);
                dialog.cancel();
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }


    @Override
    public void onLocationChanged(Location location) {
        UserLoc u = new UserLoc();
        u.setId(id);
        u.setLat(location.getLatitude());
        u.setLon(location.getLongitude());
        u.setSpeed(location.getSpeed());
        u.setTime(location.getTime());
        u.setAlt(location.getAltitude());
        DBHandler.locationInsert(u);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        UserLoc u = new UserLoc();
        u.setId(id);
        u.setLat(0);
        u.setLon(0);
        u.setSpeed(0);
        u.setTime(0);
        u.setAlt(0);


        DBHandler.locationInsert(u);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public String getProfilePicture(String id)
    {
        return "http://graph.facebook.com/"+id+"/picture?type=large&redirect=true&width=600&height=600";
    }
}