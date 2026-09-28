package com.nary.gym.viewModel

import com.nary.gym.data.remote.dto.UserResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileChangesTest {

    private val user = UserResponse(
        id = "u1", email = "a@b.com", phone = "9876543210", name = "Naresh", profile = null
    )

    @Test
    fun profileNotLoadedYet_returnsNull() {
        // Saving before get-user returns must not send blank fields that would wipe the profile.
        assertNull(profileChanges(current = null, name = "Naresh", phone = ""))
    }

    @Test
    fun nothingChanged_returnsNoChanges() {
        assertEquals(ProfileChanges(name = null, phone = null), profileChanges(user, "Naresh", "9876543210"))
    }

    @Test
    fun onlyNameChanged_sendsOnlyName() {
        assertEquals(ProfileChanges(name = "Naresh K", phone = null), profileChanges(user, "Naresh K", "9876543210"))
    }

    @Test
    fun onlyPhoneChanged_sendsOnlyPhone() {
        assertEquals(ProfileChanges(name = null, phone = "9000000000"), profileChanges(user, "Naresh", "9000000000"))
    }
}
