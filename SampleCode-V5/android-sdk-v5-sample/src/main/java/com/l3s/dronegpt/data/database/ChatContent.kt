package com.l3s.dronegpt.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "chatContent",
    foreignKeys = [
        ForeignKey(entity = Experiment::class,
                                parentColumns = arrayOf("id"),
                                childColumns = arrayOf("experimentId"),
                                onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["experimentId"])]
)
data class ChatContent(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    //foreign key
    val experimentId: Int,
    val content: String,
    val isUserContent: Boolean,
    @ColumnInfo(name = "createdOn", defaultValue = "CURRENT_TIMESTAMP")
    val createdOn: String = ""
)

@Dao
interface ChatContentDao {

    @Query("SELECT * FROM chatContent WHERE experimentId = :experimentId ORDER BY createdOn ASC")
    fun getChatContentByExperimentId(experimentId: Int): List<ChatContent>

    @Insert
    fun insertChatContent(content: ChatContent)

    @Query("SELECT * FROM chatContent ORDER BY createdOn ASC")
    abstract fun getAllChatContent(): List<ChatContent>

}