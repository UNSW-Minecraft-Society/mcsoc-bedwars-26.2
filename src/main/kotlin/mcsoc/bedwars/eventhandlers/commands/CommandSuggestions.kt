package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.generators.DefaultGeneratorTypes
import net.minecraft.commands.CommandSourceStack
import java.util.concurrent.CompletableFuture


internal class ExampleSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
		builder.suggest(ctx.source.textName)
		return builder.buildFuture()
	}
}

internal class GeneratorSuggestionProvider: SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        DefaultGeneratorTypes.getCurrentGenerators().forEach { builder.suggest(it) }
		return builder.buildFuture()
    }
}

internal class TeamSuggestionProvider: SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        ModDataTracker.getActiveTeams().forEach { builder.suggest(it.getName()) }
		return builder.buildFuture()
    }
}
