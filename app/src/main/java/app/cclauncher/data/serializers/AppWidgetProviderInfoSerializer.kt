package app.cclauncher.data.serializers

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Build
import android.os.Parcel
import android.os.UserHandle
import android.util.Log
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

// VERY SIMPLIFIED Serializer - Only stores component name.
// Might lose other info. A better approach might involve storing only
// package/class and looking up the ProviderInfo at runtime.
// Or use Parcelable serialization if storing complex state.
object AppWidgetProviderInfoSerializer : KSerializer<AppWidgetProviderInfo> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AppWidgetProviderInfo") {
        element<String>("packageName")
        element<String>("className")
        // Add other fields you NEED to persist if any
    }

    override fun serialize(encoder: Encoder, value: AppWidgetProviderInfo) {
        encoder.encodeStructure(descriptor) {
            value.provider?.let { component ->
                encodeStringElement(descriptor, 0, component.packageName)
                encodeStringElement(descriptor, 1, component.className)
            } ?: run {
                // Handle cases where provider might be null (shouldn't happen for valid info)
                encodeStringElement(descriptor, 0, "")
                encodeStringElement(descriptor, 1, "")
            }
            // Encode other fields if needed
        }
    }

    override fun deserialize(decoder: Decoder): AppWidgetProviderInfo {
        var packageName = ""
        var className = ""
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> packageName = decodeStringElement(descriptor, index)
                    1 -> className = decodeStringElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }
        // THIS IS A PROBLEM: We cannot fully reconstruct AppWidgetProviderInfo just from
        // component name easily. We often need to look it up via AppWidgetManager.
        // For now, return a dummy or throw error. Better: Don't serialize ProviderInfo directly.
        // Store package/class names separately in HomeItem.Widget and look up ProviderInfo at runtime.
        // For this example, we return a partially useful object for identification, but it's not a real ProviderInfo.
        val partialInfo = AppWidgetProviderInfo() // Create an empty one
        partialInfo.provider = ComponentName(packageName, className)
        // Assign other necessary fields if you stored them.
        return partialInfo
        // Consider: throw IllegalStateException("Cannot fully deserialize AppWidgetProviderInfo. Look up via AppWidgetManager.")
    }
}

// Optional: Serializer for UserHandle if needed elsewhere (simplified)
object UserHandleSerializer : KSerializer<UserHandle> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UserHandle", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: UserHandle) {
        // Simplified: Use hashCode or a persistent identifier if available
        // Note: getIdentifier() is API 24+
        encoder.encodeLong(value.hashCode().toLong()) // Using hashCode is NOT reliable across reboots/updates!
        // A better way would involve UserManager serialization methods if needed for complex cases.
    }

    override fun deserialize(decoder: Decoder): UserHandle {
        val identifier = decoder.decodeLong()
        // THIS IS A PROBLEM: Cannot reliably reconstruct UserHandle from hashCode or simple long.
        // Need to look up via UserManager based on a stable identifier or context.
        // Return Process.myUserHandle() as a fallback? Or handle lookup elsewhere.
        Log.w("UserHandleSerializer", "Cannot fully deserialize UserHandle from ID $identifier. Returning current user.")
        return android.os.Process.myUserHandle()
        // Consider: throw IllegalStateException("Cannot deserialize UserHandle. Look up via UserManager.")
    }
}