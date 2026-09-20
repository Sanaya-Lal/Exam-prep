package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_kits")
data class StudyKitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val classGrade: Int, // 5 to 10
    val subject: String,
    val summary: String,
    val examStrategyJson: String,
    val notesJson: String,
    val questionsJson: String,
    val syllabusTopicsJson: String,
    val previousPaperSummary: String = "",
    val attachedPhotosCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
