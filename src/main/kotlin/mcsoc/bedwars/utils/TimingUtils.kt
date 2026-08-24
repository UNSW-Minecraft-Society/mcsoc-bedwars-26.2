package mcsoc.bedwars.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


inline val Int.ticks: Duration 
    get() = toDuration(DurationUnit.SECONDS) * 20