package mcsoc.bedwars.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration


inline val Int.ticks: Duration 
    get() = this.seconds / 20

inline val Long.ticks: Duration
    get() = this.seconds / 20

inline val Duration.inWholeTicks: Long
    get() = (inWholeMilliseconds / 50)