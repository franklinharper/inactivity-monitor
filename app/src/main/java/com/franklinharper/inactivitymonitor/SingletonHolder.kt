package com.franklinharper.inactivitymonitor

open class SingletonHolder<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    // The @Volatile annotation below is required for the locking to work properly
    @Volatile private var instance: T? = null

    // For details see
    // https://medium.com/@BladeCoder/kotlin-singletons-with-argument-194ef06edd9e
    fun from(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}