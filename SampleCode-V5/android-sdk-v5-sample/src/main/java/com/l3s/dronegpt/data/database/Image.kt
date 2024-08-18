package com.l3s.dronegpt.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import dji.sdk.keyvalue.value.common.LocationCoordinate3D

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(entity = Experiment::class,
                                parentColumns = arrayOf("id"),
                                childColumns = arrayOf("experimentId"),
                                onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["experimentId"])]
)
@TypeConverters(LocationCoordinate3DConverter::class)
data class Image(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    //foreign key
    val experimentId: Int,
    val index: Int,
    val size: Long,
    val dateTimeString: String,
    val captureLocation: LocationCoordinate3D
) {
}

@Dao
interface ImageDao {

    @Query("SELECT * FROM images WHERE experimentId = :experimentId")
    fun getImagesByExperimentId(experimentId: Int): LiveData<List<Image>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertImage(image: Image)

    @Query("DELETE FROM images WHERE id = :id")
    fun deleteImage(id: Int)
    @Query("SELECT * FROM images")
    abstract fun getAllImages(): List<Image>
}
