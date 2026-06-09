package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.StartupIdeaEntity
import com.example.viewmodel.HistorySession
import com.example.viewmodel.StartupIdeaViewModel
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(
    viewModel: StartupIdeaViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()
    val selectedIdea by viewModel.selectedIdea.collectAsStateWithLifecycle()

    val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
    val showGreetingGate by viewModel.showGreetingGate.collectAsStateWithLifecycle()

    if (!isOnboarded) {
        OnboardingScreen(
            onComplete = { name, email, dob ->
                viewModel.completeOnboarding(name, email, dob)
            }
        )
    } else if (showGreetingGate) {
        GreetingGateScreen(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissGreetingGate() }
        )
    } else {
        Scaffold(
            topBar = {
                HeaderBar(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onClearAll = { viewModel.clearAllHistory() },
                    onResetProfile = { viewModel.logoutOrResetProfile() }
                )
            },
            bottomBar = {
                if (selectedIdea == null) {
                    BottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.changeTab(it) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Full screen overlay for detailed view if an idea is active
                AnimatedContent(
                    targetState = selectedIdea,
                    transitionSpec = {
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    },
                    label = "DetailTransition"
                ) { activeIdea ->
                    if (activeIdea != null) {
                        IdeaDetailDeck(
                            idea = activeIdea,
                            onBack = { viewModel.selectIdeaDetail(null) },
                            onToggleFavorite = { viewModel.toggleFavorite(activeIdea) }
                        )
                    } else {
                        // Regular Tab Screen Switch Flow
                        when (selectedTab) {
                            0 -> GenerationWorkspaceTab(viewModel)
                            1 -> HistoryTab(viewModel)
                            2 -> FavoritesTab(viewModel)
                            3 -> TrendsTab(viewModel)
                            4 -> FoundersPodcastsTab(viewModel)
                            5 -> HelpChatTab(viewModel)
                        }
                    }
                }

                // Global error Toast notification handler
                LaunchedEffect(generationError) {
                    generationError?.let {
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderBar(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onResetProfile: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Startup Gen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AI POWERED IDEAS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = { onThemeToggle(!isDarkTheme) },
                modifier = Modifier.testTag("theme_switch_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme"
                )
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options"
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reset Profile Info") },
                    onClick = {
                        onResetProfile()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear All Database History") },
                    onClick = {
                        onClearAll()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("bottom_navigation_bar"),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
            label = { Text("Incubate") },
            modifier = Modifier.testTag("tab_generate")
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
            modifier = Modifier.testTag("tab_history")
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
            label = { Text("Favorites") },
            modifier = Modifier.testTag("tab_saved")
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
            label = { Text("Trends") },
            modifier = Modifier.testTag("tab_trends")
        )
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            label = { Text("Podcasts") },
            modifier = Modifier.testTag("tab_stories")
        )
        NavigationBarItem(
            selected = selectedTab == 5,
            onClick = { onTabSelected(5) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
            label = { Text("Volt AI") },
            modifier = Modifier.testTag("tab_chat")
        )
    }
}

@Composable
fun GenerationWorkspaceTab(viewModel: StartupIdeaViewModel) {
    val activeSessionIdeas by viewModel.activeSessionIdeas.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()

    // Retrieve input values
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val interests by viewModel.interests.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val timeAvailable by viewModel.timeAvailable.collectAsStateWithLifecycle()
    val soloOrTeam by viewModel.soloOrTeam.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isGenerating) {
            AdviserLoadingScreen()
        } else if (activeSessionIdeas.isNotEmpty()) {
            // Results screen showing the 5 generated ideas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Generated Concepts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        // Reset session ideas to return to input form
                        viewModel.selectHistorySession("")
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Brainstorm")
                }
            }

            // Horizontal overview chips of search criteria
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text("Skills: $skills") })
                AssistChip(onClick = {}, label = { Text("Interests: $interests") })
                AssistChip(onClick = {}, label = { Text("Budget: $budget") })
                AssistChip(onClick = {}, label = { Text("Time: $timeAvailable") })
            }

            // List of the 5 generated Ideas
            activeSessionIdeas.forEach { idea ->
                StartupIdeaBriefCard(
                    idea = idea,
                    onClick = { viewModel.selectIdeaDetail(idea) },
                    onFavoriteToggle = { viewModel.toggleFavorite(idea) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // Welcome Header & Input Form Stage
            WelcomeFormHeader()

            Spacer(modifier = Modifier.height(12.dp))

            MotivationalReminderCard()

            Spacer(modifier = Modifier.height(18.dp))

            FormInputCard(
                viewModel = viewModel,
                skills = skills,
                interests = interests,
                budget = budget,
                timeAvailable = timeAvailable,
                soloOrTeam = soloOrTeam,
                generationError = generationError,
                onGenerate = { viewModel.generateIdeas() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            DailyAiTrendsSection(viewModel = viewModel)
        }
    }
}

@Composable
fun WelcomeFormHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Incubate Your next Idea",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generate tailored micro-SaaS, tech, or brick-and-mortar business blueprints including full MVP plans, validation checklists, brand names, and Million-Dollar angles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun FormInputCard(
    viewModel: StartupIdeaViewModel,
    skills: String,
    interests: String,
    budget: String,
    timeAvailable: String,
    soloOrTeam: String,
    generationError: String?,
    onGenerate: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Skills section
            Text(
                text = "Your Core Skills",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = skills,
                onValueChange = { viewModel.skills.value = it },
                placeholder = { Text("e.g. Python, Copywriting, Design, Marketing") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("skills_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interests section
            Text(
                text = "Your Passion / Interests",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = interests,
                onValueChange = { viewModel.interests.value = it },
                placeholder = { Text("e.g. Fitness, AI tools, Local Cafes, Healthcare") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interests_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Budget selection - Button Chips Row
            Text(
                text = "Estimated Setup Budget",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val budgets = listOf("₹0–₹5,000", "₹5,000–₹50,000", "₹50,000+")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_selection"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                budgets.forEach { item ->
                    val isSelected = item == budget
                    OutlinedButton(
                        onClick = { viewModel.budget.value = item },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time & Founder team style
            Row(modifier = Modifier.fillMaxWidth()) {
                // Time Available per week
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "Time (Week)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = timeAvailable,
                        onValueChange = { viewModel.timeAvailable.value = it },
                        placeholder = { Text("e.g. 15 hours") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("time_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    )
                }

                // Solo or Team
                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    Text(
                        text = "Partnership",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val structures = listOf("Solo Founder", "Team Setup")
                    var showDropdown by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDropdown = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("solo_team_selection"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = soloOrTeam,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false }
                        ) {
                            structures.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(entry) },
                                    onClick = {
                                        viewModel.soloOrTeam.value = entry
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error message panel
            if (generationError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = generationError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Generate Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    onGenerate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_ideas_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate New Ideas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AdviserLoadingScreen() {
    var loadingTip by remember { mutableStateOf("Stitch-assembling validation hubs...") }
    val tips = listOf(
        "Deconstructing market pain points...",
        "Simulating potential customer validation questions...",
        "Generating catchy alternative brand names & domains...",
        "Formulating 24-hr and 30-day chronological MVP structures...",
        "Mapping scalable growth angles tailored to your skill list...",
        "Drafting teenager-focused low-budget launch strategies..."
    )

    LaunchedEffect(Unit) {
        var index = 0
        while (true) {
            delay(3500)
            index = (index + 1) % tips.size
            loadingTip = tips[index]
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 5.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Incubating Startup Concepts...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            targetState = loadingTip,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "TipTransition"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        ) {
            Text(
                text = "💡 Advice: The best startups focus on a hyper-niche first. Validate before coding. You have enough skills to close your very first customer in 14 days!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartupIdeaBriefCard(
    idea: StartupIdeaEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .testTag("idea_card_${idea.startupName}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = idea.startupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = idea.tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(modifier = Modifier.size(36.dp)) {
                    IconButton(
                        onClick = {
                            onFavoriteToggle()
                            Toast.makeText(
                                context,
                                if (idea.isFavorite) "Removed from saved" else "Saved to Favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("favorite_button_${idea.startupName}")
                    ) {
                        Icon(
                            imageVector = if (idea.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (idea.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = idea.pitch,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DifficultyBadge(difficulty = idea.difficulty)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = idea.estTimeToFirstCustomer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Display Overall Score
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Score",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", idea.scoreOverall),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: StartupIdeaViewModel) {
    val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()

    if (historySessions.isEmpty()) {
        EmptyPlaceholder(
            message = "No Incubation History",
            explanation = "Your previously generated startup ideas will end up here! Enter your skills and let the AI generate customized micro-blueprints."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Brainstorms (${historySessions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(historySessions, key = { it.sessionId }) { session ->
                HistorySessionCard(
                    session = session,
                    onSelect = { viewModel.selectHistorySession(session.sessionId) },
                    onDelete = { viewModel.deleteSession(session.sessionId) }
                )
            }
        }
    }
}

@Composable
fun HistorySessionCard(
    session: HistorySession,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Skills: ${session.skills}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Sector: ${session.interests}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${session.ideasCount} personalized blueprints | Budget: ${session.budget}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun FavoritesTab(viewModel: StartupIdeaViewModel) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()

    if (favorites.isEmpty()) {
        EmptyPlaceholder(
            message = "No Favorited Blueprints",
            explanation = "Your saved ideas will appear here automatically! Review generated ideas on the Ideate tab, click the heart icon on any card, and save it to read later."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Your Saved Concepts (${favorites.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(favorites, key = { it.id }) { idea ->
                StartupIdeaBriefCard(
                    idea = idea,
                    onClick = { viewModel.selectIdeaDetail(idea) },
                    onFavoriteToggle = { viewModel.toggleFavorite(idea) }
                )
            }
        }
    }
}

@Composable
fun IdeaDetailDeck(
    idea: StartupIdeaEntity,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High polish sticky tool-bar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to list"
                )
            }
            Text(
                text = "Decon Blueprint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (idea.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Saved Favorites Star",
                    tint = if (idea.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Idea Intro Deck
            IdeaBriefIntroHeader(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Score Dashboard
            ScoreBoard(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Concept specs (pitch, problem, target market, monetization)
            ConceptProperties(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // BRANDING AND DOMAINS SUGGESTIONS
            BrandingAndDomainsSection(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Million Dollar Angle (THE HIGH ACCLAIM EXCLUSIVITIES)
            MillionDollarAngleBlueprint(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // MVP Roadmap 24h, 7d, 30d
            MvpPlanSection(idea)

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Customer engagement and live check plan
            ValidationPlaybookSection(idea)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun IdeaBriefIntroHeader(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DifficultyBadge(difficulty = idea.difficulty)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.1f Overall", idea.scoreOverall),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = idea.startupName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = idea.tagline,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Setup Parameter: Fits with a budget of ${idea.queryBudget} and weekly load of ${idea.queryTime}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScoreBoard(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Concept Scoring Matrix",
            icon = Icons.Default.Analytics,
            gradient = PinkGradient()
        )
        Spacer(modifier = Modifier.height(8.dp))

        ScoreRow(
            label = "Profit Potential",
            score = idea.scoreProfit,
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF10B981)
        )
        ScoreRow(
            label = "Ease of Building",
            score = idea.scoreEase,
            icon = Icons.Default.Build,
            color = Color(0xFF3B82F6)
        )
        ScoreRow(
            label = "Competition Sparing (inverse saturation)",
            score = idea.scoreCompetition,
            icon = Icons.Default.Shield,
            color = Color(0xFFF59E0B)
        )
        ScoreRow(
            label = "AI Adaptability Moat",
            score = idea.scoreAi,
            icon = Icons.Default.Psychology,
            color = Color(0xFF8B5CF6)
        )
        ScoreRow(
            label = "High Scalability Scale",
            score = idea.scoreScalability,
            icon = Icons.Default.ArrowUpward,
            color = Color(0xFFEC4899)
        )
    }
}

@Composable
fun ConceptProperties(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Incubator Pitch Deck",
            icon = Icons.Default.RocketLaunch,
            gradient = PurpleGradient
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🚀 One-Line Pitch",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.pitch,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "⚠️ Problem Statement",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.problem,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🎯 Ideal Target Customers",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.targetCustomers,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "💰 Revenue Monetization model",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.revenueModel,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "✨ Why it is uniquely scalable",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF06B6D4)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.whyPotential,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun BrandingAndDomainsSection(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Branding & Domains",
            icon = Icons.Default.Launch,
            gradient = TealGradient
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Alternative Brand Names Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Suggested Brands",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    idea.brandingNamesJoined.split("\n").filter { it.isNotBlank() }.mapIndexed { i, brand ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = brand, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Domains Check Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Concise Domain Ideas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    idea.domainsJoined.split("\n").filter { it.isNotBlank() }.forEach { domain ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MillionDollarAngleBlueprint(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Million Dollar Angle",
            icon = Icons.Default.CurrencyExchange,
            gradient = AmberGradient
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Moat
                AngleBulletPoint(
                    title = "The Edge Moat (Unfair Difference)",
                    description = idea.angleDifference,
                    color = Color(0xFFF59E0B)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // AI upgrade
                AngleBulletPoint(
                    title = "AI Scaling Injection",
                    description = idea.angleAiImprovement,
                    color = Color(0xFF8B5CF6)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Teenager setup
                AngleBulletPoint(
                    title = "Lean Garage Strategy (Start As A Teeneger)",
                    description = idea.angleTeenagerStart,
                    color = Color(0xFF06B6D4)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Customer Acquisition Strategy
                AngleBulletPoint(
                    title = "User #1 paying tactic",
                    description = idea.anglePayingCustomerStrat,
                    color = Color(0xFF10B981)
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Risks & Pitfalls
                AngleBulletPoint(
                    title = "Risks & Blind spots (with Mitigations)",
                    description = idea.angleRisks,
                    color = MaterialTheme.colorScheme.error
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Timeline
                Text(
                    text = "📅 30-Day Launch Roadmap",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = idea.angleLaunchRoadmap30d,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AngleBulletPoint(title: String, description: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MvpPlanSection(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Minimum Viable Product (MVP)",
            icon = Icons.Default.Build,
            gradient = TealGradient
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = idea.mvpDescription,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Core Blueprint Features:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                idea.mvpCoreFeaturesJoined.split("\n").filter { it.isNotBlank() }.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = feature, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time blocks timeline
        TimelineNode(
            stepNumber = "1",
            duration = "24 Hours Launch",
            title = "The 24h Build Setup",
            description = idea.mvp24h
        )
        TimelineNode(
            stepNumber = "2",
            duration = "7 Days Launch",
            title = "The 7d Interactive Loop",
            description = idea.mvp7d
        )
        TimelineNode(
            stepNumber = "3",
            duration = "30 Days Scale",
            title = "The 30d Core Engine",
            description = idea.mvp30d
        )
    }
}

@Composable
fun ValidationPlaybookSection(idea: StartupIdeaEntity) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Customer Validation Playbook",
            icon = Icons.Default.CheckCircle,
            gradient = PurpleGradient
        )

        // Customer locations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "📍 5 places to find potential customer lists:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                idea.validationPlacesJoined.split("\n").filter { it.isNotBlank() }.forEach { place ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(text = "•", modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Bold)
                        Text(text = place, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Validation Questions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "❓ 5 high-signal Validation Questions:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                idea.validationQuestionsJoined.split("\n").filter { it.isNotBlank() }.forEachIndexed { i, q ->
                    ValidationQuestionRow(index = i + 1, question = q)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Direct microaction checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "⚡ FIRST ACTION TO TAKE TODAY",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = idea.firstAction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun PinkGradient() = Brush.linearGradient(
    colors = listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
)

@Composable
fun MotivationalReminderCard() {
    val context = LocalContext.current
    val quotes = listOf(
        "\"The best way to predict the future is to create it.\" — Peter Drucker",
        "\"Your execution speed is your ultimate moat.\" – Don't wait for a perfect product; find your first paying customer in 14 days.",
        "\"Fall in love with the problem, not your solution.\" – Look for real business pain points that folks will pay ₹1,000/month to solve.",
        "\"Stale plans are useless; active validation is everything.\" – Do things that don't scale first. Speak to 10 potential users today.",
        "\"Don't think about building a giant enterprise. Think about building a cash-flowing micro-niche asset.\"",
        "\"If you are not embarrassed by the first version of your product, you've launched too late.\" – Reid Hoffman",
        "\"99% of startup success is showing up, adapting quickly, and refusing to quit. Tap 'Generate' below to begin!\""
    )

    var currentQuoteIndex by remember { mutableStateOf((0 until quotes.size).random()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable {
                currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
            }
            .testTag("motivational_reminder_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY FOUNDER SPARK ✨",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                AnimatedContent(
                    targetState = quotes[currentQuoteIndex],
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "QuoteTransition"
                ) { quoteText ->
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap block to cycle motivation ♻️",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DailyAiTrendsSection(viewModel: StartupIdeaViewModel) {
    val context = LocalContext.current
    val trends = listOf(
        AiTrend(
            title = "Retail AI Voice Agents",
            bubble = "+340% YoY",
            interestSeed = "AI Voice Agents for local retail shops",
            icon = Icons.Default.Phone,
            tagline = "Voice calling assistants closing orders for businesses",
            color = Color(0xFF10B981)
        ),
        AiTrend(
            title = "Supply Carbon ESG Auditing",
            bubble = "+185% MoM",
            interestSeed = "Carbon receipt auditing for tier-1 supply-chains",
            icon = Icons.Default.Description,
            tagline = "Automatic invoice checking for corporate ESG logs",
            color = Color(0xFFEC4899)
        ),
        AiTrend(
            title = "Whatsapp Micro-SaaS Store",
            bubble = "+210% YoY",
            interestSeed = "Whatsapp catalog store builders for grocery shops",
            icon = Icons.Default.Sms,
            tagline = "Nocode storefronts launching in WhatsApp chats",
            color = Color(0xFFF59E0B)
        ),
        AiTrend(
            title = "Smart Medical Scribe API",
            bubble = "+150% YoY",
            interestSeed = "AI smart medical scribble for small GP clinics",
            icon = Icons.Default.Healing,
            tagline = "Voice-to-clinical logs generator for local practitioners",
            color = Color(0xFF06B6D4)
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .testTag("ai_trends_container"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Daily AI Trends Feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "REALTIME MARKET SIGNALS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Live indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Tap a micro-SaaS trend below to load it into your interests input automatically and brainstorm faster!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            trends.forEach { trend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.interests.value = trend.interestSeed
                            viewModel.changeTab(0)
                            Toast.makeText(
                                context,
                                "Imported Trend: '${trend.title}'",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .testTag("trend_item_${trend.title}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(trend.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = trend.icon,
                                contentDescription = null,
                                tint = trend.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = trend.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(trend.color.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = trend.bubble,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = trend.color,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = trend.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class AiTrend(
    val title: String,
    val bubble: String,
    val interestSeed: String,
    val icon: ImageVector,
    val tagline: String,
    val color: Color
)

@Composable
fun TrendsTab(viewModel: StartupIdeaViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcoming card specifically explaining how trends work for beginners
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("trends_guide_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Guide",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Beginner's Cheat Sheet 🚦",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Welcome, Future Founder! Spotting real trends early is your superpower. Tap any hot AI Trend below and we will automatically load it into your inputs and switch tabs for you to generate a tailored step-by-step business blueprint instantly!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reuse the beautiful DailyAiTrendsSection layout inside the dedicated tab
        DailyAiTrendsSection(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Beginner FAQs to make the app incredibly easy to use and digest on the go
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .testTag("beginner_faq_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "How to use this app on mobile?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A SEAMLESS 3-STEP FLOW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Step 1
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "List Skills & Interests",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Input your comfort zone (e.g., Python, marketing, tutoring) or click a trend bubble to get pre-filled instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step 2
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Generate Micro-Blueprints",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Our Gemini engine analyzes viability, startup budgets, difficulty curves, and creates 5 viable startup ideas matching you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step 3
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Validate and Launch",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Read the exact target audience, marketing guide, and execution matrix. Star your favorites to keep them offline!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class FounderPodcast(
    val title: String,
    val showName: String,
    val guestName: String,
    val duration: String,
    val platform: String,
    val summary: String,
    val lessons: List<String>,
    val prefillSkills: String,
    val prefillInterests: String,
    val url: String,
    val color: Color
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FoundersPodcastsTab(viewModel: StartupIdeaViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    val podcasts = listOf(
        FounderPodcast(
            title = "NPR How I Built This: Spanx by Sara Blakely",
            showName = "How I Built This",
            guestName = "Sara Blakely",
            duration = "55 mins",
            platform = "NPR Podcast",
            summary = "How Sara Blakely went from selling fax machines door-to-door to building a shapewear empire with only $5,000 started capital, keeping 100% of her equity.",
            lessons = listOf(
                "Bootstrap with personal grit and keep equity close.",
                "Research has zero cost; write your own patent before spending thousands.",
                "Direct cold-calling can close the first critical sales deals."
            ),
            prefillSkills = "Door-to-door sales, cold calling, bootstrapped product patenting",
            prefillInterests = "Apparel retail utility, direct-to-consumer distribution",
            url = "https://www.youtube.com/watch?v=0bTox_5nCto",
            color = Color(0xFFEC4899)
        ),
        FounderPodcast(
            title = "YC Startup School: WhatsApp's Jan Koum on Extreme Simplicity",
            showName = "Y Combinator Podcast",
            guestName = "Jan Koum",
            duration = "42 mins",
            platform = "YouTube Podcast",
            summary = "Immigrating from Ukraine and living on food stamps to founding a utility messaging tool that served 450 Million users with less than 50 employees.",
            lessons = listOf(
                "Focus purely on zero-ads, high-reliability message status lists.",
                "Remove all decorative features to maintain a super-fast app.",
                "Operate with extreme administrative cost discipline."
            ),
            prefillSkills = "System admin, Erlang backend scaling, FreeBSD networking, security protocols",
            prefillInterests = "Instant communication utilities, minimal real-time networks",
            url = "https://www.youtube.com/watch?v=mD_WfUvvaX0",
            color = Color(0xFF10B981)
        ),
        FounderPodcast(
            title = "The Diary of a CEO: Canva's Melanie Perkins on Grit",
            showName = "The Diary of a CEO",
            guestName = "Melanie Perkins",
            duration = "65 mins",
            platform = "Spotify & YouTube",
            summary = "How Melanie Perkins tutored complex design tools, envisioned a simpler online template playground, and persisted through over 100 VC investor rejections.",
            lessons = listOf(
                "Secure a small profitable niche to prove demand first.",
                "Focus on speed: users want a gorgeous template draft in three clicks.",
                "Treat investor rejection as a path for system refinement."
            ),
            prefillSkills = "Design education, template styling, pitch layouts, SaaS model structures",
            prefillInterests = "Drag-and-drop graphic suites, online visual collaborations",
            url = "https://www.youtube.com/watch?v=3z_GvVfQZRE",
            color = Color(0xFF8B5CF6)
        ),
        FounderPodcast(
            title = "Masters of Scale: Designing & Growing Airbnb from Nothing",
            showName = "Masters of Scale",
            guestName = "Brian Chesky",
            duration = "50 mins",
            platform = "NPR / YouTube",
            summary = "Renting air mattresses on standard floorboards to global hotel alternative. Explains the rule of 'doing things that don't scale' by visiting hosts directly.",
            lessons = listOf(
                "Do things that don't scale—visit your users in person.",
                "Spend time making early users absolutely love the product.",
                "Co-founder trust and visual presentation are paramount."
            ),
            prefillSkills = "Design presentation, visual photography, community engagement",
            prefillInterests = "Short-term lodging platforms, local activity tours, peer booking",
            url = "https://www.youtube.com/watch?v=W608u6sBF70",
            color = Color(0xFFFF5A5F)
        ),
        FounderPodcast(
            title = "How I Built This: Pinterest's Ben Silbermann on Curation",
            showName = "How I Built This",
            guestName = "Ben Silbermann",
            duration = "48 mins",
            platform = "NPR Podcast",
            summary = "From collecting stamps to creating a massive visual search platform. Ben Silbermann explains how email outreach with early users built a high-retention core community.",
            lessons = listOf(
                "Personally email and support your first 3,000 advocates.",
                "Leverage existing natural behaviors like visual collecting.",
                "Worry more about core retention than early viral growth."
            ),
            prefillSkills = "Visual card structures, email outreach, user retention loops",
            prefillInterests = "Visual board discovery channels, lifestyle hobby lists",
            url = "https://www.youtube.com/watch?v=Xh0l05XyW6k",
            color = Color(0xFFE60023)
        )
    )

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tech", "Bootstrap")

    val filteredPodcasts = remember(selectedCategory) {
        if (selectedCategory == "All") {
            podcasts
        } else {
            podcasts.filter { podcast ->
                podcast.title.contains(selectedCategory, ignoreCase = true) ||
                podcast.platform.contains(selectedCategory, ignoreCase = true) ||
                podcast.summary.contains(selectedCategory, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Podcast Banner Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .testTag("podcasts_header_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Founders Podcasts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "WATCH & LISTEN TO STRUGGLE TO TRIUMPH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Gain deep insights by watching actual podcast interviews. Read the core takeaways, and adopt their exact background skill stack directly to incubate your own startup ideas!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Podcasts Cards
        filteredPodcasts.forEach { podcast ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isExpanded = !isExpanded }
                    .testTag("podcast_card_${podcast.guestName}"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row: Show Name / Guest Name / Platform Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = podcast.showName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = podcast.color
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = podcast.guestName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(podcast.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = podcast.platform,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = podcast.color
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏱️ ${podcast.duration}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title
                    Text(
                        text = podcast.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brief description
                    Text(
                        text = podcast.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )

                    // Expandible section
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "KEY PODCAST TAKEAWAYS",
                                style = MaterialTheme.typography.labelSmall,
                                color = podcast.color,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            podcast.lessons.forEach { lesson ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "🎧",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = lesson,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { uriHandler.openUri(podcast.url) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Watch Podcast",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.skills.value = podcast.prefillSkills
                                        viewModel.interests.value = podcast.prefillInterests
                                        viewModel.changeTab(0)
                                        Toast.makeText(
                                            context,
                                            "Adopted ${podcast.guestName}'s stack!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = podcast.color),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Adopt Stack",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Chevron trigger details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Show Less" else "Show More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Tap to collapse" else "Tap to look inside key takeaways",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var dobError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to Startup Gen! 🚀",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Let's personalize your incubation space. Please enter your details below so we can custom-tailor your ideation experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Full Name") },
                    placeholder = { Text("e.g. Sara Blakely") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (nameError) {
                    Text(
                        text = "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = false
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("name@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    isError = emailError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (emailError) {
                    Text(
                        text = "Please enter a valid email address",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = dob,
                    onValueChange = {
                        dob = it
                        dobError = false
                    },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    isError = dobError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_dob_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                if (dobError) {
                    Text(
                        text = "Please enter your date of birth",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val isNameInvalid = name.isBlank()
                        val isEmailInvalid = email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                        val isDobInvalid = dob.isBlank()

                        nameError = isNameInvalid
                        emailError = isEmailInvalid
                        dobError = isDobInvalid

                        if (!isNameInvalid && !isEmailInvalid && !isDobInvalid) {
                            onComplete(name.trim(), email.trim(), dob.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("onboarding_submit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Activate Startup Space 🚀",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GreetingGateScreen(
    viewModel: StartupIdeaViewModel,
    onDismiss: () -> Unit
) {
    val name by viewModel.userName.collectAsStateWithLifecycle()
    val email by viewModel.userEmail.collectAsStateWithLifecycle()
    val dob by viewModel.userDob.collectAsStateWithLifecycle()

    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 0..11 -> "Good Morning 🌅"
        hour in 12..16 -> "Good Afternoon ☀️"
        else -> "Good Evening 🌙"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeContent)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to the ultimate launchpad of your startup journey. 🚀 Ready to spark brilliance, design fresh micro-SaaS concepts, and build what's next?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
                lineHeight = 22.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "YOUR INCUBATION PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Email Link", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(email, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Founder DOB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dob, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("greeting_get_started_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Let's Get Started 🚀",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = { viewModel.logoutOrResetProfile() },
                modifier = Modifier.testTag("greeting_reset_profile_button")
            ) {
                Text(
                    text = "Not you? Tap to Reset Profile info",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun formatVoltText(text: String): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    val parts = text.split("**")
    for (i in parts.indices) {
        if (i % 2 == 1) {
            builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
            builder.append(parts[i])
            builder.pop()
        } else {
            builder.append(parts[i])
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun HelpChatTab(viewModel: com.example.viewmodel.StartupIdeaViewModel) {
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Smooth-scroll to bottom whenever messages list grows or is loading
    LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic Top Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("chat_header_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Volt AI Venture Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ACTIVE IN-APP DIGITAL CO-FOUNDER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Brainstorm tech stacks, critique wild ideas, outline quick MVP feature roadmaps, or explore marketing plans. Volt provides direct guidance based on industry-leading strategies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Messages Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(chatMessages) { message ->
                    val isAi = message.sender == com.example.viewmodel.MessageSender.AI
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAi) Alignment.Start else Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End,
                            modifier = Modifier.fillMaxWidth(0.85f).align(if (isAi) Alignment.Start else Alignment.End)
                        ) {
                            if (isAi) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp, end = 8.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⚡",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isAi) 4.dp else 16.dp,
                                    bottomEnd = if (isAi) 16.dp else 4.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAi) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                ),
                                border = if (isAi) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = formatVoltText(message.text),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isAi) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        },
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (isChatLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Volt is formulating a sharp answer...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Suggestions Horizontal Row
        val suggestionChips = listOf(
            "🧐 Critique my business idea",
            "💻 Build a 24h MVP Tech Stack",
            "📈 10 ways to get first customers",
            "🚀 Pitch angle for bootstrap"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestionChips.forEach { prompt ->
                AssistChip(
                    onClick = {
                        viewModel.sendChatMessage(prompt)
                    },
                    label = { 
                        Text(
                            text = prompt,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // TextInput Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { 
                    Text(
                        text = "Ask Volt anything...", 
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                trailingIcon = {
                    if (textInput.isNotEmpty()) {
                        IconButton(
                            onClick = { textInput = "" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = {
                    val message = textInput.trim()
                    if (message.isNotEmpty() && !isChatLoading) {
                        viewModel.sendChatMessage(message)
                        textInput = ""
                        keyboardController?.hide()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
