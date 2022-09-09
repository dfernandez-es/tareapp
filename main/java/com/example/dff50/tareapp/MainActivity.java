package com.example.dff50.tareapp;

import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.dff50.tareapp.db.Definicion;
import com.example.dff50.tareapp.db.Storage;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,  GoogleMap.OnMapLongClickListener, GoogleMap.OnInfoWindowClickListener {


    private static final String TAG = "MainActivity";

    //Codigos que utilizaremos para validar los permisos
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;
    private static final int REQUEST_WIFI_PERMISSION_CODE = 102;
    private static final int REQUEST_BLUE_PERMISSION_CODE = 103;

    //Lo utilizaremos para recibir los mensajes del servicio que indica la posicion actual
    private BroadcastReceiver mLocationReceiver;
    //Lo utilizaremos para almacenar la posicion actual
    private LatLng mCurrentLocation;

    private Intent mRequestLocationIntent;

    //Representara el mapa
    private GoogleMap googleMap;

    private GeofencingRequest geofencingRequest;
    private GoogleApiClient googleApiClient;

    private MarkerOptions markerOptions;

    private Marker currentLocationMarker;
    private PendingIntent pendingIntent;

    //Variable utilizada para identificar si deseamos crear tareas o alarmas
    boolean alarmaOTarea = true;

    //Representa la instancia principal de la aplicacion
    static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        instance = this;

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();


        //Definimos la configuracion del boton que indica si trabajamos con tareas o alarmas
        final FloatingActionButton alarmatarea =  findViewById(R.id.alarmatarea);
        alarmatarea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Modo Tarea
                if(alarmaOTarea){
                        alarmatarea.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#00CB1F")));
                        alarmatarea.setImageResource(android.R.drawable.ic_menu_compass);
                        alarmaOTarea = false;
                }
                //Modo alarma
                else{
                    alarmatarea.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF2D00")));
                    alarmatarea.setImageResource(android.R.drawable.ic_lock_idle_alarm);
                    alarmaOTarea = true;
                }

            }
        });

        //Solicitamos permiso para manipular el wifi
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_WIFI_PERMISSION_CODE);
        }

        //Solicitamos permiso para manipular el bluetooth
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_BLUE_PERMISSION_CODE);
        }

        //Solicitamos permisos para modificar sonido
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !notificationManager.isNotificationPolicyAccessGranted()) {

            Intent intent = new Intent(
                    android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }


        //Solicitamos permiso para conseguir la posicion gps
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }

        //Recibimos las actualizacion de posicion de "LocationUpdaterService"
        mLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCurrentLocation = intent.getParcelableExtra(LocationUpdaterService.LOCALIZACION_MESSAGE);

                //Definimos el color azul del marcador que indica donde nos encontramos
                final BitmapDescriptor color
                        = BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE);

                //Se marca la posicion en la que nos encontramos mediante un marcador que se actualiza en funcion de nuestro movimiento
                try {
                            if (currentLocationMarker != null) {
                                currentLocationMarker.remove();
                            }
                            markerOptions = new MarkerOptions();
                            markerOptions.position(new LatLng(mCurrentLocation.latitude, mCurrentLocation.longitude));
                            markerOptions.title("Ubicación actual");
                            markerOptions.icon(color);
                            currentLocationMarker = googleMap.addMarker(markerOptions);
                        }
                 catch (SecurityException e) {
                }


            }
        };
    }

    //Activamos el servicio que se encarga de actualizar la posicion
    private void startLocationMonitor() {
        mRequestLocationIntent = new Intent(this, LocationUpdaterService.class);
        startService(mRequestLocationIntent);
    }

    //Lanza el servicio que gestiona las geoceldas
    private void startGeofencing() {
        pendingIntent = getGeofencePendingIntent();
    }

    //Servicio de las geoceldas
    private PendingIntent getGeofencePendingIntent() {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(this, GeofenceRegistrationService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    //Se ejecuta al reanudar la aplicacion
    @Override
    protected void onResume() {
        super.onResume();
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocationReceiver, new IntentFilter(LocationUpdaterService.LOCALIZACION_RESULT));
        if (response != ConnectionResult.SUCCESS) {
            Toast.makeText(this, "Error, Google play service no disponible", Toast.LENGTH_SHORT).show();
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, response, 1).show();
        }
    }

    //Se lanza antes de mostrar la aplicacion al usuario.
    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.reconnect();
    }

    //Se lanza cuando la aplicacion no es visible para el usuario.
    @Override
    protected void onStop() {
        super.onStop();
    }

    //Se ejecuta al finalizar la aplicacion, en este caso paramos el servicio que actualiza la posicion gps
    @Override
    protected void onDestroy() {
        stopService(mRequestLocationIntent);
        super.onDestroy();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startGeofencing();
        startLocationMonitor();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());
    }


    @Override
    public void onMapReady(GoogleMap googlemap) {

        googleMap = googlemap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        iniciartMap();
    }


    //Utilizamos este metodo para evitar que la primera vez que ejecutamos la aplicacion la comprobacion de onMapReady no se salte este apartado
    private void iniciartMap() {
        //Se comprueba que se disponga de los permisos necesarios
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Se define la configuracion del mapa
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setMyLocationEnabled(true);

        //Se recargan los markers y areas en el mapa
        repintarMapa();
    }

    //Metodo que se ejecuta despues de conceder o cancelar permisos, si se conceden se ejecuta el metodo iniciartMap
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    iniciartMap();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
                }
                return;
            }
            case REQUEST_WIFI_PERMISSION_CODE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_WIFI_PERMISSION_CODE);
                }
            }
            case REQUEST_BLUE_PERMISSION_CODE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUE_PERMISSION_CODE);
                }
            }
        }
    }


    //Metodo encargado de configurar la geocelda y de llamar al metodo que las dibuja en el mapa
    private void crearGeofence(LatLng latLng, String nombre, int metros, boolean alarma) {


        pendingIntent = getGeofencePendingIntent();

        //Llama al metodo que dibuja el marker y su area en funcion del tipo de geocelda
        if(alarma){
            pintarAlarma(nombre, latLng, metros);
        }
        else{
            pintarTarea(nombre,latLng,metros);
        }


        //Se configura los parametros de la geocelda
        Geofence geofence = new Geofence.Builder()
                .setRequestId(nombre)
                .setCircularRegion(
                        latLng.latitude,
                        latLng.longitude,
                        metros
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();


        geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(geofence)
                .build();

        if (!googleApiClient.isConnected()) {
            Toast.makeText(this, "Error, GoogleApiClient no conectado", Toast.LENGTH_SHORT).show();
        } else {
            try {
                LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, pendingIntent).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Toast.makeText(instance, "Añadido correctamente", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(instance, "Ocurrio un error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }

        }
    }



    //Al pulsar sobre un punto del mapa de forma prolongada se llama a los metodos que permiten configurar la alarma o tarea
    @Override
    public void onMapLongClick(final LatLng latLng) {

        if(!googleApiClient.isConnected()){
            Toast.makeText(this, "Error, GoogleApiClient no conectado", Toast.LENGTH_SHORT).show();
            return;
        }

        //Se ejecuta cuando tenemos seleccionado modo alarma
        if(alarmaOTarea){
            definirAlarma(latLng);
        }

        //Se ejecuta cuando tenemos seleccionado modo tarea
        else{
            definirTarea(latLng);
        }
    }

    //Al pulsar sobre la informacion de un marker recogemos el nombre de la tarea o alarma y procedemos a la eliminacion de la lista de
    //geoceldas y de la base de datos, despues "repintamos" los marcadores y sus areas
    @Override
    public void onInfoWindowClick(Marker marker) {
        final String requestId = marker.getTitle();
        List<String> idList = new ArrayList<>();
        idList.add(requestId);
        LocationServices.GeofencingApi.removeGeofences(googleApiClient, idList);
        Storage.eliminargeofence(marker.getTitle(), instance);
        repintarMapa();
    }

    //Metodo encargado de solicitar la configuracion de la alarma
    public void definirAlarma(LatLng latLng){

        final LatLng latLng2 = latLng;

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.datos, null);
        final EditText titulo = alertLayout.findViewById(R.id.titulo);
        final TextView distan = alertLayout.findViewById(R.id.distan);
        final SeekBar sekbar = alertLayout.findViewById(R.id.distancia);

        sekbar.setProgress(1);
        distan.setText(1 + " metro");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos");

        sekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Si el valor es menor a 100 lo modificamos por 100.
                //Para evitar que se creen geoceldas con tamaños muy pequeños
                if (progress <= 100){
                    progress = 100;
                    sekbar.setProgress(progress);
                }
                distan.setText(progress + " metros");
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });
        sekbar.setProgress(100);

        builder.setView(alertLayout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String tituloText = String.valueOf(titulo.getText());

                //Se comprueba que el titulo no este en blanco, en caso contrario se cancela y se muestra un texto de error
                if(tituloText == null || tituloText.equals("")){
                    Toast.makeText(MainActivity.this, "Error, el titulo no puede estar en blanco", Toast.LENGTH_SHORT).show();
                    return;
                }

                int valorMetros = sekbar.getProgress();
                //Indicamos true indicando que se trata de una alarma
                crearGeofence(latLng2,tituloText,valorMetros,true);
                //Guardamos el marcador en la base de datos
                //Se envia tipo alarma, posicion, metros de la geocelda y la configuracion a 0 de las tareas
                Storage.guardar(tituloText, 0,latLng2, valorMetros, instance,0,0,0,0);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    //Metodo encargado de solicitar la configuracion de las tareas
    public void definirTarea(LatLng latLng){

        final LatLng latLng2 = latLng;

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.datostarea, null);
        final EditText titulo = alertLayout.findViewById(R.id.titulo);
        final TextView distan = alertLayout.findViewById(R.id.distan);
        final SeekBar sekbar = alertLayout.findViewById(R.id.distancia);
        final CheckBox wifiMarcado = alertLayout.findViewById(R.id.wifi);
        final CheckBox bluetoothMarcado = alertLayout.findViewById(R.id.bluetooth);
        final CheckBox nomolestarMarcado = alertLayout.findViewById(R.id.nomolestar);
        final CheckBox vueloMarcado = alertLayout.findViewById(R.id.vuelo);
        final Switch switchWifi = alertLayout.findViewById(R.id.switchWifi);
        final Switch switchMolestar = alertLayout.findViewById(R.id.switchMolestar);
        final Switch switchVuelo = alertLayout.findViewById(R.id.switchVuelo);
        final Switch switchBluetooth = alertLayout.findViewById(R.id.switchBluetooth);



        sekbar.setProgress(1);
        distan.setText(1 + " metro");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Datos");

        sekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                //Si el valor es menor a 100 lo modificamos por 100.
                //Para evitar que se creen geoceldas con tamaños muy pequeños
                if (progress <= 100){
                    progress = 100;
                    sekbar.setProgress(progress);
                }
                distan.setText(progress + " metros");
            }

            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
        });
        sekbar.setProgress(100);


        wifiMarcado.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked)
                {
                    switchWifi.setVisibility(View.VISIBLE);
                }
                else
                {
                    switchWifi.setVisibility(View.INVISIBLE);
                }
            }
        });

        nomolestarMarcado.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked)
                {
                    switchMolestar.setVisibility(View.VISIBLE);
                }
                else
                {
                    switchMolestar.setVisibility(View.INVISIBLE);
                }
            }
        });

        vueloMarcado.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked)
                {
                    switchVuelo.setVisibility(View.VISIBLE);
                }
                else
                {
                    switchVuelo.setVisibility(View.INVISIBLE);
                }
            }
        });

        bluetoothMarcado.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                if(isChecked)
                {
                    switchBluetooth.setVisibility(View.VISIBLE);
                }
                else
                {
                    switchBluetooth.setVisibility(View.INVISIBLE);
                }
            }
        });

        builder.setView(alertLayout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String tituloText = String.valueOf(titulo.getText());

                //Se comprueba que el titulo no este en blanco, en caso contrario se cancela y se muestra un texto de error
                if(tituloText == null || tituloText.equals("")){
                    Toast.makeText(MainActivity.this, "Error, el titulo no puede estar en blanco", Toast.LENGTH_SHORT).show();
                    return;
                }

                int valorMetros = sekbar.getProgress();

                //Variables que utilizaremos para guardar las acciones
                //0 = no realizar ninguna gestion
                //1 = apagar o desactivar
                //2 = encender o habilitar
                int wifi = 0;
                int molestar = 0;
                int vuelo = 0;
                int bluetooth = 0;


                if(wifiMarcado.isChecked()){
                    if(switchWifi.isChecked()){
                        wifi = 2;
                    }
                    else {
                        wifi = 1;
                    }
                }
                if(bluetoothMarcado.isChecked()){
                    if(switchBluetooth.isChecked()){
                        bluetooth = 2;
                    }
                    else {
                        bluetooth = 1;
                    }
                }
                if(nomolestarMarcado.isChecked()){
                    if(switchMolestar.isChecked()){
                        molestar = 2;
                    }
                    else {
                        molestar = 1;
                    }
                }
                if(vueloMarcado.isChecked()){
                    if(switchVuelo.isChecked()){
                        vuelo = 2;
                    }
                    else {
                        vuelo = 1;
                    }
                }

                //Enviamos false indicando que se trata de una tarea
                crearGeofence(latLng2,tituloText,valorMetros,false);
                //Guardamos el marcador en la base de datos junto con la configuracion de las tareas a realizar
                Storage.guardar(tituloText, 1,latLng2, valorMetros, instance,wifi,molestar,vuelo,bluetooth);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    //Metodo encargado de dibujar el marker y el area de una alarma en el mapa
    private void pintarAlarma(String key, LatLng latLng, int metros) {
        googleMap.addMarker(new MarkerOptions()
                .title(key)
                .snippet("Pulsa sobre el texto para elminar alarma")
                .position(latLng));
        googleMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(metros)
                .strokeColor(Color.RED)
                .fillColor(Color.parseColor("#80ff0000")));
    }

    //Metodo encargado de dibujar el marker y el area de una tarea en el mapa
    private void pintarTarea(String key, LatLng latLng, int metros) {
        googleMap.addMarker(new MarkerOptions()
                .title(key)
                .snippet("Pulsa sobre el texto para elminar tarea")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .position(latLng));
        googleMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(metros)
                .strokeColor(Color.GREEN)
                .fillColor(Color.parseColor("#8006A01E")));
    }

    //Pinta en el mapa todas las alarmas y tareas almacenadas en la base de datos
    private void repintarMapa() {
        googleMap.clear();
        try (Cursor cursor = Storage.getCursor(instance)) {
            while (cursor.moveToNext()) {
                String nombre = cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE));
                int tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_TIPO)));
                double lat = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_LAT)));
                double lng = Double.parseDouble(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_LNG)));
                int metros = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_METROS)));

                if(tipo == 0){
                    //Si el tipo es 0 es igual a alarma, dibujamos el circulo en rojo
                    pintarAlarma(nombre, new LatLng(lat, lng), metros);
                }
                else{
                    //Si el tipo es 1 es igual a tarea, dibujamos el circulo en verde
                    pintarTarea(nombre, new LatLng(lat, lng), metros);
                }

            }
        }
    }

    //Metodo encargado de recoger el texto del editex del menu principal, intentar localizar su geolocalizacion y mover la camara hacia dicho punto
    public void busqueda(View view)
    {
        //Este apartado de codigo se utiliza para ocultar el teclado una vez pulsado el boton de busqueda
        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
        }

        EditText location_tf = findViewById(R.id.Ciudad);
        String location = location_tf.getText().toString();
        List<Address> addressList = null;
        if(location != null || !location.equals(""))
        {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location , 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addressList != null && addressList.size() != 0) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude() , address.getLongitude());
                //Movemos la camara hacia el punto encontrado con una configuracion de zoom 13
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,13));
            }
            else {
                Toast.makeText(MainActivity.this, "Localizacion no encontrada", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
