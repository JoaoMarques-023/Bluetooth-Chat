package com.example.onoffbasedados

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "sender_name")
    val senderName: String,

    @ColumnInfo(name = "message")
    val message: String
)
