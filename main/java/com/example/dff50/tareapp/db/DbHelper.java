package com.example.dff50.tareapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "geofences.db";


    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Definicion.GeofenceEntry.TABLE_NAME + " (" +
                    Definicion.GeofenceEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Definicion.GeofenceEntry.COLUMN_NAME_NOMBRE + " TEXT," +
                    Definicion.GeofenceEntry.COLUMN_NAME_TIPO + " INTEGER," +
                    Definicion.GeofenceEntry.COLUMN_NAME_LAT + " TEXT," +
                    Definicion.GeofenceEntry.COLUMN_NAME_LNG + " TEXT," +
                    Definicion.GeofenceEntry.COLUMN_NAME_METROS + " INTEGER," +
                    Definicion.GeofenceEntry.COLUMN_NAME_WIFI + " INTEGER," +
                    Definicion.GeofenceEntry.COLUMN_NAME_MOLESTAR + " INTEGER," +
                    Definicion.GeofenceEntry.COLUMN_NAME_VUELO + " INTEGER," +
                    Definicion.GeofenceEntry.COLUMN_NAME_BLUE + " INTEGER)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Definicion.GeofenceEntry.TABLE_NAME;


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
       db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static DbHelper get(Context cont) {
        return new DbHelper(cont);
    }
}