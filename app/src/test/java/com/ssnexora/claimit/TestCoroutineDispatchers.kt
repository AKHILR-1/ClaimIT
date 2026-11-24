package com.ssnexora.claimit

import com.ssnexora.claimit.core.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class TestCoroutineDispatchers : CoroutineDispatchers {
    override val io: CoroutineDispatcher get() = Dispatchers.Unconfined
    override val default: CoroutineDispatcher get() = Dispatchers.Unconfined
    override val main: CoroutineDispatcher get() = Dispatchers.Unconfined
    override val unconfined: CoroutineDispatcher get() = Dispatchers.Unconfined
}

