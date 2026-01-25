package ru.andvl.chatkeep.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import ru.andvl.chatkeep.api.dto.FeatureStatusDto
import ru.andvl.chatkeep.api.dto.SetFeatureRequest
import ru.andvl.chatkeep.domain.service.gated.GatedFeatureService

@RestController
@RequestMapping("/api/v1/admin/chats/{chatId}/features")
@Tag(name = "Admin - Gated Features", description = "Admin API for managing gated features")
@SecurityRequirement(name = "BearerAuth")
class AdminGatedFeaturesController(
    private val gatedFeatureService: GatedFeatureService
) {

    @GetMapping
    @Operation(summary = "Get all features for a chat (Admin)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success")
    )
    fun getFeatures(
        @PathVariable chatId: Long
    ): List<FeatureStatusDto> {
        return gatedFeatureService.getFeatures(chatId).map { feature ->
            FeatureStatusDto(
                key = feature.key,
                enabled = feature.enabled,
                name = feature.name,
                description = feature.description,
                enabledAt = feature.enabledAt,
                enabledBy = feature.enabledBy
            )
        }
    }

    @PutMapping("/{featureKey}")
    @Operation(summary = "Enable or disable a feature (Admin)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode = "400", description = "Invalid feature key")
    )
    fun setFeature(
        @PathVariable chatId: Long,
        @PathVariable featureKey: String,
        @Valid @RequestBody request: SetFeatureRequest
    ): FeatureStatusDto {
        // Admin API - using 0 as userId since it's admin action
        val feature = gatedFeatureService.setFeature(chatId, featureKey, request.enabled, 0)

        return FeatureStatusDto(
            key = feature.key,
            enabled = feature.enabled,
            name = feature.name,
            description = feature.description,
            enabledAt = feature.enabledAt,
            enabledBy = feature.enabledBy
        )
    }
}
