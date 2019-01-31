package sdp.project.twitter.Utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import sdp.project.twitter.Model.Location;


public class MyGeocoderUtil {

    public static interface  LocationCallback {
         void onNewLocationAvailable(Location location);

         void failedToGetLocation();
    }


    @SuppressLint("MissingPermission")
    public static void requestSingleUpdate(final Context context, final LocationCallback callback) {

        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isNetworkEnabled) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            locationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(android.location.Location location) {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = null;
                    try {
                        addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    } catch (IOException e) {
                        callback.failedToGetLocation();
                    }
                    if(addresses!= null && addresses.size()>0)
                        callback.onNewLocationAvailable(new Location((float) location.getLatitude(), (float) location.getLongitude(), addresses.get(0).getCountryCode(), addresses.get(0).getLocality()));
                    else
                        callback.failedToGetLocation();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, null);
        } else {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSEnabled) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                locationManager.requestSingleUpdate(criteria, new LocationListener() {
                    @Override
                    public void onLocationChanged(android.location.Location location) {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        } catch (IOException e) {
                            callback.failedToGetLocation();
                        }
                        if(addresses!= null && addresses.size()>0)
                        callback.onNewLocationAvailable(new Location((float) location.getLatitude(), (float) location.getLongitude(), addresses.get(0).getCountryCode(), addresses.get(0).getLocality()));
                        else
                            callback.failedToGetLocation();
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }
                }, null);
            }
        }
    }
}