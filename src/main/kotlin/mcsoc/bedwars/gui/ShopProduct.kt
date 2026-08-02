package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

interface ShopProduct {
    fun getItemStack() : ItemStack
    fun getClickCallback() : GuiElement.ClickCallback
}

class ShopItem : ShopProduct {
    private val itemStack: ItemStack

    constructor(itemStack: ItemStack) {
        this.itemStack = itemStack
    }
    constructor(item: Item, count: Int) : this(ItemStack(item, count))

    override fun getItemStack(): ItemStack {
        return itemStack
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player
            if (clickType == ClickType.MOUSE_LEFT) {
                player?.addItem(itemStack)
                player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value())
                player?.sendSystemMessage(Component.literal("Bought $itemStack"), false)
            }
        }
    }
}