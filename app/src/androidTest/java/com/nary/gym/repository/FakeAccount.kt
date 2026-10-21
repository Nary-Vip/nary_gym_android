package com.nary.gym.repository

import com.nary.gym.data.local.CurrentAccount
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccount(id: String?) : CurrentAccount {
    override val accountId = MutableStateFlow(id)
}
