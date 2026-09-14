package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration


inline val Int.ticks: Duration 
    get() = this.seconds / 20

inline val Long.ticks: Duration
    get() = this.seconds / 20

inline val Duration.inWholeTicks: Long
    get() = (inWholeMilliseconds / 50)

val INSTANT_CODEC: Codec<Instant> = Codec.LONG.xmap(Instant::fromEpochMilliseconds, Instant::toEpochMilliseconds)