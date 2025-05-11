package app.cclauncher.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

object HomeItemAppSerializer : KSerializer<HomeItem.App> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HomeItem.App") {
        element<String>("id")
        element<Int>("row")
        element<Int>("column")
        element<Int>("rowSpan")
        element<Int>("columnSpan")
        element<String>("appLabel")
        element<String>("appPackage")
        element<String?>("activityClassName")
        element<String>("userString")
        element<Boolean>("isHidden")
    }

    override fun serialize(encoder: Encoder, value: HomeItem.App) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeIntElement(descriptor, 1, value.row)
            encodeIntElement(descriptor, 2, value.column)
            encodeIntElement(descriptor, 3, value.rowSpan)
            encodeIntElement(descriptor, 4, value.columnSpan)
            encodeStringElement(descriptor, 5, value.appModel.appLabel)
            encodeStringElement(descriptor, 6, value.appModel.appPackage)
            encodeStringElement(descriptor, 7, value.appModel.activityClassName.toString())
            encodeStringElement(descriptor, 8, value.appModel.userString)
            encodeBooleanElement(descriptor, 9, value.appModel.isHidden)
        }
    }

    override fun deserialize(decoder: Decoder): HomeItem.App {
        var id = ""
        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1
        var appLabel = ""
        var appPackage = ""
        var activityClassName: String? = null
        var userString = ""
        var isHidden = false

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, index)
                    1 -> row = decodeIntElement(descriptor, index)
                    2 -> column = decodeIntElement(descriptor, index)
                    3 -> rowSpan = decodeIntElement(descriptor, index)
                    4 -> columnSpan = decodeIntElement(descriptor, index)
                    5 -> appLabel = decodeStringElement(descriptor, index)
                    6 -> appPackage = decodeStringElement(descriptor, index)
                    7 -> activityClassName = decodeStringElement(descriptor, index)
                    8 -> userString = decodeStringElement(descriptor, index)
                    9 -> isHidden = decodeBooleanElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        // Create AppModel with serializable data
        val appModel = AppModel(
            appLabel = appLabel,
            appPackage = appPackage,
            activityClassName = activityClassName,
            isHidden = isHidden,
            userString = userString
        )

        return HomeItem.App(
            id = id,
            appModel = appModel,
            row = row,
            column = column,
            rowSpan = rowSpan,
            columnSpan = columnSpan
        )
    }
}

object HomeItemWidgetSerializer : KSerializer<HomeItem.Widget> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HomeItem.Widget") {
        element<String>("id")
        element<Int>("appWidgetId")
        element<String>("packageName")
        element<String>("providerClassName")
        element<Int>("row")
        element<Int>("column")
        element<Int>("rowSpan")
        element<Int>("columnSpan")
    }

    override fun serialize(encoder: Encoder, value: HomeItem.Widget) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeIntElement(descriptor, 1, value.appWidgetId)
            encodeStringElement(descriptor, 2, value.packageName)
            encodeStringElement(descriptor, 3, value.providerClassName)
            encodeIntElement(descriptor, 4, value.row)
            encodeIntElement(descriptor, 5, value.column)
            encodeIntElement(descriptor, 6, value.rowSpan)
            encodeIntElement(descriptor, 7, value.columnSpan)
        }
    }

    override fun deserialize(decoder: Decoder): HomeItem.Widget {
        var id = ""
        var appWidgetId = -1
        var packageName = ""
        var providerClassName = ""
        var row = 0
        var column = 0
        var rowSpan = 1
        var columnSpan = 1

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, index)
                    1 -> appWidgetId = decodeIntElement(descriptor, index)
                    2 -> packageName = decodeStringElement(descriptor, index)
                    3 -> providerClassName = decodeStringElement(descriptor, index)
                    4 -> row = decodeIntElement(descriptor, index)
                    5 -> column = decodeIntElement(descriptor, index)
                    6 -> rowSpan = decodeIntElement(descriptor, index)
                    7 -> columnSpan = decodeIntElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        return HomeItem.Widget(
            id = id,
            appWidgetId = appWidgetId,
            packageName = packageName,
            providerClassName = providerClassName,
            row = row,
            column = column,
            rowSpan = rowSpan,
            columnSpan = columnSpan
        )
    }
}