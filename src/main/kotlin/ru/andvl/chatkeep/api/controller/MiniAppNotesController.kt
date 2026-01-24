package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.auth.TelegramAuthFilter
import ru.andvl.chatkeep.api.auth.TelegramAuthService
import ru.andvl.chatkeep.api.dto.CreateNoteRequest
import ru.andvl.chatkeep.api.dto.ModerationActionResponse
import ru.andvl.chatkeep.api.dto.NoteResponse
import ru.andvl.chatkeep.api.dto.UpdateNoteRequest
import ru.andvl.chatkeep.api.exception.AccessDeniedException
import ru.andvl.chatkeep.api.exception.ResourceNotFoundException
import ru.andvl.chatkeep.api.exception.UnauthorizedException
import ru.andvl.chatkeep.api.exception.ValidationException
import ru.andvl.chatkeep.domain.service.NotesService
import ru.andvl.chatkeep.domain.service.moderation.AdminCacheService

@RestController
@RequestMapping("/api/v1/miniapp/chats/{chatId}/notes")
@Tag(name = "Mini App - Notes", description = "Saved notes management")
@SecurityRequirement(name = "TelegramAuth")
class MiniAppNotesController(
    private val notesService: NotesService,
    private val adminCacheService: AdminCacheService
) {

    private fun getUserFromRequest(request: HttpServletRequest): TelegramAuthService.TelegramUser {
        return request.getAttribute(TelegramAuthFilter.USER_ATTR) as? TelegramAuthService.TelegramUser
            ?: throw UnauthorizedException("User not authenticated")
    }

    @GetMapping
    @Operation(summary = "Get all notes for a chat")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun getNotes(
        @PathVariable chatId: Long,
        request: HttpServletRequest
    ): List<NoteResponse> {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = false)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val notes = notesService.getAllNotes(chatId)

        return notes.map { note ->
            NoteResponse(
                id = note.id,
                chatId = note.chatId,
                noteName = note.noteName,
                content = note.content,
                createdBy = note.createdBy,
                createdAt = note.createdAt.toString()
            )
        }
    }

    @PostMapping
    @Operation(summary = "Create a new note")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Created"),
        ApiResponse(responseCode = "400", description = "Note already exists"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin")
    )
    fun createNote(
        @PathVariable chatId: Long,
        @Valid @RequestBody createRequest: CreateNoteRequest,
        request: HttpServletRequest
    ): ResponseEntity<NoteResponse> {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val saved = try {
            notesService.createNote(
                chatId = chatId,
                noteName = createRequest.noteName,
                content = createRequest.content,
                createdBy = user.id
            )
        } catch (e: IllegalStateException) {
            throw ValidationException(e.message ?: "Note already exists")
        }

        val response = NoteResponse(
            id = saved.id,
            chatId = saved.chatId,
            noteName = saved.noteName,
            content = saved.content,
            createdBy = saved.createdBy,
            createdAt = saved.createdAt.toString()
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update a note")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Note not found")
    )
    fun updateNote(
        @PathVariable chatId: Long,
        @PathVariable noteId: Long,
        @Valid @RequestBody updateRequest: UpdateNoteRequest,
        request: HttpServletRequest
    ): NoteResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val updated = try {
            notesService.updateNote(chatId, noteId, updateRequest.content)
        } catch (e: IllegalArgumentException) {
            throw ResourceNotFoundException("Note", noteId)
        }

        return NoteResponse(
            id = updated.id,
            chatId = updated.chatId,
            noteName = updated.noteName,
            content = updated.content,
            createdBy = updated.createdBy,
            createdAt = updated.createdAt.toString()
        )
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete a note")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "403", description = "Forbidden - not admin"),
        ApiResponse(responseCode = "404", description = "Note not found")
    )
    fun deleteNote(
        @PathVariable chatId: Long,
        @PathVariable noteId: Long,
        request: HttpServletRequest
    ): ModerationActionResponse {
        val user = getUserFromRequest(request)

        val isAdmin = runBlocking(Dispatchers.IO) {
            adminCacheService.isAdmin(user.id, chatId, forceRefresh = true)
        }
        if (!isAdmin) {
            throw AccessDeniedException("You are not an admin in this chat")
        }

        val deleted = notesService.deleteNote(chatId, noteId)
        if (!deleted) {
            throw ResourceNotFoundException("Note", noteId)
        }

        return ModerationActionResponse(
            success = true,
            message = "Note deleted successfully"
        )
    }
}
