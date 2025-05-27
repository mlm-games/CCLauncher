@file:Suppress("unused")

package app.cclauncher.data

object Constants {

    const val REQUEST_CODE_BIND_WIDGET = 102

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
    }

    object Dialog {
        const val ABOUT = "ABOUT"
    }

    object UserState {
        const val START = "START"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
    }

//    object DateTime {
//        const val OFF = 0
//        const val ON = 1
//        const val DATE_ONLY = 2
//
//        fun isTimeVisible(dateTimeVisibility: Int): Boolean {
//            return dateTimeVisibility == ON
//        }
//
//        fun isDateVisible(dateTimeVisibility: Int): Boolean {
//            return dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
//        }
//    }

    object SwipeAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
        const val APP = 3
        const val NULL = 0
    }

    object GridSize {
        const val MIN_ROWS = 4
        const val MAX_ROWS = 12
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 8
        const val DEFAULT_ROWS = 8
        const val DEFAULT_COLUMNS = 4
    }


    object TextSize {
        const val ONE = 0.6f
        const val TWO = 0.75f
        const val THREE = 0.9f
        const val FOUR = 1f
        const val FIVE = 1.15f
        const val SIX = 1.3f
        const val SEVEN = 1.45f
    }


    object HomeAppCount {
        const val NUM = 16
    }

    const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_HOME_APP_1 = 1
    const val FLAG_SET_HOME_APP_2 = 2
    const val FLAG_SET_HOME_APP_3 = 3
    const val FLAG_SET_HOME_APP_4 = 4
    const val FLAG_SET_HOME_APP_5 = 5
    const val FLAG_SET_HOME_APP_6 = 6
    const val FLAG_SET_HOME_APP_7 = 7
    const val FLAG_SET_HOME_APP_8 = 8
    const val FLAG_SET_HOME_APP_9 = 9
    const val FLAG_SET_HOME_APP_10 = 10
    const val FLAG_SET_HOME_APP_11 = 11
    const val FLAG_SET_HOME_APP_12 = 12
    const val FLAG_SET_HOME_APP_13 = 13
    const val FLAG_SET_HOME_APP_14 = 14
    const val FLAG_SET_HOME_APP_15 = 15
    const val FLAG_SET_HOME_APP_16 = 16

    const val FLAG_SET_SWIPE_LEFT_APP = 17
    const val FLAG_SET_SWIPE_RIGHT_APP = 18
    const val FLAG_SET_SWIPE_UP_APP = 19
    const val FLAG_SET_SWIPE_DOWN_APP = 20

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val HINT_RATE_US = 25

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 10f

    const val URL_ABOUT_CCLAUNCHER = "https://github.com/mlm-games/CCLauncher"
    const val URL_CCLAUNCHER_PRIVACY = "https://github.com/mlm-games/CCLauncher"
    const val URL_DOUBLE_TAP = ""
    const val URL_CCLAUNCHER_GITHUB = "https://github.com/mlm-games/CCLauncher"
    const val URL_DUCK_SEARCH = "https://duckduckgo.com?q="

    // Need to add the comments below as tooltips when long-tap ig?
    object SearchType {
        const val CONTAINS = 0      // Default substring search
        const val FUZZY = 1         // Fuzzy search (characters in order anywhere)
        const val STARTS_WITH = 2   // Only match beginning of app names
    }
}

object Navigation {
    const val HOME = "home"
    const val APP_DRAWER = "app_drawer"
    const val SETTINGS = "settings"
    const val HIDDEN_APPS = "hidden_apps"
    const val WIDGET_PICKER = "widget_picker"
}


