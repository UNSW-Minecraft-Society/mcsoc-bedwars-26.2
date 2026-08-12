package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials

enum class Team(val chatColour: ChatFormatting, val trimMaterial: ResourceKey<TrimMaterial>, val dyeColour: Int) {
    RED(ChatFormatting.RED, TrimMaterials.REDSTONE, 14),
    GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD, 13),
    BLUE(ChatFormatting.BLUE, TrimMaterials.LAPIS, 11),
    YELLOW(ChatFormatting.YELLOW, TrimMaterials.GOLD, 4),

    CYAN(ChatFormatting.AQUA, TrimMaterials.DIAMOND, 9),
    MAGENTA(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, 2),
    ORANGE(ChatFormatting.GOLD, TrimMaterials.RESIN, 1),
    PURPLE(ChatFormatting.DARK_PURPLE, TrimMaterials.AMETHYST, 10),

    PINK(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, 6),
    BROWN(ChatFormatting.DARK_RED, TrimMaterials.RESIN, 12),
    LIGHT_GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD, 5),
    LIGHT_BLUE(ChatFormatting.BLUE, TrimMaterials.DIAMOND, 3),
    GRAY(ChatFormatting.DARK_GRAY, TrimMaterials.IRON, 7),
    LIGHT_GRAY(ChatFormatting.GRAY, TrimMaterials.IRON, 8),
    WHITE(ChatFormatting.WHITE, TrimMaterials.QUARTZ, 0),
    BLACK(ChatFormatting.BLACK, TrimMaterials.NETHERITE, 15),

    NONE(ChatFormatting.GRAY, TrimMaterials.IRON, 7);

    fun getName() = name.lowercase()

    companion object {
        val CODEC: Codec<Team> = Codec.STRING.xmap(
            { Team.valueOf(it.uppercase()) },
            { it.name.lowercase() }
        )
    }
}