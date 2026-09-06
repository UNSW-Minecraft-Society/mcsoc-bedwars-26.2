package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials

enum class Team(val chatColour: ChatFormatting, val trimMaterial: ResourceKey<TrimMaterial>, val dyeColour: DyeColor) {
    RED(ChatFormatting.RED, TrimMaterials.REDSTONE, DyeColor.RED),
    GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD, DyeColor.GREEN),
    BLUE(ChatFormatting.BLUE, TrimMaterials.LAPIS, DyeColor.BLUE),
    YELLOW(ChatFormatting.YELLOW, TrimMaterials.GOLD, DyeColor.YELLOW),

    CYAN(ChatFormatting.AQUA, TrimMaterials.DIAMOND, DyeColor.CYAN),
    MAGENTA(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, DyeColor.MAGENTA),
    ORANGE(ChatFormatting.GOLD, TrimMaterials.RESIN, DyeColor.ORANGE),
    PURPLE(ChatFormatting.DARK_PURPLE, TrimMaterials.AMETHYST, DyeColor.PURPLE),

    PINK(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST, DyeColor.PINK),
    BROWN(ChatFormatting.DARK_RED, TrimMaterials.RESIN, DyeColor.BROWN),
    LIGHT_GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD, DyeColor.LIME),
    LIGHT_BLUE(ChatFormatting.BLUE, TrimMaterials.DIAMOND, DyeColor.LIGHT_BLUE),
    GRAY(ChatFormatting.DARK_GRAY, TrimMaterials.IRON, DyeColor.GRAY),
    LIGHT_GRAY(ChatFormatting.GRAY, TrimMaterials.IRON, DyeColor.LIGHT_GRAY),
    WHITE(ChatFormatting.WHITE, TrimMaterials.QUARTZ, DyeColor.WHITE),
    BLACK(ChatFormatting.BLACK, TrimMaterials.NETHERITE, DyeColor.BLACK),

    NONE(ChatFormatting.GRAY, TrimMaterials.IRON, DyeColor.GRAY);

    fun getName() = name.lowercase()

    companion object {
        val CODEC: Codec<Team> = Codec.STRING.xmap(
            { Team.valueOf(it.uppercase()) },
            { it.name.lowercase() }
        )
    }
}