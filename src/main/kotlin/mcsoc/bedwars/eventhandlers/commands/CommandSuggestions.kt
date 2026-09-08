package mcsoc.bedwars.eventhandlers.commands

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import mcsoc.bedwars.entities.CustomEntityType
import mcsoc.bedwars.gui.ShopType
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.datatrackers.ModDataTracker
import mcsoc.bedwars.datatrackers.configloader.BedwarsConfigData
import mcsoc.bedwars.datatrackers.configloader.maploader.structures_directory
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.generators.GeneratorType
import net.minecraft.commands.CommandSourceStack
import java.util.concurrent.CompletableFuture
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


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

internal class LoadedMapSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
		for (map in BedwarsConfigData.map_data.keys) {
		    builder.suggest(map)
        }
        
		return builder.buildFuture()
	}
}

internal class UpgradeItemsSuggestionProvider : SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        UpgradeItemType.entries.forEach { builder.suggest(it.name.lowercase()) }
        return builder.buildFuture()
    }
}

internal class GeneratorSuggestionProvider: SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        GeneratorType::class.sealedSubclasses
        	.mapNotNull{it.objectInstance}
        	.forEach{builder.suggest(it.id.lowercase())}
		return builder.buildFuture()
    }
}

internal class TeamSuggestionProvider: SuggestionProvider<CommandSourceStack> {
    override fun getSuggestions(context: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        context.source.level.gameState.getActiveTeams().forEach { builder.suggest(it.getName()) }
		return builder.buildFuture()
    }
}


internal class EntityTypeSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		context: CommandContext<CommandSourceStack>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions?>? {
		CustomEntityType.entries.forEach { builder.suggest(it.name.lowercase()) }
		return builder.buildFuture()
	}
}

internal class ShopTypeSuggestionProvider: SuggestionProvider<CommandSourceStack> {
	override fun getSuggestions(
		context: CommandContext<CommandSourceStack?>,
		builder: SuggestionsBuilder
	): CompletableFuture<Suggestions> {
		ShopType.entries.forEach { builder.suggest(it.name.lowercase()) }
		return builder.buildFuture()
	}
}