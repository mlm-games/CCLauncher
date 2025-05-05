package app.cclauncher.data

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.runtime.Immutable
import app.cclauncher.data.serializers.AppWidgetProviderInfoSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Immutable
sealed class HomeItem {
    abstract val id: String
    abstract val row: Int
    abstract val column: Int
    abstract val rowSpan: Int
    abstract val columnSpan: Int

    @Serializable
    data class App(
        val appModel: @Contextual AppModel,
        override val id: String = appModel.getKey(),
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int = 1,
        override val columnSpan: Int = 1,
    ) : HomeItem()

    @Serializable
    data class Widget(
        val appWidgetId: Int,
        @Serializable(with = AppWidgetProviderInfoSerializer::class)
        @Transient // Exclude from default serialization, handle manually if needed via serializer
        val providerInfo: AppWidgetProviderInfo? = null,
        val packageName: String,
        val providerClassName: String,
        override val id: String = "widget_$appWidgetId",
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int,
        override val columnSpan: Int,
    ) : HomeItem()
}

// Simple data class for storing the layout
@Serializable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val rows: Int = 8, // TODO: configurable?
    val columns: Int = 4 // TODO: configurable?
)