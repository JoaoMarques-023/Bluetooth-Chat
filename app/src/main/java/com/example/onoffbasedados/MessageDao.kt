package com.example.onoffbasedados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend   fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages")
    fun getAllMessages(): List<MessageEntity>
}
