package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ConversationEntity
import com.example.model.MessageEntity
import com.example.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationById(id: Long): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<ConversationEntity>)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp, unreadCount = :unreadCount WHERE id = :id")
    suspend fun updateLastMessage(id: Long, lastMessage: String, timestamp: Long, unreadCount: Int)

    @Query("UPDATE conversations SET unreadCount = 0, lastReadAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: Long, readAt: Long)

    @Query("UPDATE conversations SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePin(id: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("UPDATE conversations SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun updateBlockedStatus(id: Long, isBlocked: Boolean)

    @Query("UPDATE conversations SET isAccepted = :isAccepted, isUnknownContact = :isUnknownContact WHERE id = :id")
    suspend fun updateAcceptedStatus(id: Long, isAccepted: Boolean, isUnknownContact: Boolean)

    @Query("SELECT * FROM conversations WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun findConversationByPhone(phoneNumber: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE title = :title LIMIT 1")
    suspend fun findConversationByTitle(title: String): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getCount(): Int

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationOnce(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE remoteId = :remoteId AND remoteId != '' LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): ConversationEntity?

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :id")
    suspend fun incrementUnread(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT DISTINCT conversationId FROM messages WHERE content LIKE '%' || :query || '%'")
    fun getConversationIdsWithMatchingMessages(query: String): Flow<List<Long>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageOnce(id: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE remoteId = :remoteId AND remoteId != '' LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): MessageEntity?

    @Query("SELECT remoteId FROM messages WHERE conversationId = :conversationId AND remoteId != ''")
    suspend fun getRemoteIdsForConversation(conversationId: Long): List<String>

    @Query("DELETE FROM messages WHERE conversationId = :conversationId AND remoteId = :remoteId AND remoteId != ''")
    suspend fun deleteByRemoteId(conversationId: Long, remoteId: String)

    @Query("UPDATE messages SET reactionEmoji = :reaction, status = :status, readTimestamp = :readTimestamp WHERE id = :id")
    suspend fun updateFromRemote(id: Long, reaction: String, status: MessageStatus, readTimestamp: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET reactionEmoji = :reaction WHERE id = :messageId")
    suspend fun updateReaction(messageId: Long, reaction: String)

    @Query("UPDATE messages SET status = :status, deliveredTimestamp = :deliveredTimestamp WHERE id = :messageId")
    suspend fun updateMessageDelivered(messageId: Long, status: MessageStatus, deliveredTimestamp: Long)

    @Query("UPDATE messages SET status = :status, readTimestamp = :readTimestamp WHERE id = :messageId")
    suspend fun updateMessageRead(messageId: Long, status: MessageStatus, readTimestamp: Long)

    @Query("UPDATE messages SET status = :status, readTimestamp = :readTimestamp WHERE conversationId = :conversationId AND senderId = 'me' AND status != 'READ'")
    suspend fun markAllMyMessagesRead(conversationId: Long, status: MessageStatus, readTimestamp: Long)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun clearMessagesForConversation(conversationId: Long)
}
