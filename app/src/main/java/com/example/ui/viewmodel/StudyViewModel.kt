package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudyDatabase
import com.example.data.local.StudyKitEntity
import com.example.data.local.StudyKitRepository
import com.example.data.model.QuestionItem
import com.example.data.model.StudyKitData
import com.example.data.model.UploadedPhoto
import com.example.data.remote.GeminiStudyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab {
    CREATE,
    CURRENT_KIT,
    SAVED_KITS,
    QUIZ
}

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyKitRepository
    private val geminiService: GeminiStudyService

    init {
        val db = StudyDatabase.getDatabase(application)
        repository = StudyKitRepository(db.studyKitDao())
        geminiService = GeminiStudyService(application)
    }

    val savedKits: StateFlow<List<StudyKitEntity>> = repository.getAllKits()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentNavTab = MutableStateFlow(AppNavTab.CREATE)
    val currentNavTab: StateFlow<AppNavTab> = _currentNavTab.asStateFlow()

    // Form inputs for kit generation
    private val _selectedClass = MutableStateFlow(10)
    val selectedClass: StateFlow<Int> = _selectedClass.asStateFlow()

    private val _selectedSubject = MutableStateFlow("Science")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _customSubject = MutableStateFlow("")
    val customSubject: StateFlow<String> = _customSubject.asStateFlow()

    private val _examTitle = MutableStateFlow("")
    val examTitle: StateFlow<String> = _examTitle.asStateFlow()

    private val _pastPaperText = MutableStateFlow("")
    val pastPaperText: StateFlow<String> = _pastPaperText.asStateFlow()

    private val _syllabusText = MutableStateFlow("")
    val syllabusText: StateFlow<String> = _syllabusText.asStateFlow()

    private val _chapterNotesText = MutableStateFlow("")
    val chapterNotesText: StateFlow<String> = _chapterNotesText.asStateFlow()

    private val _uploadedPhotos = MutableStateFlow<List<UploadedPhoto>>(emptyList())
    val uploadedPhotos: StateFlow<List<UploadedPhoto>> = _uploadedPhotos.asStateFlow()

    // Loading & status
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisStatus = MutableStateFlow("")
    val analysisStatus: StateFlow<String> = _analysisStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Currently active / viewed study kit
    private val _activeKit = MutableStateFlow<StudyKitData?>(null)
    val activeKit: StateFlow<StudyKitData?> = _activeKit.asStateFlow()

    private val _activeKitId = MutableStateFlow<Long?>(null)
    val activeKitId: StateFlow<Long?> = _activeKitId.asStateFlow()

    // Filter for saved kits
    private val _gradeFilter = MutableStateFlow<Int?>(null)
    val gradeFilter: StateFlow<Int?> = _gradeFilter.asStateFlow()

    fun setNavTab(tab: AppNavTab) {
        _currentNavTab.value = tab
    }

    fun setSelectedClass(grade: Int) {
        _selectedClass.value = grade
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun setCustomSubject(custom: String) {
        _customSubject.value = custom
    }

    fun setExamTitle(title: String) {
        _examTitle.value = title
    }

    fun setPastPaperText(text: String) {
        _pastPaperText.value = text
    }

    fun setSyllabusText(text: String) {
        _syllabusText.value = text
    }

    fun setChapterNotesText(text: String) {
        _chapterNotesText.value = text
    }

    fun setGradeFilter(grade: Int?) {
        _gradeFilter.value = grade
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun addPhotos(uris: List<Uri>, label: String) {
        val current = _uploadedPhotos.value.toMutableList()
        uris.forEach { uri ->
            current.add(UploadedPhoto(uriString = uri.toString(), label = label))
        }
        _uploadedPhotos.value = current
    }

    fun removePhoto(index: Int) {
        val current = _uploadedPhotos.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _uploadedPhotos.value = current
        }
    }

    fun loadSampleData(grade: Int, subject: String) {
        _selectedClass.value = grade
        _selectedSubject.value = subject
        _examTitle.value = "Class $grade $subject Final Exam Prep"

        when (grade) {
            5, 6 -> {
                _syllabusText.value = "Chapter 1: Food & Components of Food, Chapter 2: Sorting Materials, Chapter 3: Plants & Photosynthesis"
                _pastPaperText.value = "Q1: Name two energy-giving foods.\nQ2: What is photosynthesis? (5 marks with diagram)\nQ3: Difference between herbivores and carnivores.\nQ4: Deficiency disease of Vitamin A."
                _chapterNotesText.value = "Focus on balanced diet nutrients (Carbs, Fats, Proteins, Vitamins) and leaf diagram parts."
            }
            7, 8 -> {
                _syllabusText.value = "Unit 1: Force and Pressure, Unit 2: Cell - Structure and Functions, Unit 3: Friction"
                _pastPaperText.value = "Q1: Why do camels walk easily in desert sand? (2 marks)\nQ2: Plant cell vs Animal cell 3 differences (3 marks)\nQ3: Define friction, advantages, disadvantages & methods to reduce (5 marks)"
                _chapterNotesText.value = "Key formulas: Pressure = Force / Area. Label nucleus, cytoplasm, cell wall in plant cell."
            }
            else -> {
                _syllabusText.value = "Chapter 1: Chemical Reactions & Equations, Chapter 6: Life Processes, Chapter 12: Electricity"
                _pastPaperText.value = "2024: Aerobic vs Anaerobic respiration (3 marks)\n2023: State Ohm's Law and calculate equivalent resistance in parallel (5 marks)\n2022: Photolytic decomposition of AgCl (2 marks)\n2024: Function of nephron in kidneys (3 marks)"
                _chapterNotesText.value = "Must cover: V = IR, P = VI, resistor formulas, chemical equation balancing, mitochondria vs cytoplasm respiration steps."
            }
        }
    }

    fun generateStudyKit() {
        val grade = _selectedClass.value
        val subject = if (_selectedSubject.value == "Custom" && _customSubject.value.isNotBlank()) {
            _customSubject.value
        } else {
            _selectedSubject.value
        }
        val title = if (_examTitle.value.isNotBlank()) _examTitle.value else "Class $grade $subject Study Guide"

        _isAnalyzing.value = true
        _analysisStatus.value = "Analyzing previous papers and syllabus with Gemini AI..."
        _errorMessage.value = null

        val photoPairs = _uploadedPhotos.value.map {
            Uri.parse(it.uriString) to it.label
        }

        viewModelScope.launch {
            val result = geminiService.generateStudyKit(
                classGrade = grade,
                subject = subject,
                examTitle = title,
                pastPaperText = _pastPaperText.value,
                syllabusText = _syllabusText.value,
                chapterNotesText = _chapterNotesText.value,
                photoUris = photoPairs
            )

            result.fold(
                onSuccess = { kitData ->
                    _analysisStatus.value = "Saving study kit and question bank to local database..."
                    val newId = repository.saveKit(
                        data = kitData,
                        previousPaperSummary = _pastPaperText.value.take(200),
                        attachedPhotosCount = _uploadedPhotos.value.size
                    )
                    _activeKit.value = kitData
                    _activeKitId.value = newId
                    _isAnalyzing.value = false
                    _currentNavTab.value = AppNavTab.CURRENT_KIT
                },
                onFailure = { error ->
                    _isAnalyzing.value = false
                    _errorMessage.value = error.message ?: "Failed to generate study kit. Please check inputs."
                }
            )
        }
    }

    fun openSavedKit(entity: StudyKitEntity) {
        val parsed = repository.parseKitData(entity)
        _activeKit.value = parsed
        _activeKitId.value = entity.id
        _currentNavTab.value = AppNavTab.CURRENT_KIT
    }

    fun toggleFavorite(entity: StudyKitEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity.id, !entity.isFavorite)
        }
    }

    fun deleteKit(id: Long) {
        viewModelScope.launch {
            repository.deleteKit(id)
            if (_activeKitId.value == id) {
                _activeKit.value = null
                _activeKitId.value = null
                _currentNavTab.value = AppNavTab.SAVED_KITS
            }
        }
    }

    fun toggleQuestionMastery(questionIndex: Int) {
        val current = _activeKit.value ?: return
        val currentQuestions = current.questions.toMutableList()
        if (questionIndex in currentQuestions.indices) {
            val item = currentQuestions[questionIndex]
            val updatedItem = item.copy(isMastered = !item.isMastered)
            currentQuestions[questionIndex] = updatedItem
            val updatedKit = current.copy(questions = currentQuestions)
            _activeKit.value = updatedKit

            val kitId = _activeKitId.value
            if (kitId != null) {
                viewModelScope.launch {
                    repository.updateQuestions(kitId, currentQuestions)
                }
            }
        }
    }

    fun exportStudyNotesText(): String {
        val kit = _activeKit.value ?: return ""
        val sb = StringBuilder()
        sb.appendLine("=============================")
        sb.appendLine("${kit.title} (Class ${kit.classGrade} - ${kit.subject})")
        sb.appendLine("=============================\n")

        sb.appendLine("EXAM STRATEGY & TIPS:")
        kit.examStrategyTips.forEachIndexed { i, tip ->
            sb.appendLine("${i + 1}. $tip")
        }
        sb.appendLine()

        sb.appendLine("IMPORTANT QUESTIONS (FROM PAST PAPERS & SYLLABUS):")
        kit.questions.forEachIndexed { i, q ->
            sb.appendLine("[Q${i + 1}] (${q.marks} | ${q.type})")
            sb.appendLine("Question: ${q.question}")
            sb.appendLine("Topic: ${q.chapterTopic}")
            sb.appendLine("Why Important: ${q.whyImportant}")
            sb.appendLine("Answer / Hint: ${q.answerHint}")
            sb.appendLine("---")
        }
        sb.appendLine()

        sb.appendLine("REVISION NOTES & SUMMARY:")
        kit.notes.forEach { note ->
            sb.appendLine("• ${note.heading}")
            note.keyPoints.forEach { pt ->
                sb.appendLine("  - $pt")
            }
            if (note.formulasOrDefinitions.isNotEmpty()) {
                sb.appendLine("  * Key Rules/Definitions:")
                note.formulasOrDefinitions.forEach { f ->
                    sb.appendLine("    $f")
                }
            }
            if (note.memoryTrick.isNotBlank()) {
                sb.appendLine("  * Memory Tip: ${note.memoryTrick}")
            }
            sb.appendLine()
        }

        return sb.toString()
    }
}
