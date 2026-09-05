package mcsoc.bedwars.items

import mcsoc.bedwars.utils.addItemLore
import mcsoc.bedwars.utils.applyTag
import mcsoc.bedwars.utils.renameItem
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

const val BEDWARS_ITEM_TAG = "bedwars_item"
const val CUSTOM_ITEM_TAG = "bedwars_custom_item"

enum class CustomItemTypes(val value: String) {
    FIREBALL("fireball"),
    BRIDGE_EGG("bridge_egg"),
    INSTANT_TNT("instant_tnt"),
    BALL_OF_BUGS("ball_of_bugs"),
    POPUP_TOWER("popup_tower"),
    PLAYER_TRACKER("player_tracker")
}

object BedwarsItems {
    fun fireballItemStack(): ItemStack {
        val stack = Items.FIRE_CHARGE.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CustomItemTypes.FIREBALL.value)
        applyTag(stack, CUSTOM_ITEM_TAG, CustomItemTypes.FIREBALL.value)
        renameItem(stack, "Fireball")
        addItemLore(stack, "Right click to shoot a fireball in the direction you look.")
        return stack
    }

    fun bridgeEggItemStack(): ItemStack {
        val stack = Items.EGG.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CustomItemTypes.BRIDGE_EGG.value)
        applyTag(stack, CUSTOM_ITEM_TAG, CustomItemTypes.BRIDGE_EGG.value)
        renameItem(stack, "Bridge Egg")
        addItemLore(stack, "Right click to throw the egg, creating a bridge in it's wake.")
        return stack
    }

    fun primedTNTItemStack(): ItemStack {
        val stack = Items.TNT.defaultInstance
        applyTag(stack, BEDWARS_ITEM_TAG, CustomItemTypes.INSTANT_TNT.value)
        applyTag(stack, CUSTOM_ITEM_TAG, CustomItemTypes.INSTANT_TNT.value)
        renameItem(stack, "Instant TNT")
        addItemLore(stack, "Right click on a block to place down instantly primed TNT.")
        return stack
    }

    fun ballOfBugsItemStack(): ItemStack {
        val stack = Items.ENDER_PEARL.defaultInstance
        stack.remove(DataComponents.USE_COOLDOWN)
        applyTag(stack, BEDWARS_ITEM_TAG, CustomItemTypes.BALL_OF_BUGS.value)
        applyTag(stack, CUSTOM_ITEM_TAG, CustomItemTypes.BALL_OF_BUGS.value)
        renameItem(stack, "Ball of Bugs")
        addItemLore(stack, "Right click to throw a ball, spawning an endermite when it lands.")
        return stack
    }
}