package com.example.dff50.tareapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.dff50.tareapp.db.Definicion;
import com.example.dff50.tareapp.db.Storage;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.Random;

import static android.provider.Settings.Global.AIRPLANE_MODE_ON;

public class GeofenceRegistrationService extends IntentService {

    private static final String TAG = "GeoIntentService";

    public GeofenceRegistrationService() {
        super(TAG);
    }

    //Metodo encargado de detectar los eventos de entrada en las geovallas
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.d(TAG, "GeofencingEvent error " + geofencingEvent.getErrorCode());
        } else {
            int transaction = geofencingEvent.getGeofenceTransition();
            List<Geofence> geofences = geofencingEvent.getTriggeringGeofences();
            Geofence geofence = geofences.get(0);
            //Si el evento que se produce es de entrada
            if (transaction == Geofence.GEOFENCE_TRANSITION_ENTER) {
                //Se muestra notificacion en el dispositivo
                sendNotification(geofence.getRequestId());
                //Se ejecuta tarea (Si las hubiera)
                ejecutarTarea(geofence.getRequestId());
            }
        }
    }

    //Metodo encargado de recibir una cadena y mostrarla como notificacion en el dispositivo, tambien ejecuta vibracion, sonido y activa led.
    private void sendNotification(String nombreAviso) {

        //Generamos un numero aleatorio
        //Lo utilizaremos para indicar un id diferente a la hora de mostrar la notificacion, para conseguir que se muestren varias notificaciones
        //y no sustituya la ultima a la anterior.
        Random r = new Random();
        int id = r.nextInt(500);

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);

        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle("TareAPP: Aviso")
                .setContentText(nombreAviso)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(notificationPendingIntent);

        builder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //Proporcionamos un id aleatorio
        mNotificationManager.notify(id, builder.build());
    }


    //Metodo encargado de recoger los datos de la base de datos de la tarea con el nombre que se envia
    private void ejecutarTarea(String nombre) {

        int tipo = 0;
        int wifi = 0;
        int blue = 0;
        int molestar = 0;
        int vuelo = 0;

        try (Cursor cursor = Storage.tareasRealizar(nombre, MainActivity.instance)) {
            while (cursor.moveToNext()) {
                tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_TIPO)));
                wifi = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_WIFI)));
                blue = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_BLUE)));
                molestar = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_MOLESTAR)));
                vuelo = Integer.parseInt(cursor.getString(cursor.getColumnIndex(Definicion.GeofenceEntry.COLUMN_NAME_VUELO)));
            }
        }
        //Despues de recoger los datos se comprueba por cada uno de ellos si es necesario realizar alguna accion:
        //0 = no realizar ninguna gestion
        //1 = apagar o desactivar
        //2 = encender o habilitar
        if (tipo == 1) {
            if (wifi == 1) {
                WifiManager wifiManager = (WifiManager)MainActivity.instance.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(false);
            } else if (wifi == 2) {
                WifiManager wifiManager = (WifiManager)MainActivity.instance.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(true);
            }

            if (blue == 1) {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                mBluetoothAdapter.disable();
            } else if (blue == 2){
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                mBluetoothAdapter.enable();
            }

            if (molestar == 1) {
                final AudioManager mode = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                mode.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else if(molestar == 2){
                final AudioManager mode = (AudioManager) this.getSystemService(MainActivity.instance.AUDIO_SERVICE);
                mode.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }

            if (vuelo == 1) {
                ContentResolver contentResolver = MainActivity.instance.getContentResolver();
                Settings.Global.getInt(contentResolver, AIRPLANE_MODE_ON, 1);
            } else if(vuelo == 2){
                ContentResolver contentResolver = MainActivity.instance.getContentResolver();
                Settings.Global.getInt(contentResolver, AIRPLANE_MODE_ON, 0);
            }
        }
    }


}
