package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CreateStudyKitScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SavedKitsScreen
import com.example.ui.screens.StudyKitDetailScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ExamPrepApp()
            }
        }
    }
}

@Composable
fun ExamPrepApp(
    viewModel: StudyViewModel = viewModel()
) {
    val currentNavTab by viewModel.currentNavTab.collectAsStateWithLifecycle()
    val activeKit by viewModel.activeKit.collectAsStateWithLifecycle()
    val savedKits by viewModel.savedKits.collectAsStateWithLifecycle()

    val showBottomBar = currentNavTab != AppNavTab.QUIZ

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentNavTab == AppNavTab.CREATE,
                        onClick = { viewModel.setNavTab(AppNavTab.CREATE) },
                        icon = {
                            Icon(
                                imageVector = if (currentNavTab == AppNavTab.CREATE) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "Create Kit"
                            )
                        },
                        label = { Text("Generate", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_create_tab")
                    )

                    NavigationBarItem(
                        selected = currentNavTab == AppNavTab.CURRENT_KIT,
                        onClick = { viewModel.setNavTab(AppNavTab.CURRENT_KIT) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (activeKit != null) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("${activeKit?.questions?.size ?: 0}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentNavTab == AppNavTab.CURRENT_KIT) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook,
                                    contentDescription = "Study Kit"
                                )
                            }
                        },
                        label = { Text("Study Kit", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_study_kit_tab")
                    )

                    NavigationBarItem(
                        selected = currentNavTab == AppNavTab.SAVED_KITS,
                        onClick = { viewModel.setNavTab(AppNavTab.SAVED_KITS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedKits.isNotEmpty()) {
                                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                            Text("${savedKits.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (currentNavTab == AppNavTab.SAVED_KITS) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Saved Guides"
                                )
                            }
                        },
                        label = { Text("Saved (${savedKits.size})", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_saved_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentNavTab) {
                AppNavTab.CREATE -> {
                    CreateStudyKitScreen(
                        viewModel = viewModel
                    )
                }
                AppNavTab.CURRENT_KIT -> {
                    StudyKitDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.setNavTab(AppNavTab.CREATE) },
                        onStartQuiz = { viewModel.setNavTab(AppNavTab.QUIZ) }
                    )
                }
                AppNavTab.SAVED_KITS -> {
                    SavedKitsScreen(
                        viewModel = viewModel,
                        onCreateNew = { viewModel.setNavTab(AppNavTab.CREATE) }
                    )
                }
                AppNavTab.QUIZ -> {
                    QuizScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.setNavTab(AppNavTab.CURRENT_KIT) }
                    )
                }
            }
        }
    }
}
