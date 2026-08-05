package com.triptrace.domain.image.image.storage

import com.triptrace.domain.image.image.processing.dto.StoredFile
import java.io.IOException

interface FileStorage {
    @Throws(IOException::class)
    fun save(file: ByteArray, filePath: String, fileName: String): StoredFile

    @Throws(IOException::class)
    fun delete(filePath: String)
}
