package com.myra.assistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.automation.ActionType
import com.myra.assistant.automation.AutomationRecipe
import com.myra.assistant.automation.AutomationRecipeManager
import com.myra.assistant.automation.TriggerType
import com.myra.assistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationRecipesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { AutomationRecipeManager.getInstance(context) }
    var recipes by remember { mutableStateOf(manager.getRecipes()) }
    var history by remember { mutableStateOf(manager.getExecutionHistory()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Automation Recipes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Trigger-Action Routines",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Recipe", tint = CyanNeon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardDark,
                contentColor = PrimaryBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Recipes (${recipes.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        history = manager.getExecutionHistory()
                        selectedTab = 1
                    },
                    text = { Text("History (${history.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                if (recipes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "कोई रेसिपी सक्रिय नहीं है। '+' दबाकर नई रेसिपी बनाएं।",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recipes, key = { it.id }) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onToggle = { isEnabled ->
                                    manager.toggleRecipe(recipe.id, isEnabled)
                                    recipes = manager.getRecipes()
                                },
                                onDelete = {
                                    manager.deleteRecipe(recipe.id)
                                    recipes = manager.getRecipes()
                                }
                            )
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "अभी तक कोई ऑटोमेशन निष्पादित नहीं हुआ।",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history, key = { it.id }) { log ->
                            HistoryCard(log = log)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateRecipeDialog(
                onDismiss = { showCreateDialog = false },
                onSave = { newRecipe ->
                    manager.saveRecipe(newRecipe)
                    recipes = manager.getRecipes()
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun RecipeCard(
    recipe: AutomationRecipe,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = CardDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (recipe.isEnabled) PrimaryBlue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Trigger: ${recipe.triggerType.displayName} ${if (recipe.triggerValue.isNotBlank()) "(${recipe.triggerValue})" else ""}",
                        color = LightBlue,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = recipe.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryBlue,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Action: ${recipe.actionType.displayName} • ${recipe.actionValue}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(log: com.myra.assistant.automation.RecipeExecutionLog) {
    val dateStr = remember(log.timestamp) {
        SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(log.timestamp))
    }

    Surface(
        color = CardDark.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (log.success) AccentEmerald.copy(alpha = 0.3f) else AccentRed.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (log.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (log.success) AccentEmerald else AccentRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.recipeTitle,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateStr,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.message,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CreateRecipeDialog(
    onDismiss: () -> Unit,
    onSave: (AutomationRecipe) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTrigger by remember { mutableStateOf(TriggerType.BATTERY_BELOW) }
    var triggerValue by remember { mutableStateOf("20") }
    var selectedAction by remember { mutableStateOf(ActionType.SPEAK_ALERT) }
    var actionValue by remember { mutableStateOf("बैटरी कम है, कृपया चार्जर लगाएं।") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("नई ऑटोमेशन रेसिपी बनाएं", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Recipe Title") },
                    placeholder = { Text("उदा. 20% Battery Alert") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Trigger Condition:", color = LightBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TriggerType.values().take(3).forEach { t ->
                        FilterChip(
                            selected = selectedTrigger == t,
                            onClick = { selectedTrigger = t },
                            label = { Text(t.displayName.take(15), fontSize = 10.sp) }
                        )
                    }
                }

                if (selectedTrigger == TriggerType.BATTERY_BELOW) {
                    OutlinedTextField(
                        value = triggerValue,
                        onValueChange = { triggerValue = it },
                        label = { Text("Battery Threshold (%)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text("Action to Execute:", color = LightBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionType.values().take(3).forEach { a ->
                        FilterChip(
                            selected = selectedAction == a,
                            onClick = { selectedAction = a },
                            label = { Text(a.displayName.take(12), fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = actionValue,
                    onValueChange = { actionValue = it },
                    label = { Text("Action Value / Message") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newRecipe = AutomationRecipe(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            triggerType = selectedTrigger,
                            triggerValue = triggerValue.trim(),
                            actionType = selectedAction,
                            actionValue = actionValue.trim(),
                            isEnabled = true
                        )
                        onSave(newRecipe)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Recipe", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark
    )
}
