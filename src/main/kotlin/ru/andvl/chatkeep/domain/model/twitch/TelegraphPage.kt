package ru.andvl.chatkeep.domain.model.twitch

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Telegraph API response models
 */
@Serializable
data class TelegraphResponse<T>(
    val ok: Boolean,
    val result: T? = null,
    val error: String? = null
)

@Serializable
data class TelegraphPage(
    val path: String,
    val url: String,
    val title: String,
    val description: String,
    val views: Int = 0
)

@Serializable
data class TelegraphAccount(
    val short_name: String,
    val author_name: String,
    val author_url: String? = null,
    val access_token: String? = null,
    val auth_url: String? = null
)

@Serializable
data class CreatePageRequest(
    val access_token: String,
    val title: String,
    val content: List<NodeElement>,
    val author_name: String = "Chatkeep Bot",
    val author_url: String? = null,
    val return_content: Boolean = false
)

@Serializable
data class EditPageRequest(
    val access_token: String,
    val path: String,
    val title: String,
    val content: List<NodeElement>,
    val author_name: String = "Chatkeep Bot",
    val author_url: String? = null,
    val return_content: Boolean = false
)

/**
 * Telegraph DOM content node
 * Can be either a string (text) or structured element
 */
@Serializable(with = NodeElementSerializer::class)
sealed class NodeElement {
    data class Text(val text: String) : NodeElement()
    data class Tag(
        val tag: String,
        val attrs: Map<String, String>? = null,
        val children: List<NodeElement>? = null
    ) : NodeElement()
}

object NodeElementSerializer : KSerializer<NodeElement> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NodeElement", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NodeElement {
        val input = decoder as? JsonDecoder ?: throw IllegalStateException("This serializer can be used only with JSON")
        val element = input.decodeJsonElement()

        return when {
            element is JsonPrimitive && element.isString -> NodeElement.Text(element.content)
            element is JsonObject -> {
                val tag = element["tag"]?.jsonPrimitive?.content ?: ""
                val attrs = element["attrs"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }
                val children = element["children"]?.jsonArray?.map {
                    Json.decodeFromJsonElement<NodeElement>(it)
                }
                NodeElement.Tag(tag, attrs, children)
            }
            else -> throw IllegalArgumentException("Unknown NodeElement type")
        }
    }

    override fun serialize(encoder: Encoder, value: NodeElement) {
        val output = encoder as? JsonEncoder ?: throw IllegalStateException("This serializer can be used only with JSON")
        val element = when (value) {
            is NodeElement.Text -> JsonPrimitive(value.text)
            is NodeElement.Tag -> buildJsonObject {
                put("tag", value.tag)
                value.attrs?.let { attrs ->
                    put("attrs", buildJsonObject {
                        attrs.forEach { (k, v) -> put(k, v) }
                    })
                }
                value.children?.let { children ->
                    put("children", buildJsonArray {
                        children.forEach { child ->
                            add(Json.encodeToJsonElement(child))
                        }
                    })
                }
            }
        }
        output.encodeJsonElement(element)
    }
}
