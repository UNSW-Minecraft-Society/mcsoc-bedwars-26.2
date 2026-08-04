package mcsoc.bedwars.utils

import com.mojang.serialization.Codec

enum class Colour {
    RED,
    BLUE,
    GREEN,
    YELLOW,
        
    ORANGE,
    PINK,
    BROWN,
    LIGHT_GREEN,
        
    LIGHT_BLUE,
    MAGENTA,
    CYAN,
    PURPLE,
    GRAY,
    LIGHT_GRAY,
    WHITE,
    BLACK,
    NONE;

    companion object {
        val CODEC: Codec<Colour> = Codec.STRING.xmap(
            { Colour.valueOf(it.uppercase()) },
            { it.name.lowercase() }
        )
    }
}