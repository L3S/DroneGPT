package com.l3s.dronegpt.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import dji.sdk.keyvalue.value.common.LocationCoordinate3D

class LocationCoordinate3DConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLocationCoordinate3D(coordinates: LocationCoordinate3D): String {
        return gson.toJson(coordinates)
    }

    @TypeConverter
    fun toLocationCoordinate3D(locationCoordinate3DString: String): LocationCoordinate3D {
        return gson.fromJson(locationCoordinate3DString, LocationCoordinate3D::class.java)
    }

}
