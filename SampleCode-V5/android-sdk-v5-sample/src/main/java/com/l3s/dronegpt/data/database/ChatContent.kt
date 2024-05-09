package com.l3s.dronegpt.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "chatContent",
        foreignKeys = [ForeignKey(entity = Experiment::class,
                                    parentColumns = arrayOf("id"),
                                    childColumns = arrayOf("experimentId"),
                                    onDelete = ForeignKey.CASCADE)])
data class ChatContent(

    //foreign key
    val experimentId: Int,
    val content: String,
    val isUserContent: Boolean
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
}

@Dao
interface ChatContentDao {

    @Query("SELECT * FROM chatContent WHERE experimentId = :experimentId")
    fun getChatContentByExperimentId(experimentId: Int): LiveData<List<ChatContent>> //TODO: LiveData?

    @Insert
    fun insertChatContent(content: ChatContent)

    @Query("DELETE FROM chatContent WHERE id = :id")
    fun deleteChatContent(id: Int)
}