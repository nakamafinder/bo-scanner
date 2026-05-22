package com.example.data.local

import androidx.room.*
import com.example.data.model.Folder
import com.example.data.model.Document
import com.example.data.model.DocumentPage
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    // --- Folder Queries ---
    @Query("SELECT * FROM folders ORDER BY timestamp DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Delete
    suspend fun deleteFolder(folder: Folder)

    // --- Document Queries ---
    @Query("SELECT * FROM documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getDocumentsInFolder(folderId: Int): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderId IS NULL ORDER BY timestamp DESC")
    fun getUnsortedDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): Document?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: Document): Long

    @Update
    suspend fun updateDocument(document: Document)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Int)

    // --- Document Page Queries ---
    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesForDocument(documentId: Int): Flow<List<DocumentPage>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesForDocumentSync(documentId: Int): List<DocumentPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPage): Long

    @Update
    suspend fun updatePage(page: DocumentPage)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Int)

    // --- Complex Search (Title, Tags, or OCR Text match) ---
    @Query("""
        SELECT DISTINCT d.* FROM documents d 
        LEFT JOIN document_pages p ON d.id = p.documentId 
        WHERE d.title LIKE '%' || :query || '%' 
        OR d.tags LIKE '%' || :query || '%' 
        OR p.ocrText LIKE '%' || :query || '%'
        ORDER BY d.timestamp DESC
    """)
    fun searchDocuments(query: String): Flow<List<Document>>
}
