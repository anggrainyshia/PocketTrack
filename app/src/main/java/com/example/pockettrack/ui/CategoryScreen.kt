package com.example.pockettrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pockettrack.data.Category
import com.example.pockettrack.viewmodel.AppViewModel

// ─── CategoryScreen ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(vm: AppViewModel) {
    val categories  = vm.allCategories.observeAsState(emptyList()).value
    val topCats     = categories.filter { it.parentId == 0 }
    var filterType  by remember { mutableStateOf("Expense") }
    var showAddDialog   by remember { mutableStateOf(false) }
    var editTarget      by remember { mutableStateOf<Category?>(null) }
    var deleteTarget    by remember { mutableStateOf<Category?>(null) }
    var addSubParent    by remember { mutableStateOf<Category?>(null) } // triggers add sub-category

    // ── Delete confirmation ──
    deleteTarget?.let { cat ->
        val hasSubs = categories.any { it.parentId == cat.id }
        ConfirmDialog(
            title = "Delete Category",
            message = {
                Text("Delete \"${cat.icon}  ${cat.name}\"?")
                Spacer(Modifier.height(4.dp))
                if (hasSubs) {
                    Text(
                        "⚠️ This will also delete all sub-categories.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "Existing transactions won't be deleted.",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            isDestructive = true,
            onConfirm = {
                // Delete sub-categories first
                categories.filter { it.parentId == cat.id }.forEach { vm.deleteCategory(it) }
                vm.deleteCategory(cat)
            },
            onDismiss = { deleteTarget = null }
        )
    }

    // ── Add / Edit dialog ──
    if (showAddDialog || editTarget != null || addSubParent != null) {
        CategoryDialog(
            existing = editTarget,
            parentCategory = addSubParent,
            allTopCategories = topCats,
            onSave = { cat ->
                if (editTarget != null) vm.updateCategory(cat)
                else vm.addCategory(cat)
                showAddDialog = false
                editTarget = null
                addSubParent = null
            },
            onDismiss = {
                showAddDialog = false
                editTarget = null
                addSubParent = null
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Categories") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Category")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))

            // Filter: Expense / Income
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Expense", "Income").forEach { t ->
                    FilterChip(
                        selected = filterType == t,
                        onClick = { filterType = t },
                        label = { Text(t) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val filtered = topCats.filter { it.type == filterType }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No $filterType categories yet.\nTap + to add one.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { cat ->
                        val subs = categories.filter { it.parentId == cat.id }
                        CategoryCard(
                            category = cat,
                            subCategories = subs,
                            onEdit = { editTarget = it },
                            onDelete = { deleteTarget = it },
                            onAddSub = { addSubParent = cat }
                        )
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }
}


// ─── CategoryCard ────────────────────────────────────────────
@Composable
fun CategoryCard(
    category: Category,
    subCategories: List<Category>,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onAddSub: () -> Unit
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.15f)
    } catch (e: Exception) { Color.Gray.copy(0.1f) }

    val iconColor = try {
        Color(android.graphics.Color.parseColor(category.color))
    } catch (e: Exception) { Color.Gray }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {

            // ── Parent row ──
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(bgColor, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(category.icon, fontSize = 22.sp) }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(category.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "${subCategories.size} sub-categor${if (subCategories.size == 1) "y" else "ies"}",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onAddSub, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        "Add sub-category",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onEdit(category) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(category) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }

            // ── Sub-categories ──
            if (subCategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(Modifier.height(6.dp))

                subCategories.forEach { sub ->
                    val subBg = try {
                        Color(android.graphics.Color.parseColor(sub.color)).copy(0.12f)
                    } catch (e: Exception) { Color.Gray.copy(0.1f) }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("↳", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(subBg, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(sub.icon, fontSize = 14.sp) }
                        Spacer(Modifier.width(8.dp))
                        Text(sub.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onEdit(sub) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, "Edit sub",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(sub) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete, "Delete sub",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ─── CategoryDialog (Add & Edit) ────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    existing: Category? = null,
    parentCategory: Category? = null,    // non-null = adding a sub-category
    allTopCategories: List<Category> = emptyList(),
    onSave: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing   = existing != null
    val isSub       = (existing?.parentId ?: 0) != 0 || parentCategory != null
    val parentId    = parentCategory?.id ?: existing?.parentId ?: 0

    var name    by remember { mutableStateOf(existing?.name ?: "") }
    var icon    by remember { mutableStateOf(existing?.icon ?: "📦") }
    var color   by remember { mutableStateOf(existing?.color ?: "#9CA3AF") }
    var type    by remember { mutableStateOf(existing?.type ?: parentCategory?.type ?: "Expense") }
    var nameError by remember { mutableStateOf(false) }

    val presetIcons = listOf(
        "🍔","🍕","☕","🍜","🚗","🚌","✈️","🛍️","👕","💄",
        "💡","📱","🏠","💊","🏥","🎮","🎵","🎬","📚","🎓",
        "💼","💻","📈","🎁","💰","🏋️","⚽","🐾","🌿","📦"
    )
    val presetColors = listOf(
        "#FF6B6B","#FF8C42","#F59E0B","#84CC16","#10B981",
        "#0EA5E9","#3B82F6","#6366F1","#A855F7","#EC4899",
        "#F472B6","#4ECDC4","#059669","#EF4444","#9CA3AF"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isEditing && isSub -> "Edit Sub-Category"
                    isEditing          -> "Edit Category"
                    isSub              -> "Add Sub-Category"
                    else               -> "New Category"
                }
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Category Name") },
                    isError = nameError,
                    supportingText = { if (nameError) Text("Name cannot be empty") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Type — only show for top-level categories
                if (!isSub) {
                    Text("Type", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Expense", "Income").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t) }
                            )
                        }
                    }
                }

                if (isSub && parentCategory != null) {
                    Text(
                        "Parent: ${parentCategory.icon} ${parentCategory.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Icon picker
                Text("Icon", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetIcons.forEach { e ->
                        FilterChip(
                            selected = icon == e,
                            onClick = { icon = e },
                            label = { Text(e, fontSize = 18.sp) },
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Color picker
                Text("Color", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetColors.forEach { c ->
                        val parsed = try {
                            Color(android.graphics.Color.parseColor(c))
                        } catch (e: Exception) { Color.Gray }
                        val isSelected = color == c
                        Box(
                            Modifier
                                .size(32.dp)
                                .background(parsed, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            FilterChip(
                                selected = isSelected,
                                onClick = { color = c },
                                label = {},
                                modifier = Modifier.size(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = parsed,
                                    selectedContainerColor = parsed
                                )
                            )
                            if (isSelected) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Preview
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            try { Color(android.graphics.Color.parseColor(color)).copy(0.12f) }
                            catch (e: Exception) { Color.Gray.copy(0.1f) },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        name.ifBlank { "Preview" },
                        fontWeight = FontWeight.Medium,
                        color = try { Color(android.graphics.Color.parseColor(color)) }
                        catch (e: Exception) { Color.Gray }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                onSave(
                    Category(
                        id       = existing?.id ?: 0,
                        name     = name.trim(),
                        icon     = icon,
                        color    = color,
                        type     = type,
                        parentId = parentId
                    )
                )
            }) { Text(if (isEditing) "Update" else "Add") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
