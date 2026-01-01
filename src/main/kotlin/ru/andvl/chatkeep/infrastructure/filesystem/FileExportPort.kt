package ru.andvl.chatkeep.infrastructure.filesystem

import java.io.File

/**
 * Port for file export operations.
 * Clean Architecture abstraction for file system interactions.
 */
interface FileExportPort {
    /**
     * Export content to a file in JSON format.
     *
     * @param fileName Name of the file to create
     * @param content Content to write
     * @return Created file
     */
    fun exportJson(fileName: String, content: String): File

    /**
     * Delete a file.
     *
     * @param file File to delete
     */
    fun deleteFile(file: File)
}
