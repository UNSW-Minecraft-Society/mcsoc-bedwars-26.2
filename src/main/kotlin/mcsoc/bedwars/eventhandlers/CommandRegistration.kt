package mcsoc.bedwars.eventhandlers

import mcsoc.bedwars.gui.ShopGui
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
        dispatcher.register(Commands.literal(ROOT_NODE)
        .then(Commands.literal("open_shop_gui").executes(ShopGui::displayShop))
            .then(Commands.literal("test_simple_gui").executes(ShopGui::testSimpleGui))
            .then(Commands.literal("test_simple_gui_4").executes(ShopGui::testSimpleGui4))
        )
    }
}