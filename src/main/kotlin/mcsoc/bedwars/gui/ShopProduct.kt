package mcsoc.bedwars.gui

import eu.pb4.sgui.api.ClickType
import eu.pb4.sgui.api.elements.GuiElement
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.level.Level

interface ShopProduct {
    fun getItemStack() : ItemStack
    fun getClickCallback() : GuiElement.ClickCallback
}

class ShopItem : ShopProduct {
    private val item_template: ItemStackTemplate
    private lateinit var item: ItemStack

    constructor(template: ItemStackTemplate) {
        this.item_template = template
    }
    constructor(item: Item, count: Int) : this(ItemStackTemplate(item, count))

    private fun resolveItemStackTemplate(): ItemStack {
        if (!::item.isInitialized) this.item = item_template.create()
        return item
    }
    
    override fun getItemStack(): ItemStack {
        return resolveItemStackTemplate()
    }

    override fun getClickCallback(): GuiElement.ClickCallback {
        return GuiElement.ClickCallback { index, clickType, action, gui ->
            val player = gui.player
            val purchase = getItemStack()
            if (clickType == ClickType.MOUSE_LEFT) {
                player?.addItem(purchase)
                player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value())
                player?.sendSystemMessage(Component.literal("Bought $purchase"), false)
            }
        }
    }
}