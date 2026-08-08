package mcsoc.bedwars.utils

import com.mojang.serialization.Codec
import net.minecraft.ChatFormatting
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials

enum class Team(val chatColour: ChatFormatting, val trimMaterial: ResourceKey<TrimMaterial>) {
    RED(ChatFormatting.RED, TrimMaterials.REDSTONE),
    GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD),
    BLUE(ChatFormatting.BLUE, TrimMaterials.LAPIS),
    YELLOW(ChatFormatting.YELLOW, TrimMaterials.GOLD),
    
    CYAN(ChatFormatting.AQUA, TrimMaterials.DIAMOND),
    MAGENTA(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST),
    ORANGE(ChatFormatting.GOLD, TrimMaterials.RESIN),
    PURPLE(ChatFormatting.DARK_PURPLE, TrimMaterials.AMETHYST),
    
    PINK(ChatFormatting.LIGHT_PURPLE, TrimMaterials.AMETHYST),
    BROWN(ChatFormatting.DARK_RED, TrimMaterials.RESIN),
    LIGHT_GREEN(ChatFormatting.GREEN, TrimMaterials.EMERALD),
    LIGHT_BLUE(ChatFormatting.BLUE, TrimMaterials.DIAMOND),
    GRAY(ChatFormatting.DARK_GRAY, TrimMaterials.IRON),
    LIGHT_GRAY(ChatFormatting.GRAY, TrimMaterials.IRON),
    WHITE(ChatFormatting.WHITE, TrimMaterials.QUARTZ),
    BLACK(ChatFormatting.BLACK, TrimMaterials.NETHERITE),
    
    NONE(ChatFormatting.GRAY, TrimMaterials.IRON);
    
    companion object {
        val CODEC: Codec<Team> = Codec.STRING.xmap(
            { Team.valueOf(it.uppercase()) },
            { it.name.lowercase() }
        )
    }
}