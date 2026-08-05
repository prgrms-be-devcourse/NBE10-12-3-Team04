package com.triptrace.domain.image.image.storage

import com.triptrace.domain.image.image.processing.dto.StoredFile
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Paths

@Component
class LocalFileStorage : FileStorage {
    override fun save(file: ByteArray, filePath: String, fileName: String): StoredFile {
        val path = Paths.get(filePath)
        Files.createDirectories(path)
        Files.write(path.resolve(fileName), file)
        return StoredFile(filePath, fileName, file.size.toLong())
    }

    override fun delete(filePath: String) {
        Files.deleteIfExists(Paths.get(filePath))
    }
}
