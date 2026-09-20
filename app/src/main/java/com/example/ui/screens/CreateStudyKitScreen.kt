package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.GradeSelectorRow
import com.example.ui.components.PhotoUploadSection
import com.example.ui.components.SubjectSelectorRow
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun CreateStudyKitScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubject.collectAsStateWithLifecycle()
    val customSubject by viewModel.customSubject.collectAsStateWithLifecycle()
    val examTitle by viewModel.examTitle.collectAsStateWithLifecycle()
    val pastPaperText by viewModel.pastPaperText.collectAsStateWithLifecycle()
    val syllabusText by viewModel.syllabusText.collectAsStateWithLifecycle()
    val chapterNotesText by viewModel.chapterNotesText.collectAsStateWithLifecycle()
    val uploadedPhotos by viewModel.uploadedPhotos.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisStatus by viewModel.analysisStatus.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var showPastPaperText by remember { mutableStateOf(false) }
    var showSyllabusText by remember { mutableStateOf(false) }
    var showChapterNotesText by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_study_hero),
                        contentDescription = "Exam study hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Class 5 to 10",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exam Study Master",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Feed in your previous question papers, upcoming syllabus, and chapter photos. AI will craft high-yield questions, revision notes, and exam tips!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Quick Load Template Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Demo / Sample Presets",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.loadSampleData(10, "Science") },
                        modifier = Modifier.weight(1f).testTag("btn_sample_10"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Class 10 (Board)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.loadSampleData(8, "Science") },
                        modifier = Modifier.weight(1f).testTag("btn_sample_8"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Class 8", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.loadSampleData(6, "Science") },
                        modifier = Modifier.weight(1f).testTag("btn_sample_6"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Class 6", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Step 1: Select Class
        Column {
            Text(
                text = "1. Select Student Grade / Class",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            GradeSelectorRow(
                selectedGrade = selectedClass,
                onGradeSelected = { viewModel.setSelectedClass(it) }
            )
        }

        // Step 2: Subject & Exam Title
        Column {
            Text(
                text = "2. Subject & Exam Name",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            SubjectSelectorRow(
                selectedSubject = selectedSubject,
                onSubjectSelected = { viewModel.setSelectedSubject(it) }
            )

            if (selectedSubject == "Custom") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customSubject,
                    onValueChange = { viewModel.setCustomSubject(it) },
                    label = { Text("Enter Subject (e.g. Economics, History)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_subject"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = examTitle,
                onValueChange = { viewModel.setExamTitle(it) },
                label = { Text("Exam Title (e.g. Term 1 Finals, Unit Test 2)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_exam_title"),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                }
            )
        }

        // Step 3: Previous Question Papers
        Column {
            Text(
                text = "3. Previous Year Question Papers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Upload photos of past test papers or type questions. AI detects repeated patterns and mark distributions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            PhotoUploadSection(
                title = "Past Question Paper Photos",
                subtitle = "Take or pick photos of question papers",
                labelTag = "Previous Paper",
                photos = uploadedPhotos,
                onAddPhotos = { uris -> viewModel.addPhotos(uris, "Previous Paper") },
                onRemovePhoto = { index -> viewModel.removePhoto(index) }
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { showPastPaperText = !showPastPaperText },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (showPastPaperText) "Hide Text Input" else "+ Paste Questions as Text", fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = showPastPaperText) {
                OutlinedTextField(
                    value = pastPaperText,
                    onValueChange = { viewModel.setPastPaperText(it) },
                    label = { Text("Paste Previous Exam Questions here") },
                    placeholder = { Text("e.g. 2023: Explain Ohm's law (5 marks)\n2024: Define Photosynthesis (2 marks)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .padding(top = 6.dp)
                        .testTag("input_past_paper_text"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6
                )
            }
        }

        // Step 4: Upcoming Exam Syllabus
        Column {
            Text(
                text = "4. Upcoming Exam Syllabus",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Provide chapters or syllabus sheet for the upcoming exam.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            PhotoUploadSection(
                title = "Syllabus Photos",
                subtitle = "Upload syllabus sheet or table of contents",
                labelTag = "Syllabus",
                photos = uploadedPhotos,
                onAddPhotos = { uris -> viewModel.addPhotos(uris, "Syllabus") },
                onRemovePhoto = { index -> viewModel.removePhoto(index) }
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { showSyllabusText = !showSyllabusText },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (showSyllabusText) "Hide Syllabus Text" else "+ Type Syllabus Topics", fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = showSyllabusText) {
                OutlinedTextField(
                    value = syllabusText,
                    onValueChange = { viewModel.setSyllabusText(it) },
                    label = { Text("Type Chapters or Topics in Syllabus") },
                    placeholder = { Text("e.g. Chapter 1: Chemical Reactions, Chapter 6: Life Processes, Chapter 12: Electricity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(top = 6.dp)
                        .testTag("input_syllabus_text"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
            }
        }

        // Step 5: Chapter Photos & Notes
        Column {
            Text(
                text = "5. Chapter Photos & Notes (Optional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Snap photos of textbook pages, notebook summaries, or diagrams to extract key points from.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            PhotoUploadSection(
                title = "Textbook / Chapter Photos",
                subtitle = "Add textbook pages, diagrams, or class notes",
                labelTag = "Chapter Photo",
                photos = uploadedPhotos,
                onAddPhotos = { uris -> viewModel.addPhotos(uris, "Chapter Photo") },
                onRemovePhoto = { index -> viewModel.removePhoto(index) }
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { showChapterNotesText = !showChapterNotesText },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (showChapterNotesText) "Hide Notes Text" else "+ Specific Topics to Focus", fontSize = 12.sp)
                }
            }

            AnimatedVisibility(visible = showChapterNotesText) {
                OutlinedTextField(
                    value = chapterNotesText,
                    onValueChange = { viewModel.setChapterNotesText(it) },
                    label = { Text("Special instructions or focus areas") },
                    placeholder = { Text("e.g. Include numerical problems, highlight diagrams, give 5-mark answer keys.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(top = 6.dp)
                        .testTag("input_chapter_notes_text"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Generate Button
        Button(
            onClick = { viewModel.generateStudyKit() },
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("generate_study_kit_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Analyzing Papers & Generating...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Study Kit & Important Questions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isAnalyzing && analysisStatus.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analysisStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
