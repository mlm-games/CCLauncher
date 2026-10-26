package app.cclauncher.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Immutable
sealed class HomeItem {
    abstract val id: String
    abstract val page: Int
    abstract val row: Int
    abstract val column: Int
    abstract val rowSpan: Int
    abstract val columnSpan: Int

    @Serializable(with = HomeItemAppSerializer::class)
    data class App(
        val appModel: AppModel,
        override val id: String = appModel.getKey(),
        override val page: Int = 0,
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
        override val page: Int = 0,
        override val row: Int,
        override val column: Int,
        override val rowSpan: Int,
        override val columnSpan: Int,
    ) : HomeItem() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Widget) return false
            return id == other.id &&
                page == other.page &&
                appWidgetId == other.appWidgetId &&
                packageName == other.packageName &&
                providerClassName == other.providerClassName &&
                row == other.row &&
                column == other.column &&
                rowSpan == other.rowSpan &&
                columnSpan == other.columnSpan
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + page
            result = 31 * result + appWidgetId
            result = 31 * result + packageName.hashCode()
            result = 31 * result + providerClassName.hashCode()
            result = 31 * result + row
            result = 31 * result + column
            result = 31 * result + rowSpan
            result = 31 * result + columnSpan
            return result
        }
    }
}

@Serializable
data class HomeLayout(
    val items: List<HomeItem> = emptyList(),
    val rows: Int = 8,
    val columns: Int = 4,
    val pageCount: Int = 1
) {
    fun itemsForPage(page: Int): List<HomeItem> = items.filter { it.page == page }

    fun isPageEmpty(page: Int): Boolean = items.none { it.page == page }

    fun lastNonEmptyPage(): Int = items.maxOfOrNull { it.page } ?: 0
}