package com.kojinguide.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kojinguide.app.data.GuideRepository

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab("home", "ガイド", Icons.AutoMirrored.Filled.MenuBook),
    BottomTab("checklist", "チェックリスト", Icons.Default.Checklist),
    BottomTab("glossary", "用語集", Icons.Default.Translate)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KojinGuideApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentCategoryId = backStackEntry?.arguments?.getString("categoryId")
    val detailCategory = currentCategoryId?.let { GuideRepository.findCategory(it) }

    val title = when {
        detailCategory != null -> detailCategory.title
        currentDestination?.route == "checklist" -> "開業チェックリスト"
        currentDestination?.route == "glossary" -> "用語集"
        else -> "個人事業主の手引き"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (detailCategory != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route == tab.route } == true ||
                        (tab.route == "home" && detailCategory != null)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    onCategoryClick = { category ->
                        navController.navigate("guide/${category.id}")
                    },
                    contentPadding = innerPadding
                )
            }
            composable("guide/{categoryId}") { entry ->
                val categoryId = entry.arguments?.getString("categoryId")
                val category = categoryId?.let { GuideRepository.findCategory(it) }
                if (category != null) {
                    GuideDetailScreen(category = category, contentPadding = innerPadding)
                }
            }
            composable("checklist") {
                ChecklistScreen(contentPadding = innerPadding)
            }
            composable("glossary") {
                GlossaryScreen(contentPadding = innerPadding)
            }
        }
    }
}
