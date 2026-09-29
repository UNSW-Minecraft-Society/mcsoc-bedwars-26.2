package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials
import net.minecraft.world.scores.TeamColor

enum class Team(val chatColour: ChatFormatting, val trimMaterial: ResourceKey<TrimMaterial>, val dyeColour: DyeColor, val teamColour: TeamColor) {
    RED(ChatFormatting.RED, TrimMaterials.REDSTONE, DyeColor.RED, TeamColor.RED),
    GREEN(ChatFormatting.DARK_GREEN, TrimMaterials.EMERALD, DyeColor.GREEN, TeamColor.DARK_GREEN),
    BLUE(ChatFormatting.DARK_BLUE, TrimMaterials.LAPIS, DyeColor.BLUE, TeamColor.DARK_BLUE),
    YELLOW(ChatFormatting.YELLOW, TrimMaterials.GOLD, DyeColor.YELLOW, TeamColor.YELLOW),

    CYAN(ChatFormatting.AQUA, TrimMaterials.DIAMOND, DyeColor.CYAN, TeamColor.AQUA),
    MAGENTA(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, DyeColor.MAGENTA, TeamColor.LIGHT_PURPLE),
    ORANGE(ChatFormatting.GOLD, TrimMaterials.RESIN, DyeColor.ORANGE, TeamColor.GOLD),
    PURPLE(ChatFormatting.DARK_PURPLE, TrimMaterials.AMETHYST, DyeColor.PURPLE, TeamColor.DARK_PURPLE),

    PINK(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, DyeColor.PINK, TeamColor.LIGHT_PURPLE),
    BROWN(ChatFormatting.DARK_RED, TrimMaterials.RESIN, DyeColor.BROWN, TeamColor.DARK_RED),
    LIGHT_GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD, DyeColor.LIME, TeamColor.GREEN),
    LIGHT_BLUE(ChatFormatting.BLUE, TrimMaterials.DIAMOND, DyeColor.LIGHT_BLUE, TeamColor.BLUE),
    GRAY(ChatFormatting.DARK_GRAY, TrimMaterials.IRON, DyeColor.GRAY, TeamColor.DARK_GRAY),
    LIGHT_GRAY(ChatFormatting.GRAY, TrimMaterials.IRON, DyeColor.LIGHT_GRAY, TeamColor.GRAY),
    WHITE(ChatFormatting.WHITE, TrimMaterials.QUARTZ, DyeColor.WHITE, TeamColor.WHITE),
    BLACK(ChatFormatting.BLACK, TrimMaterials.NETHERITE, DyeColor.BLACK, TeamColor.BLACK),

    NONE(ChatFormatting.GRAY, TrimMaterials.NETHERITE, DyeColor.WHITE, TeamColor.GRAY);

    fun getName() = name.lowercase()

    companion object {
        val CODEC: Codec<Team> = Codec.STRING.xmap(
            { Team.valueOf(it.uppercase()) },
            { it.name.lowercase() }
        )
    }
}