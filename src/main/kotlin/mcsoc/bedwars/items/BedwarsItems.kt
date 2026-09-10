package mcsoc.bedwars.items

import mcsoc.bedwars.utils.withItemLore
import mcsoc.bedwars.utils.withTag
import mcsoc.bedwars.utils.renamedTo
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items


const val BEDWARS_ITEM_TAG = "bedwars_item"
const val CUSTOM_ITEM_TAG = "bedwars_custom_item"

enum class CustomItemTypes(val value: String, private val item_factory: () -> ItemStack) {
    FIREBALL("fireball", {BedwarsItems.fireballItemStack()}),
    BRIDGE_EGG("bridge_egg", {BedwarsItems.bridgeEggItemStack()}),
    INSTANT_TNT("instant_tnt", {BedwarsItems.instantTNTItemStack()}),
    BALL_OF_BUGS("ball_of_bugs", {BedwarsItems.ballOfBugsItemStack()}),
    POPUP_TOWER("popup_tower", {BedwarsItems.popupTowerItemStack()}),
    PLAYER_TRACKER("player_tracker", {BedwarsItems.playerTrackerItemStack()});
    
    fun giveToPlayer(player: ServerPlayer?): Int {
        return if (player is ServerPlayer && player.addItem(item_factory())) 1
        else 0
    }
}

object BedwarsItems {
    fun fireballItemStack(): ItemStack {
        return Items.FIRE_CHARGE.defaultInstance
        .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.FIREBALL.value)
        .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.FIREBALL.value)
        .renamedTo("Fireball")
        .withItemLore("Right click to shoot a fireball in the direction you look.")
    }

    fun bridgeEggItemStack(): ItemStack {
        return Items.EGG.defaultInstance
            .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.BRIDGE_EGG.value)
            .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.BRIDGE_EGG.value)
            .renamedTo("Bridge Egg")
            .withItemLore("Right click to throw the egg, creating a bridge in it's wake.")
    }

    fun instantTNTItemStack(): ItemStack {
        return Items.TNT.defaultInstance
            .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.INSTANT_TNT.value)
            .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.INSTANT_TNT.value)
            .renamedTo("Instant TNT")
            .withItemLore("Right click on a block to place down instantly primed TNT.")
    }

    fun ballOfBugsItemStack(): ItemStack {
        return Items.ENDER_PEARL.defaultInstance
            .also{it.remove(DataComponents.USE_COOLDOWN)}
            .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.BALL_OF_BUGS.value)
            .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.BALL_OF_BUGS.value)
            .renamedTo("Ball of Bugs")
            .withItemLore("Right click to throw a ball, spawning an endermite when it lands.")
    }

    fun popupTowerItemStack(): ItemStack {
        return Items.REINFORCED_DEEPSLATE.defaultInstance
            .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.POPUP_TOWER.value)
            .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.POPUP_TOWER.value)
            .renamedTo("Popup Tower")
            .withItemLore("Right click on a block to instantly create a tower structure.")
    }

    fun playerTrackerItemStack(): ItemStack {
        return Items.COMPASS.defaultInstance
            .withTag(BEDWARS_ITEM_TAG, CustomItemTypes.PLAYER_TRACKER.value)
            .withTag(CUSTOM_ITEM_TAG, CustomItemTypes.PLAYER_TRACKER.value)
            .renamedTo("Player Tracker")
            .withItemLore("Points to where the nearest player on an enemy team was, Right click to update the location.")
    }
}