package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuestionItem(
    val question: String = "",
    val marks: String = "2-3 Marks",
    val type: String = "Important", // "High Probability", "Short Answer", "Long Answer", "MCQ"
    val chapterTopic: String = "",
    val whyImportant: String = "",
    val answerHint: String = "",
    var isMastered: Boolean = false
)

@JsonClass(generateAdapter = true)
data class NoteSection(
    val heading: String = "",
    val keyPoints: List<String> = emptyList(),
    val formulasOrDefinitions: List<String> = emptyList(),
    val memoryTrick: String = ""
)

@JsonClass(generateAdapter = true)
data class StudyKitData(
    val title: String = "",
    val classGrade: Int = 10,
    val subject: String = "Science",
    val summary: String = "",
    val examStrategyTips: List<String> = emptyList(),
    val notes: List<NoteSection> = emptyList(),
    val questions: List<QuestionItem> = emptyList(),
    val syllabusTopics: List<String> = emptyList()
)

data class UploadedPhoto(
    val uriString: String,
    val label: String, // "Previous Paper", "Syllabus", "Chapter Photo"
    val base64Data: String? = null
)
