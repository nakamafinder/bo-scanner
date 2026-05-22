package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int? = null, // null means unsorted / Root
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tags: String = "", // Comma-separated tags, e.g., "Invoice,Work"
    val isSynced: Boolean = false,
    val isEncrypted: Boolean = true
)

@Entity(tableName = "document_pages")
data class DocumentPage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val pageNumber: Int,
    val imagePresetName: String, // Mockup preset like "RECEIPT", "INVOICE", "BUSINESS_CARD", "BOOK_PAGE"
    val filterType: String = "MAGIC", // ORIGINAL, GRAYSCALE, MONOCHROME, MAGIC
    val ocrText: String = "",
    val cropCorners: String = "0.05,0.05,0.95,0.08,0.92,0.95,0.08,0.92" // Simulated normalized corner handles (x1,y1,...) after auto edge detection
)
