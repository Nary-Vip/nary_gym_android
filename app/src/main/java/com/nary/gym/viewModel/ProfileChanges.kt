package com.nary.gym.viewModel

import com.nary.gym.data.remote.dto.UserResponse

/** Fields to send in a profile update; null means unchanged. */
data class ProfileChanges(val name: String?, val phone: String?)

/**
 * What a profile save should send, or null if the profile hasn't loaded yet. Without the null
 * guard, saving before `auth/get-user` returns compared the edited fields against nothing and
 * sent blank values, wiping the user's name/phone on the server.
 */
fun profileChanges(current: UserResponse?, name: String, phone: String): ProfileChanges? {
    if (current == null) return null
    return ProfileChanges(
        name = name.takeIf { it != current.name },
        phone = phone.takeIf { it != current.phone },
    )
}
