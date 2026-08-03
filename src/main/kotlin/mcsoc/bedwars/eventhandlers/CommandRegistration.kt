package mcsoc.bedwars.eventhandlers

import com.mojang.brigadier.arguments.StringArgumentType
import mcsoc.bedwars.datatrackers.GeneratorType
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.utils.format
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.phys.Vec3


const val ROOT_NODE = "bedwars"


fun registerCommands() {
    CommandRegistrationCallback.EVENT.register { dispatcher, buildContext, selection ->
        dispatcher.register(
            Commands.literal(ROOT_NODE)
                .requires { source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR) }

                // add and remove generators
                // use: /bedwars generator add <type> [<coords>]
                // use: /bedwars generator reemove <coords>
                .then(
                    Commands.literal("generator").then(
                        Commands.literal("add").then(
                            Commands.argument("type", StringArgumentType.word())
                                .suggests { _, builder ->
                                    GeneratorType.entries.forEach { type ->
                                        builder.suggest(type.name.lowercase())
                                    }
                                    builder.buildFuture()
                                }
                                .executes {
                                    val player = it.source.playerOrException
                                    val genArg = StringArgumentType.getString(it, "type")

                                    addGenerator(player, genArg, player.blockPosition())
                                }

                                .then(
                                    Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes {
                                            val player = it.source.playerOrException
                                            val genArg = StringArgumentType.getString(it, "type")
                                            val pos: BlockPos = BlockPosArgument.getBlockPos(it, "pos").above()

                                            addGenerator(player, genArg, pos)
                                        }
                                )
                        )
                    ).then(
                        Commands.literal("remove").then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes {
                                    val pos: BlockPos = BlockPosArgument.getBlockPos(it, "pos").above()
                                    ModDataTracker.removeGenerator(Vec3.atBottomCenterOf(pos))
                                    0
                                }
                        )
                    )
                )
        )
    }
}


private fun addGenerator(player: ServerPlayer, type: String, block: BlockPos): Int {
    val position = Vec3.atBottomCenterOf(block)
    
    try {
        val genType = enumValueOf<GeneratorType>(type.uppercase())
        ModDataTracker.addGenerator(genType, position, player)
    } catch (e: Exception) {
        player.sendSystemMessage(Component.literal("$type is not a valid generator"))
        e.printStackTrace()
        return 1
    }

    player.sendSystemMessage(Component.literal("added $type generator at ${position.format}"))
    return 0
}