package com.axelliant.hris.config

/** Rejects late responses even when a user signs back in with the same backend token. */
object SessionVersion {
    @Volatile var current: Long = 0L
        private set
    fun advance() { current++ }
}
