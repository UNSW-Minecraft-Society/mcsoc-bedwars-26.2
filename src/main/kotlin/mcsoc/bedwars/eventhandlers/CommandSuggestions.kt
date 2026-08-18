package mcsoc.bedwars.eventhandlers

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.commands.CommandSourceStack
import java.util.concurrent.CompletableFuture


object UpgradeItemsSuggestionProvider : SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        UpgradeItemType.entries.forEach { builder.suggest(it.name.lowercase()) }
        return builder.buildFuture()
    }
}