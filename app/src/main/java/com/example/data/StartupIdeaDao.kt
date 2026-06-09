package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StartupIdeaDao {
    @Query("SELECT * FROM ideas ORDER BY timestamp DESC")
    fun getAllIdeasFlow(): Flow<List<StartupIdeaEntity>>

    @Query("SELECT * FROM ideas WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoritesFlow(): Flow<List<StartupIdeaEntity>>

    @Query("SELECT * FROM ideas WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getIdeasBySessionFlow(sessionId: String): Flow<List<StartupIdeaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdeas(ideas: List<StartupIdeaEntity>)

    @Query("UPDATE ideas SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE ideas SET isFavorite = :isFavorite WHERE startupName = :startupName")
    suspend fun updateFavoriteByName(startupName: String, isFavorite: Boolean)

    @Query("DELETE FROM ideas WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM ideas")
    suspend fun clearAll()
}
