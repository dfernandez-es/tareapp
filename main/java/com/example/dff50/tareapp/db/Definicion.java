package com.example.dff50.tareapp.db;

import android.provider.BaseColumns;


public class Definicion {
    private Definicion() {}

    public static class GeofenceEntry implements BaseColumns {
        public static final String TABLE_NAME = "geofences";
        public static final String COLUMN_NAME_NOMBRE = "nombre";
        public static final String COLUMN_NAME_TIPO = "tipo";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LNG = "lng";
        public static final String COLUMN_NAME_METROS = "metros";
        public static final String COLUMN_NAME_WIFI = "wifi";
        public static final String COLUMN_NAME_BLUE = "blue";
        public static final String COLUMN_NAME_VUELO = "vuelo";
        public static final String COLUMN_NAME_MOLESTAR = "molestar";
    }
}
