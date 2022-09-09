package com.example.dff50.tareapp;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import java.text.DateFormat;
import java.util.Date;

public class LocationUpdaterService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {



    public static final String LOCALIZACION_RESULT = "Actualizacion de posicion";

    public static final String LOCALIZACION_MESSAGE = "Actualizacion de posicion";

    protected GoogleApiClient mGoogleApiClient;


    protected LocationRequest mLocationRequest;


    protected Location mCurrentLocation;


    protected String mLastUpdateTime;


    private LocalBroadcastManager mBroadcaster;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Al comenzar se realiza la configuracion basica para funcionar
    @Override
    public void onCreate() {
        super.onCreate();

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        mBroadcaster = LocalBroadcastManager.getInstance(this);
    }


    //Al terminar se para el servicio
    @Override
    public void onDestroy() {
        stopLocationUpdates();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (mGoogleApiClient.isConnected()){
            startLocationUpdates();
        }
        return START_STICKY;
    }

    //Inicializa la api de google
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    //Se configura el intervalo en el que se revisa la posicion
    protected void createLocationRequest() {
        mLocationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    //Lanza el comienzo del servicio
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    //Para el servicio
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
    }

    //Se manda un broadcast con la informacion de la posicion
    private void sendResult(LatLng message) {
        Intent intent = new Intent(LOCALIZACION_RESULT);
        if (message != null)
            intent.putExtra(LOCALIZACION_MESSAGE, message);
        mBroadcaster.sendBroadcast(intent);
    }


    //Al comenzar se comprueban los permisos necesarios y se recoge la ultima posicion conocida
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            if (mCurrentLocation != null) {
                sendResult(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            }
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    //En el momento que se detecta que la posicion ha cambiado se manda un mensaje avisando del cambio y la posicion actual
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        sendResult(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }
}
