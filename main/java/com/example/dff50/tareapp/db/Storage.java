package com.example.dff50.tareapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;


public class Storage {
    private static final String TAG = "Storage";

    public static void guardar(String nombre,int tipo ,LatLng latLng, int metros, Context cont, int wifi, int molestar, int vuelo, int bluetooth ) {
        DbHelper helper = DbHelper.get(cont);

        try {
            ContentValues values = new ContentValues();
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE, nombre);
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_TIPO, tipo + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_METROS, metros + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_LAT, latLng.latitude + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_LNG, latLng.longitude + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_WIFI, wifi + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_MOLESTAR, molestar + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_VUELO, vuelo + "");
            values.put(Definicion.GeofenceEntry.COLUMN_NAME_BLUE, bluetooth + "");

            helper.getWritableDatabase().insert(Definicion.GeofenceEntry.TABLE_NAME, null, values);
        }catch (Exception e){
            Log.e(TAG, "Error al guardarlo en la base de datos " + e);
        }
    }

    public static Cursor getCursor(Context cont){
        String[] columns = new String[]{Definicion.GeofenceEntry._ID, Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE, Definicion.GeofenceEntry.COLUMN_NAME_TIPO , Definicion.GeofenceEntry.COLUMN_NAME_LNG, Definicion.GeofenceEntry.COLUMN_NAME_LAT, Definicion.GeofenceEntry.COLUMN_NAME_METROS,
        Definicion.GeofenceEntry.COLUMN_NAME_WIFI, Definicion.GeofenceEntry.COLUMN_NAME_MOLESTAR, Definicion.GeofenceEntry.COLUMN_NAME_VUELO, Definicion.GeofenceEntry.COLUMN_NAME_BLUE};
        Cursor cursor = DbHelper.get(cont).getReadableDatabase().query(Definicion.GeofenceEntry.TABLE_NAME, columns, null, null, null, null, Definicion.GeofenceEntry._ID + " DESC");
        return cursor;
    }

    public static void eliminargeofence(String nombre, Context cont) {
        String where = Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE + " = '" + nombre + "'";
        DbHelper.get(cont).getReadableDatabase().delete(Definicion.GeofenceEntry.TABLE_NAME, where, null);
    }

    public static Cursor tareasRealizar(String nombre, Context cont) {
        Cursor cursor = DbHelper.get(cont).getReadableDatabase().rawQuery("SELECT "+ Definicion.GeofenceEntry.COLUMN_NAME_TIPO + ","
                + Definicion.GeofenceEntry.COLUMN_NAME_WIFI +","+ Definicion.GeofenceEntry.COLUMN_NAME_BLUE +","+ Definicion.GeofenceEntry.COLUMN_NAME_VUELO+","+ Definicion.GeofenceEntry.COLUMN_NAME_MOLESTAR
                +" FROM " + Definicion.GeofenceEntry.TABLE_NAME + " WHERE "+ Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE + " = ?" , new String[] {nombre});
        return cursor;
    }

}
