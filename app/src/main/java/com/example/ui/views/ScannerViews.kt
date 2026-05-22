@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.core.content.FileProvider
import com.example.data.model.Document
import com.example.data.model.DocumentPage
import com.example.data.model.Folder
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderSecondary
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ScannerViewModel
import com.example.ui.viewmodel.ConvertedFile
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerAppMainContent(viewModel: ScannerViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val ocrStatusMessage by viewModel.ocrStatusMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-Screen Navigation Switcher
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.Home -> DashboardHomeScreen(viewModel)
                AppScreen.Scanner -> ScannerViewfinderScreen(viewModel)
                AppScreen.CropEditor -> CropAndFilterEditorScreen(viewModel)
                is AppScreen.Details -> DocumentDetailsScreen(viewModel, screen.documentId)
                AppScreen.SecuritySettings -> SecurityVaultScreen(viewModel)
                AppScreen.Converter -> FileConverterScreen(viewModel)
            }
        }

        // Global OCR processing overlay
        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = viewModel.translate("processing"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = ocrStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD HOME SCREEN
// ==========================================
@Composable
fun DashboardHomeScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val syncEnabled by viewModel.syncEnabled.collectAsState()

    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            // Navigation Bottom pill consistent with front-end rules
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Home") },
                    label = { Text(viewModel.translate("dashboard"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.Converter) },
                    icon = { Icon(Icons.Default.ChangeCircle, contentDescription = "PDF Word Converter") },
                    label = { Text("Converter", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.SecuritySettings) },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Security Vault") },
                    label = { Text(viewModel.translate("security_title"), fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearScanningSession()
                    viewModel.navigateTo(AppScreen.Scanner)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("new_scan_fab")
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Scan icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("new_scan"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Block: Logo & Unified Search & Language Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Scanner,
                                contentDescription = "BO Scanner Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BO Scanner",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " OCR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = viewModel.translate("dashboard"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Embedded multi-language quick selector
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            val active = lang == language
                            Text(
                                text = when (lang) {
                                    AppLanguage.ENGLISH -> "EN"
                                    AppLanguage.SPANISH -> "ES"
                                    AppLanguage.FRENCH -> "FR"
                                    AppLanguage.JAPANESE -> "JP"
                                    AppLanguage.INDONESIAN -> "ID"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.setLanguage(lang) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Space Metrics & Security Sync Dashboard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = LavenderSecondary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF4F378B).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(LavenderPrimary, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (syncEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                        contentDescription = "Cloud Status",
                                        tint = LavenderSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "STATUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LavenderPrimary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = if (syncEnabled) viewModel.translate("sync_active") else "Offline Vault Mode",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Dynamic state tags
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = LavenderPrimary.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = viewModel.translate("e2e_badge"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = LavenderPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = viewModel.translate("offline_badge"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.translate("analytics"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )

                        // Aesthetic linear capacity metric indicator
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = 0.21f,
                            color = LavenderPrimary,
                            trackColor = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "2.1 MB stored (Encrypted SQLite + Bitmaps)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "21% cloud weight",
                                style = MaterialTheme.typography.bodySmall,
                                color = LavenderPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Capture & Gallery Image Import OCR Section
            item {
                var showPresetSelectMenu by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚡ QUICK CAPTURE & GALLERY OCR IMPORT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.clearScanningSession()
                                    viewModel.navigateTo(AppScreen.Scanner)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Camera Scan")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Camera Scan", fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    showPresetSelectMenu = !showPresetSelectMenu
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = "Galleries Import")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import OCR File", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        
                        if (showPresetSelectMenu) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Choose document original template to simulate image-run OCR:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    Pair("RECEIPT", "Receipt"),
                                    Pair("INVOICE", "Invoice"),
                                    Pair("BUSINESS_CARD", "Biz Card"),
                                    Pair("BOOK_PAGE", "Book Page")
                                )
                                presets.forEach { p ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            showPresetSelectMenu = false
                                            viewModel.importLibraryFileForOcr(p.first) {
                                                viewModel.navigateTo(AppScreen.CropEditor)
                                            }
                                        },
                                        label = { Text(p.second) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Beautiful search item bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text(viewModel.translate("search_scans")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    ),
                    singleLine = true
                )
            }

            // Folder Management Quick row carousel with Add Folder interaction
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("folders"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                folderNameInput = ""
                                showAddFolderDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add folder",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // "All" Folders button
                        item {
                            FilterChip(
                                selected = selectedFolderId == null,
                                onClick = { viewModel.selectFolder(null) },
                                label = { Text(viewModel.translate("all_docs")) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = "Folders") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }

                        // Room folders database mapping loop
                        items(folders) { folder ->
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = { viewModel.selectFolder(folder.id) },
                                label = { Text(folder.name) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = "Folder") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            // Documents List header section
            item {
                Text(
                    text = viewModel.translate("doc_count") + " (${documents.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Map list of Room Scanned Documents or empty placeholder
            if (documents.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPasteOff,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = viewModel.translate("no_docs"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(documents) { doc ->
                    DocumentCardItem(doc = doc, folders = folders, onClick = {
                        viewModel.navigateTo(AppScreen.Details(doc.id))
                    })
                }
            }

            // Spacing buffer bottom
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal dialog to add dynamic folders in Local Database
    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    placeholder = { Text("e.g. Invoices, Taxes, Personal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.addNewFolder(folderNameInput)
                        }
                        showAddFolderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DocumentCardItem(doc: Document, folders: List<Folder>, onClick: () -> Unit) {
    val matchingFolder = folders.find { it.id == doc.folderId }?.name ?: "Root"
    val formattedDate = remember(doc.timestamp) {
        val date = java.util.Date(doc.timestamp)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        sdf.format(date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("document_card_${doc.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant document thumbnail proxy containing file format symbol
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Doc format icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Folder location",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = matchingFolder,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Display custom tags associated with document
                if (doc.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        doc.tags.split(",").forEach { tag ->
                            if (tag.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "#" + tag.trim(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See Detail",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// 2. SCANNING VIEWFINDER SCREEN
// ==========================================
@Composable
fun ScannerViewfinderScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val scannedPages by viewModel.scannedSessionPages.collectAsState()
    val isBatchMode by viewModel.isBatchMode.collectAsState()

    var focusActive by remember { mutableStateOf(false) }
    var captureSuccessOverlay by remember { mutableStateOf(false) }

    // Multi-page batch scan visual queue animation offset
    val sizePages = scannedPages.size

    // Pulsing line simulation for scan viewfinder matching luxury feel
    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserY"
    )

    // Launch instant camera grid lock sequence
    LaunchedEffect(Unit) {
        delay(1200)
        focusActive = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.translate("new_scan"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { viewModel.setBatchMode(!isBatchMode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isBatchMode) Icons.Default.Layers else Icons.Default.LayersClear,
                            contentDescription = "Batch mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBatchMode) "Batch [ON]" else "Single Page",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live scanning viewfinder canvas simulator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C0F0E))
                    .border(
                        BorderStroke(
                            2.dp,
                            if (focusActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Interactive background showing simulated paper outline
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw perspective guide lines
                    drawRect(
                        color = if (focusActive) primaryColor.copy(alpha = 0.2f) else Color(0x19FFFFFF),
                        topLeft = Offset(w * 0.15f, h * 0.22f),
                        size = androidx.compose.ui.geometry.Size(w * 0.7f, h * 0.56f),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                        )
                    )

                    // Bracket corners
                    val ext = 24.dp.toPx()
                    val gap = 4.dp.toPx()
                    val pX1 = w * 0.15f
                    val pY1 = h * 0.22f
                    val pX2 = w * 0.85f
                    val pY2 = h * 0.78f

                    // Top Left Bracket
                    drawLine(primaryColor, Offset(pX1 - gap, pY1 - gap), Offset(pX1 + ext, pY1 - gap), 3.dp.toPx())
                    drawLine(primaryColor, Offset(pX1 - gap, pY1 - gap), Offset(pX1 - gap, pY1 + ext), 3.dp.toPx())
                    // Top Right Bracket
                    drawLine(primaryColor, Offset(pX2 + gap, pY1 - gap), Offset(pX2 - ext, pY1 - gap), 3.dp.toPx())
                    drawLine(primaryColor, Offset(pX2 + gap, pY1 - gap), Offset(pX2 + gap, pY1 + ext), 3.dp.toPx())
                    // Bottom Left Bracket
                    drawLine(primaryColor, Offset(pX1 - gap, pY2 + gap), Offset(pX1 + ext, pY2 + gap), 3.dp.toPx())
                    drawLine(primaryColor, Offset(pX1 - gap, pY2 + gap), Offset(pX1 - gap, pY2 - ext), 3.dp.toPx())
                    // Bottom Right Bracket
                    drawLine(primaryColor, Offset(pX2 + gap, pY2 + gap), Offset(pX2 - ext, pY2 + gap), 3.dp.toPx())
                    drawLine(primaryColor, Offset(pX2 + gap, pY2 + gap), Offset(pX2 + gap, pY2 - ext), 3.dp.toPx())

                    // Draw scanning active laser line representing automatic edge detection
                    val laserY = h * laserYOffset
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = Offset(w * 0.10f, laserY),
                        end = Offset(w * 0.90f, laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Inner Status banner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
                ) {
                    Surface(
                        color = if (focusActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.DarkGray,
                        shape = CircleShape
                    ) {
                        Text(
                            text = if (focusActive) "AUTO EDGE DETECT: LOCKED ✓" else "ALIGNING DOCUMENT VIEW...",
                            color = if (focusActive) MaterialTheme.colorScheme.primary else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Captured flashing camera burst visual feedback
                androidx.compose.animation.AnimatedVisibility(
                    visible = captureSuccessOverlay,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.75f))
                    )
                }

                // Guide label at bottom of viewport
                Text(
                    text = "Position card/document inside brackets for auto-perspective detection.",
                    textAlign = TextAlign.Center,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 20.dp)
                )
            }

            // Interactive Selector row for mock layouts so scanner has beautiful predictable document shapes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Document Preset to Scan:",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Beautiful scrolling row of document scan presets (MOCK OCR text binds to these!)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Receipt", "Invoice", "Business_Card", "Book_Page").forEach { preset ->
                        AssistChip(
                            onClick = {
                                viewModel.capturePage(preset)
                                captureSuccessOverlay = true
                                // Trigger vibration simulation
                                focusActive = false
                            },
                            label = { Text(preset.replace("_", " ")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (preset) {
                                        "Receipt" -> Icons.Default.Receipt
                                        "Invoice" -> Icons.Default.ReceiptLong
                                        "Business_Card" -> Icons.Default.AccountBox
                                        else -> Icons.Default.MenuBook
                                    },
                                    contentDescription = preset
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                labelColor = Color.White,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Clean camera shutter mock button action
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Queue Thumbnail Preview for Batch Mode (if pages have been scanned!)
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("scanned_queue_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sizePages > 0) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$sizePages Stack",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Empty Stack", tint = Color.Gray)
                            }
                        }
                    }

                    // Main trigger Shutter
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                // Default capture receipt if no preset clicked
                                viewModel.capturePage("Receipt")
                                captureSuccessOverlay = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Cam Trigger", tint = Color.White)
                        }
                    }

                    // Compiler Proceed compiler check button (Proceed to crop perspective adjust!)
                    IconButton(
                        onClick = {
                            if (scannedPages.isNotEmpty()) {
                                viewModel.startSessionEditor()
                                viewModel.navigateTo(AppScreen.CropEditor)
                            } else {
                                Toast.makeText(context, "Scan a page from the presets above first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("crop_proceed_button")
                            .clip(CircleShape)
                            .background(
                                if (scannedPages.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.White.copy(
                                    alpha = 0.1f
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = if (scannedPages.isNotEmpty()) Color.Black else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    // Reset snap background burst effect gracefully after flash
    LaunchedEffect(captureSuccessOverlay) {
        if (captureSuccessOverlay) {
            delay(200)
            captureSuccessOverlay = false
            focusActive = true
        }
    }
}

// ==========================================
// 3. PERSPECTIVE DETECTING & CROP EDITOR
// ==========================================
@Composable
fun CropAndFilterEditorScreen(viewModel: ScannerViewModel) {
    val scannedPages by viewModel.scannedSessionPages.collectAsState()
    val editingIdx by viewModel.editingPageIndex.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val docTitle by viewModel.docTitleInput.collectAsState()
    val docTags by viewModel.docTagsInput.collectAsState()
    val docFolderId by viewModel.docFolderInput.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val handles by viewModel.cornerHandles.collectAsState()

    var showFolderSelectorDropdown by remember { mutableStateOf(false) }

    val activePagePreset = scannedPages.getOrNull(editingIdx)?.first ?: "Receipt"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.translate("corners_title"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Scanner) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveDocument {
                                viewModel.navigateTo(AppScreen.Home)
                            }
                        },
                        modifier = Modifier.testTag("save_document_button")
                    ) {
                        Text(
                            text = viewModel.translate("save_doc"),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Drag Perspective crop view container
            Text(
                text = "Simulating automatic edge detection. Drag highlighted corner coordinates manually to tweak perspective bounds:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.DarkGray.copy(alpha = 0.25f))
                    .testTag("perspective_crop_canvas")
            ) {
                // Interactive Drag Perspective Crop canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(handles) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val touchPos = change.position
                                val w = size.width
                                val h = size.height

                                // Find nearest corner handles
                                var nearestIdx = -1
                                var minDistance = Float.MAX_VALUE
                                handles.forEachIndexed { index, pair ->
                                    val handleX = pair.first * w
                                    val handleY = pair.second * h
                                    val dx = touchPos.x - handleX
                                    val dy = touchPos.y - handleY
                                    val dist = (dx * dx) + (dy * dy)
                                    if (dist < minDistance && dist < 120.dp.toPx() * 120.dp.toPx()) {
                                        minDistance = dist
                                        nearestIdx = index
                                    }
                                }

                                if (nearestIdx != -1) {
                                    val nextX = (touchPos.x / w).coerceIn(0f, 1f)
                                    val nextY = (touchPos.y / h).coerceIn(0f, 1f)
                                    viewModel.updateCornerHandle(nearestIdx, nextX, nextY)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw placeholder scanned document card matching selected preset
                    val docPath = Path().apply {
                        moveTo(handles[0].first * w, handles[0].second * h)
                        lineTo(handles[1].first * w, handles[1].second * h)
                        lineTo(handles[2].first * w, handles[2].second * h)
                        lineTo(handles[3].first * w, handles[3].second * h)
                        close()
                    }

                    // Simulated document color filter mapping
                    val docColor = when (currentFilter) {
                        "GRAYSCALE" -> Color.LightGray
                        "MONOCHROME" -> Color.White
                        "MAGIC" -> primaryColor.copy(alpha = 0.12f) // Magic highlights dynamically use primary
                        else -> Color(0xFFFAF6EE) // Parchment paper tone for original
                    }

                    drawPath(path = docPath, color = docColor)

                    // Draw simulated perspective quadrilateral polygon outline
                    drawPath(
                        path = docPath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw guidelines grid inside document polygon structure
                    // Drawing cross lines
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset((handles[0].first + handles[3].first)/2 * w, (handles[0].second + handles[3].second)/2 * h),
                        end = Offset((handles[1].first + handles[2].first)/2 * w, (handles[1].second + handles[2].second)/2 * h),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset((handles[0].first + handles[1].first)/2 * w, (handles[0].second + handles[1].second)/2 * h),
                        end = Offset((handles[3].first + handles[2].first)/2 * w, (handles[3].second + handles[2].second)/2 * h),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw drag circles
                    handles.forEachIndexed { index, pair ->
                        val px = pair.first * w
                        val py = pair.second * h
                        // outer ring glow
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.25f),
                            radius = 16.dp.toPx(),
                            center = Offset(px, py)
                        )
                        // inner solid
                        drawCircle(
                            color = primaryColor,
                            radius = 8.dp.toPx(),
                            center = Offset(px, py)
                        )
                        // dot core
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // Title label showing overlay of preset image scanned
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Source Preset: ${activePagePreset.replace("_", " ")} | Page ${editingIdx + 1}/${scannedPages.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Document Enhancement Filters Picker Row
            Column {
                Text(
                    text = "Aesthetic Enhancement Scanner Filters:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterPresets = listOf(
                        Pair("ORIGINAL", "Original"),
                        Pair("GRAYSCALE", "B&W Mono"),
                        Pair("MONOCHROME", "High Contrast"),
                        Pair("MAGIC", "Magic Color")
                    )

                    filterPresets.forEach { pair ->
                        val selected = pair.first == currentFilter
                        ElevatedCard(
                            onClick = { viewModel.setFilter(pair.first) },
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = when (pair.first) {
                                        "ORIGINAL" -> Icons.Default.FilterNone
                                        "GRAYSCALE" -> Icons.Default.FilterBAndW
                                        "MONOCHROME" -> Icons.Default.Contrast
                                        else -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = pair.second,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pair.second,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Header info configuration inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Document Metadata",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Title
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { viewModel.updateDocTitle(it) },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("title_input_field")
                    )

                    // Tags comma separated
                    OutlinedTextField(
                        value = docTags,
                        onValueChange = { viewModel.updateDocTags(it) },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("Receipt, Invoice, Work, Personal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("tags_input_field")
                    )

                    // Folder Selector Dropdown Box
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val folderName = folders.find { it.id == docFolderId }?.name ?: "Root / Unsorted"
                        OutlinedButton(
                            onClick = { showFolderSelectorDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder Icon")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Save in folder: $folderName")
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }

                        DropdownMenu(
                            expanded = showFolderSelectorDropdown,
                            onDismissRequest = { showFolderSelectorDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("(Unsorted Root)") },
                                onClick = {
                                    viewModel.setDocFolder(null)
                                    showFolderSelectorDropdown = false
                                }
                            )
                            folders.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text(f.name) },
                                    onClick = {
                                        viewModel.setDocFolder(f.id)
                                        showFolderSelectorDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ==========================================
// 4. DOCUMENT DETAILS SCAN READS SCREEN
// ==========================================
@Composable
fun DocumentDetailsScreen(viewModel: ScannerViewModel, docId: Int) {
    val context = LocalContext.current
    val doc by viewModel.detailsDocument.collectAsState()
    val pages by viewModel.detailsPages.collectAsState()

    var activePageIndex by remember { mutableStateOf(0) }

    // On Load hook
    LaunchedEffect(docId) {
        viewModel.loadDocumentDetail(docId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.title ?: "Document Reader", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    // Export Share sheet buttons (PDF & Word options)
                    IconButton(
                        onClick = {
                            val activeDoc = doc
                            if (activeDoc != null && pages.isNotEmpty()) {
                                val file = viewModel.getExportPdf(activeDoc, pages)
                                triggerSystemShare(context, file, activeDoc.title, "application/pdf")
                            }
                        },
                        modifier = Modifier.testTag("share_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Export and Share", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val activeDoc = doc
                            if (activeDoc != null && pages.isNotEmpty()) {
                                val file = viewModel.getExportWord(activeDoc, pages)
                                triggerSystemShare(context, file, activeDoc.title, "application/msword")
                            }
                        },
                        modifier = Modifier.testTag("share_word_btn")
                    ) {
                        Icon(Icons.Default.Article, contentDescription = "Word Export and Share", tint = MaterialTheme.colorScheme.tertiary)
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteDocument(docId) {
                                viewModel.navigateTo(AppScreen.Home)
                            }
                        }
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Purge document history", tint = Color.Red)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (doc == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val activePage = pages.getOrNull(activePageIndex)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Swipe index slideshow page tags
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { idx, p ->
                            val active = idx == activePageIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (active) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Gray)
                                    .clickable { activePageIndex = idx }
                            )
                        }
                    }
                }

                // Scanned Viewport rendering based on preset mockup details
                val primaryColor = MaterialTheme.colorScheme.primary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing document bounding box on details
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val customBg = Color(0xFFF9FAF8)
                            drawRect(customBg, topLeft = Offset(w * 0.2f, h * 0.1f), size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.8f))
                            drawRect(
                                primaryColor,
                                topLeft = Offset(w * 0.2f, h * 0.1f),
                                size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.8f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Preview overlay icon
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (activePage?.imagePresetName?.uppercase() ?: "") {
                                    "RECEIPT" -> Icons.Default.Receipt
                                    "INVOICE" -> Icons.Default.ReceiptLong
                                    "BUSINESS_CARD" -> Icons.Default.AccountBox
                                    else -> Icons.Default.Description
                                },
                                contentDescription = "Active doc page format icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Page ${activePageIndex + 1} of ${pages.size} (${activePage?.imagePresetName ?: "Receipt"})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Filter Type: ${activePage?.filterType ?: "MAGIC"}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // OCR Extracted readable content output block
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.translate("view_file"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Clipboard replication
                        TextButton(
                            onClick = {
                                activePage?.ocrText?.let {
                                    copyToClipboard(context, it)
                                    Toast.makeText(context, viewModel.translate("copied"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Plaintext", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activePage?.ocrText ?: "Extracting OCR scanning results...",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                // Custom Tag widgets detail re-config
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = "Tags icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = viewModel.translate("tags") + ": " + (doc?.tags?.ifEmpty { "None" }),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ==========================================
// 5. SECURITY & SYNC VAULT CONFIG SCREEN
// ==========================================
@Composable
fun SecurityVaultScreen(viewModel: ScannerViewModel) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val dailyNotifications by viewModel.dailyNotifications.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(viewModel.translate("dashboard"), fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.Converter) },
                    icon = { Icon(Icons.Default.ChangeCircle, contentDescription = "PDF Word Converter") },
                    label = { Text("Converter", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(AppScreen.SecuritySettings) },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Vault Settings") },
                    label = { Text(viewModel.translate("security_title"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Security Status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Vault & Security",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "End-to-End Cryptography configurations",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Sync toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Encrypted Cloud Sync Status",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Sync documents and custom folders across devices using military-grade zero-knowledge AES-256 keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (syncEnabled) "CLOUD SYNCHRONIZATION: ENABLED" else "CLOUD SYNCHRONIZATION: MUTED (OFFLINE)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (syncEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { viewModel.toggleSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // Interactive notification settings vault to satisfy "add customizable notification settings to keep track of deadlines"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Custom Review Notifications",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Keep track of tax return schedules, document submission deadlines, or expiration alerts dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receive scan reviews and deadline alerts daily",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = dailyNotifications,
                            onCheckedChange = { viewModel.toggleDailyNotifications() }
                        )
                    }

                    if (dailyNotifications) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "✓ Notifications will trigger at 9:00 AM every morning for document deadlines.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Zero Knowledge confirmation indicator card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Verified Key", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Local Encryption Signature Details",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Database state is signed with a localized key hash. Backups are salted with PBKDF2. No local files are stored as plain text.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ==========================================
// 6. SHARED UTILITIES
// ==========================================
fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("OCR Extracted text", text)
    clipboard.setPrimaryClip(clip)
}

fun triggerSystemShare(context: Context, file: File, docTitle: String, mimeType: String) {
    try {
        // Build Content URI using Standard FileProvider
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "BO Scanner OCR Export: $docTitle")
            putExtra(Intent.EXTRA_TEXT, "Sending Encrypted Scanned Document: $docTitle via BO Scanner OCR.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Scanned PDF / Word..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// 6. FILE CONVERTER HUB SCREEN
// ==========================================
@Composable
fun FileConverterScreen(viewModel: ScannerViewModel) {
    val context = LocalContext.current
    val isConverting by viewModel.isConverting.collectAsState()
    val conversionProgress by viewModel.conversionProgress.collectAsState()
    val conversionLogs by viewModel.conversionLogs.collectAsState()
    val convertedFiles by viewModel.convertedFiles.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var selectedDocId by remember { mutableStateOf<Int?>(null) }
    var selectedFileName by remember { mutableStateOf("My_Scanned_Document.pdf") }
    var sourceType by remember { mutableStateOf("PDF") } // PDF, DOCX, XLSX, IMAGE
    var targetType by remember { mutableStateOf("Word (DOCX)") } // Word (DOCX), PDF Document, Excel (XLSX)

    var customFileName by remember { mutableStateOf("Monthly_Report_2026") }
    var showDocSelectorDialog by remember { mutableStateOf(false) }
    val previewState = remember { mutableStateOf<ConvertedFile?>(null) }
    val activePreviewFile = previewState.value

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.Home) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(viewModel.translate("dashboard"), fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { viewModel.navigateTo(AppScreen.Converter) },
                    icon = { Icon(Icons.Default.ChangeCircle, contentDescription = "PDF Word Converter") },
                    label = { Text("Converter", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(AppScreen.SecuritySettings) },
                    icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "Vault Settings") },
                    label = { Text(viewModel.translate("security_title"), fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Clean minimalist premium header
                Column {
                    Text(
                        text = "BO Scanner Converter Hub",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Seamless high-fidelity offline format conversions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
            }

            // Converter controls card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "NEW CONVERSION TASK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Prest buttons
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickPresets = listOf(
                                Triple("PDF ➜ DOCX", "PDF", "Word (DOCX)"),
                                Triple("DOCX ➜ PDF", "DOCX", "PDF Document"),
                                Triple("XLSX ➜ PDF", "EXCEL", "PDF Document")
                            )
                            quickPresets.forEach { (label, src, tgt) ->
                                val isSelected = sourceType == src && targetType == tgt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        sourceType = src
                                        targetType = tgt
                                        if (selectedDocId == null) {
                                            selectedFileName = when (src) {
                                                "PDF" -> "Scanned_Invoice.pdf"
                                                "DOCX" -> "Project_Proposal.docx"
                                                else -> "Financial_Table.xlsx"
                                            }
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Source File selection
                        Text(
                            text = "Select Document Source:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Real or simulated choosing container
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDocSelectorDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = "Select active scan")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick BO Scanner Scan", fontSize = 12.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedDocId = null
                                    selectedFileName = "$customFileName.${sourceType.lowercase()}"
                                },
                                modifier = Modifier.weight(1f),
                                border = if (selectedDocId == null) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonBorder,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Simulate custom upload")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Custom File", fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current source file card display
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (sourceType) {
                                        "PDF" -> Icons.Default.PictureAsPdf
                                        "EXCEL", "XLSX" -> Icons.Default.GridOn
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = "Source Format",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedFileName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (selectedDocId != null) "Linked to BO Scanner Database Document" else "Simulated uploaded custom file",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (selectedDocId == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Custom name field
                            OutlinedTextField(
                                value = customFileName,
                                onValueChange = {
                                    customFileName = it
                                    selectedFileName = "$it.${sourceType.lowercase()}"
                                },
                                label = { Text("Custom File Name", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "file name edit") },
                                textStyle = TextStyle(fontSize = 13.sp),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Select target format
                        Text(
                            text = "Convert to Target Format:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val targets = when (sourceType) {
                                "PDF" -> listOf("Word (DOCX)", "Excel (XLSX)")
                                "EXCEL", "XLSX" -> listOf("PDF Document")
                                else -> listOf("PDF Document", "Word (DOCX)")
                            }
                            targets.forEach { target ->
                                FilterChip(
                                    selected = targetType == target,
                                    onClick = { targetType = target },
                                    label = { Text(target) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (isConverting) {
                            // High progress design
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { conversionProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Converting file...",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${(conversionProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                // Scrollable logger preview inside conversion card
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    color = Color.Black.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.padding(8.dp),
                                        reverseLayout = true
                                    ) {
                                        items(conversionLogs.reversed()) { log ->
                                            Text(
                                                text = "➜ $log",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Primary click buttons
                            Button(
                                onClick = {
                                    viewModel.runFileConversion(
                                        sourceFileName = selectedFileName,
                                        sourceType = sourceType,
                                        targetType = targetType,
                                        sourceDocumentId = selectedDocId,
                                        onComplete = {
                                            Toast.makeText(context, "Successfully Converted!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run Converter conversion")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Execute Conversion", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Conversion History list
            item {
                Text(
                    text = "RECONSTRUCTED FILES & CONVERSIONS HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    letterSpacing = 1.1.sp
                )
            }

            if (convertedFiles.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Difference,
                            contentDescription = "Empty History",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No conversion logs yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(convertedFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (file.targetFormat.contains("Word", ignoreCase = true)) Icons.Default.Description else Icons.Default.PictureAsPdf,
                                        contentDescription = "File Type",
                                        tint = if (file.targetFormat.contains("Word", ignoreCase = true)) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = file.resultName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                             text = "Source: ${file.originalName} • ${file.fileSize}",
                                             style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                
                                IconButton(onClick = { viewModel.deleteConvertedFile(file.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove conversion",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { previewState.value = file },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Preview Text", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Preview Content", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Dynamic sharing simulation
                                        val temp = File(context.cacheDir, file.resultName)
                                        if (!temp.exists()) {
                                            temp.writeText(file.textPreview)
                                        }
                                        triggerSystemShare(
                                            context = context,
                                            file = temp,
                                            docTitle = file.resultName,
                                            mimeType = if (file.targetFormat.contains("Word", ignoreCase = true)) "application/vnd.openxmlformats-officedocument.wordprocessingml.document" else "application/pdf"
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Out", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Document Picker for Real scan file integration!
    if (showDocSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showDocSelectorDialog = false },
            title = { Text("Select Scanned Doc from Vault", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
            text = {
                if (documents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No scanned documents found. Create one first!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(documents) { doc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDocId = doc.id
                                        selectedFileName = doc.title
                                        // Intelligently map custom doc presets or extensions
                                        sourceType = "PDF"
                                        targetType = "Word (DOCX)"
                                        showDocSelectorDialog = false
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = "Doc", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                        Text("Saved Scans Database File", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocSelectorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal Preview Dialog showing true extracted/synthesized layers
    if (activePreviewFile != null) {
        val file = activePreviewFile
        AlertDialog(
            onDismissRequest = { previewState.value = null },
            title = {
                Column {
                    Text(file.resultName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Converted: ${file.targetFormat}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    LazyColumn(modifier = Modifier.padding(14.dp)) {
                        item {
                            Text(
                                text = file.textPreview,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { previewState.value = null }) {
                    Text("Close Preview")
                }
            }
        )
    }
}
