package com.nary.gym.checkin

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPermissionsTest {

    @Test
    fun nothingGranted_requestsForeground() {
        assertEquals(
            LocationPermissionStep.REQUEST_FOREGROUND,
            nextLocationPermissionStep(sdkInt = 34, hasFine = false, hasCoarse = false, hasBackground = false)
        )
    }

    @Test
    fun coarseOnly_needsPrecise() {
        assertEquals(
            LocationPermissionStep.NEED_PRECISE,
            nextLocationPermissionStep(sdkInt = 34, hasFine = false, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineWithoutBackground_onApi29Plus_requestsBackground() {
        assertEquals(
            LocationPermissionStep.REQUEST_BACKGROUND,
            nextLocationPermissionStep(sdkInt = 29, hasFine = true, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineOnApi28_isEnough() {
        // Background location permission doesn't exist before API 29.
        assertEquals(
            LocationPermissionStep.GRANTED,
            nextLocationPermissionStep(sdkInt = 28, hasFine = true, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineAndBackground_isGranted() {
        assertEquals(
            LocationPermissionStep.GRANTED,
            nextLocationPermissionStep(sdkInt = 36, hasFine = true, hasCoarse = true, hasBackground = true)
        )
    }
}
