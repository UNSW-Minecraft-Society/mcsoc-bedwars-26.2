package mcsoc.bedwars.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


inline val Int.ticks: Duration 
    get() = toDuration(DurationUnit.SECONDS) * 20

inline val Duration.inWholeTicks: Duration
    get() = ((inWholeMilliseconds / 50).toInt() * 50).toDuration(DurationUnit.MILLISECONDS)