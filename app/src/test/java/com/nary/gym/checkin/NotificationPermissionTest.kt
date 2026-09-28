package com.nary.gym.checkin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionTest {

    @Test
    fun api33_notGranted_needsRequest() {
        assertTrue(needsNotificationPermission(sdkInt = 33, granted = false))
    }

    @Test
    fun api33_granted_doesNotNeedRequest() {
        assertFalse(needsNotificationPermission(sdkInt = 35, granted = true))
    }

    @Test
    fun belowApi33_neverNeedsRequest() {
        // POST_NOTIFICATIONS doesn't exist before Android 13; notifications are allowed by default.
        assertFalse(needsNotificationPermission(sdkInt = 32, granted = false))
    }
}
