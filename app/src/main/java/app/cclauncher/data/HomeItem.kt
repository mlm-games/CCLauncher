package app.cclauncher.data

import androidx.compose.runtime.Immutable
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

    @Serializable(with = HomeItemAppSerializer::class)
    data class App(
        val appModel: AppModel,
        override val id: String = appModel.getKey(),
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int = 1,
        override val columnSpan: Int = 1,
    ) : HomeItem()

    @Serializable(with = HomeItemWidgetSerializer::class)
    data class Widget(
        val appWidgetId: Int,
        @Transient
        val providerInfo: android.appwidget.AppWidgetProviderInfo? = null,
        val packageName: String,
        val providerClassName: String,
        override val id: String = "widget_$appWidgetId",
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int,
        override val columnSpan: Int,
    ) : HomeItem()
}

@Serializable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val rows: Int = 8, // Will be overridden
    val columns: Int = 4
)

