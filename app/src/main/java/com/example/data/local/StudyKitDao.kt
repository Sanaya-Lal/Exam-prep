package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyKitDao {
    @Query("SELECT * FROM study_kits ORDER BY createdAt DESC")
    fun getAllKits(): Flow<List<StudyKitEntity>>

    @Query("SELECT * FROM study_kits WHERE classGrade = :grade ORDER BY createdAt DESC")
    fun getKitsByGrade(grade: Int): Flow<List<StudyKitEntity>>

    @Query("SELECT * FROM study_kits WHERE id = :id LIMIT 1")
    suspend fun getKitById(id: Long): StudyKitEntity?

    @Query("SELECT * FROM study_kits WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteKits(): Flow<List<StudyKitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKit(kit: StudyKitEntity): Long

    @Update
    suspend fun updateKit(kit: StudyKitEntity)

    @Query("UPDATE study_kits SET isFavorite = :isFav WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFav: Boolean)

    @Query("DELETE FROM study_kits WHERE id = :id")
    suspend fun deleteKitById(id: Long)
}
