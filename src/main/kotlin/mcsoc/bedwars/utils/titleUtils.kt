package mcsoc.bedwars.utils

import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.level.ServerPlayer

fun ServerPlayer.sendTitle(text: Component) {
    this.connection.send(ClientboundClearTitlesPacket(true))
    this.connection.send(ClientboundSetTitlesAnimationPacket(0, 100, 0))
    this.connection.send(ClientboundSetTitleTextPacket(text))
}

fun ServerPlayer.sendTitle(text: Component, subtitle: Component) {
    this.sendTitle(text)
    this.connection.send(ClientboundSetSubtitleTextPacket(subtitle))
}
