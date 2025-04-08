package app.cclauncher.data

import androidx.appcompat.app.AppCompatDelegate

object Constants {

    object Key {
        const val FLAG = "flag"
        const val RENAME = "rename"
    }

    object Dialog {
        const val ABOUT = "ABOUT"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
        const val HIDDEN = "HIDDEN"
        const val KEYBOARD = "KEYBOARD"
        const val DIGITAL_WELLBEING = "DIGITAL_WELLBEING"
    }

    object UserState {
        const val START = "START"
        const val REVIEW = "REVIEW"
        const val RATE = "RATE"
        const val SHARE = "SHARE"
    }

    object DateTime {
        const val OFF = 0
        const val ON = 1
        const val DATE_ONLY = 2

        fun isTimeVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON
        }

        fun isDateVisible(dateTimeVisibility: Int): Boolean {
            return dateTimeVisibility == ON || dateTimeVisibility == DATE_ONLY
        }
    }

    object SwipeDownAction {
        const val SEARCH = 1
        const val NOTIFICATIONS = 2
    }

    object TextSize {
        const val ONE = 0.6f
        const val TWO = 0.75f
        const val THREE = 0.9f
        const val FOUR = 1f
        const val FIVE = 1.1f
        const val SIX = 1.2f
        const val SEVEN = 1.3f
    }

    object CharacterIndicator{
        const val SHOW = 102
        const val HIDE = 101
    }

    const val WALL_TYPE_LIGHT = "light"
    const val WALL_TYPE_DARK = "dark"

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

    const val FLAG_SET_SWIPE_LEFT_APP = 11
    const val FLAG_SET_SWIPE_RIGHT_APP = 12
    const val FLAG_SET_CLOCK_APP = 13
    const val FLAG_SET_CALENDAR_APP = 14

    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val REQUEST_CODE_LAUNCHER_SELECTOR = 678

    const val HINT_RATE_US = 15

    const val LONG_PRESS_DELAY_MS = 500L
    const val ONE_DAY_IN_MILLIS = 86400000L
    const val ONE_HOUR_IN_MILLIS = 3600000L
    const val ONE_MINUTE_IN_MILLIS = 60000L

    const val MIN_ANIM_REFRESH_RATE = 10f

    const val URL_ABOUT_CCLAUNCHER = "https://github.com/mlm-games/CCLauncher"
    const val URL_CCLAUNCHER_PRIVACY = "https://github.com/mlm-games/CCLauncher"
    const val URL_DOUBLE_TAP = ""
    const val URL_CCLAUNCHER_GITHUB = "https://github.com/mlm-games/CCLauncher"
    const val URL_DUCK_SEARCH = ""
    const val WALLPAPER_WORKER_NAME = "WALLPAPER_WORKER_NAME"
}

object Navigation {
    const val HOME = "home"
    const val APP_DRAWER = "app_drawer"
    const val SETTINGS = "settings"
    const val HIDDEN_APPS = "hidden_apps"
    const val WIDGET_PICKER = "widget_picker"
    const val WIDGET_MANAGER = "widget_manager"
    const val WIDGET_CONFIG = "widget_config"
    const val WIDGET_EDIT = "widget_edit"
}
