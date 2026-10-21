package com.nary.gym.data.local

import kotlinx.coroutines.flow.StateFlow

interface CurrentAccount {
    val accountId: StateFlow<String?>
}
