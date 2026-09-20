package com.example.data.local

import com.example.data.model.NoteSection
import com.example.data.model.QuestionItem
import com.example.data.model.StudyKitData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudyKitRepository(private val dao: StudyKitDao) {

    private val moshi: Moshi = Moshi.Builder().build()
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val questionsListType = Types.newParameterizedType(List::class.java, QuestionItem::class.java)
    private val questionsAdapter = moshi.adapter<List<QuestionItem>>(questionsListType)

    private val notesListType = Types.newParameterizedType(List::class.java, NoteSection::class.java)
    private val notesAdapter = moshi.adapter<List<NoteSection>>(notesListType)

    fun getAllKits(): Flow<List<StudyKitEntity>> = dao.getAllKits()

    fun getKitsByGrade(grade: Int): Flow<List<StudyKitEntity>> = dao.getKitsByGrade(grade)

    fun getFavorites(): Flow<List<StudyKitEntity>> = dao.getFavoriteKits()

    suspend fun getKitById(id: Long): StudyKitEntity? = dao.getKitById(id)

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = dao.toggleFavorite(id, isFav)

    suspend fun deleteKit(id: Long) = dao.deleteKitById(id)

    suspend fun saveKit(
        data: StudyKitData,
        previousPaperSummary: String,
        attachedPhotosCount: Int
    ): Long {
        val entity = StudyKitEntity(
            title = data.title,
            classGrade = data.classGrade,
            subject = data.subject,
            summary = data.summary,
            examStrategyJson = stringListAdapter.toJson(data.examStrategyTips),
            notesJson = notesAdapter.toJson(data.notes),
            questionsJson = questionsAdapter.toJson(data.questions),
            syllabusTopicsJson = stringListAdapter.toJson(data.syllabusTopics),
            previousPaperSummary = previousPaperSummary,
            attachedPhotosCount = attachedPhotosCount,
            createdAt = System.currentTimeMillis(),
            isFavorite = false
        )
        return dao.insertKit(entity)
    }

    suspend fun updateQuestions(kitId: Long, updatedQuestions: List<QuestionItem>) {
        val existing = dao.getKitById(kitId) ?: return
        val updated = existing.copy(
            questionsJson = questionsAdapter.toJson(updatedQuestions)
        )
        dao.updateKit(updated)
    }

    fun parseKitData(entity: StudyKitEntity): StudyKitData {
        val tips = runCatching { stringListAdapter.fromJson(entity.examStrategyJson) }.getOrNull() ?: emptyList()
        val notes = runCatching { notesAdapter.fromJson(entity.notesJson) }.getOrNull() ?: emptyList()
        val questions = runCatching { questionsAdapter.fromJson(entity.questionsJson) }.getOrNull() ?: emptyList()
        val topics = runCatching { stringListAdapter.fromJson(entity.syllabusTopicsJson) }.getOrNull() ?: emptyList()

        return StudyKitData(
            title = entity.title,
            classGrade = entity.classGrade,
            subject = entity.subject,
            summary = entity.summary,
            examStrategyTips = tips,
            notes = notes,
            questions = questions,
            syllabusTopics = topics
        )
    }
}
