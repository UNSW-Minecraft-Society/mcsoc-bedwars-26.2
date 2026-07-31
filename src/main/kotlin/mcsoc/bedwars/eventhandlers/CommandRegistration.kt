package mcsoc.bedwars.eventhandlers

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register{dispatcher, buildContext, selection ->
        dispatcher.register(Commands.literal(ROOT_NODE)
        
        )
    }
}