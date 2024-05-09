package com.l3s.dronegpt.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import dji.sdk.keyvalue.value.common.LocationCoordinate3D

@Entity(tableName = "images",
        foreignKeys = [ForeignKey(entity = Experiment::class,
                                    parentColumns = arrayOf("id"),
                                    childColumns = arrayOf("experimentId"),
                                    onDelete = ForeignKey.CASCADE)])
data class Image(

    //foreign key
    val experimentId: Int,
    val localURI: String,
    val remoteURI: String,
    val captureLocation: LocationCoordinate3D
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
}

@Dao
interface ImageDao {

    @Query("SELECT * FROM images WHERE experimentId = :experimentId")
    fun getImagesByExperimentId(experimentId: Int): LiveData<List<Image>>

    @Insert
    fun insertImage(image: Image)

    @Query("DELETE FROM images WHERE id = :id")
    fun deleteImage(id: Int)
}
