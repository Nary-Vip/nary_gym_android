package com.nary.gym.navigation

import com.nary.gym.checkin.CheckInNotificationHelper

const val SOURCE_REMINDER = "notification_yes"

sealed interface LaunchTarget {
    data object None : LaunchTarget
    data object Profile : LaunchTarget
    data class OpenTab(val tab: Tab) : LaunchTarget
}

fun launchTarget(source: String?, dataUri: String?, dataPath: String?): LaunchTarget = when {
    source == SOURCE_REMINDER -> LaunchTarget.OpenTab(Tab.TODAY)
    source == CheckInNotificationHelper.SOURCE_CHECK_IN -> LaunchTarget.OpenTab(Tab.CHECK_INS)
    dataUri == "narygym://profile" || dataPath == "/profile" -> LaunchTarget.Profile
    else -> LaunchTarget.None
}
