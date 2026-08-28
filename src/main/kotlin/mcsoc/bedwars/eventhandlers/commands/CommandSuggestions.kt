package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.maploader.structures_directory
import mcsoc.bedwars.upgrades.UpgradeItemType
import net.minecraft.commands.CommandSourceStack
import java.util.concurrent.CompletableFuture
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


internal class LoadedMapSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
		for (map in BedwarsConfigData.map_data.keys) {
		    builder.suggest(map)
        }
        
		return builder.buildFuture()
	}
}

internal class AvailableStructureSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
		for (schematic in structures_directory.listDirectoryEntries()) {
		    builder.suggest(schematic.name)
        }
		return builder.buildFuture()
	}
}

internal class ExampleSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
		builder.suggest(ctx.source.textName)
		return builder.buildFuture()
	}
}

internal class UpgradeItemsSuggestionProvider : SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        for (type in UpgradeItemType.entries) {
        	builder.suggest(type.name.lowercase())
        }
        return builder.buildFuture()
    }
}