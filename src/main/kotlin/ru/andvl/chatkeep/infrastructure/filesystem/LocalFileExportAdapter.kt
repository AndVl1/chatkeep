package ru.andvl.chatkeep.infrastructure.filesystem

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.andvl.chatkeep.config.AdminLogsProperties
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Local file system implementation of FileExportPort.
 */
@Component
class LocalFileExportAdapter(
    private val properties: AdminLogsProperties
) : FileExportPort {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        ensureExportDirectoryExists()
    }

    override fun exportJson(fileName: String, content: String): File {
        val filePath = Paths.get(properties.path, fileName)
        val file = filePath.toFile()

        try {
            file.writeText(content)
            logger.info("Exported JSON to file: ${file.absolutePath}")
        } catch (e: Exception) {
            logger.error("Failed to export JSON to file: ${file.absolutePath}", e)
            throw e
        }

        return file
    }

    override fun deleteFile(file: File) {
        try {
            if (file.exists()) {
                file.delete()
                logger.info("Deleted file: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            logger.warn("Failed to delete file: ${file.absolutePath}", e)
        }
    }

    private fun ensureExportDirectoryExists() {
        val path = Paths.get(properties.path)
        if (!path.exists()) {
            try {
                path.createDirectories()
                logger.info("Created export directory: ${path.toAbsolutePath()}")
            } catch (e: Exception) {
                logger.error("Failed to create export directory: ${path.toAbsolutePath()}", e)
                throw e
            }
        }
    }
}
