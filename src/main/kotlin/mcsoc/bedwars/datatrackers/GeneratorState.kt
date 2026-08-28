package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.generators.DefaultGeneratorTypes
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.TieredGenerator
import mcsoc.bedwars.utils.Team
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

internal interface GeneratorsExposer {
    fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>): Boolean
    fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>, team: Team): Boolean
    fun upgradeTeamGenerators(team: Team)
    fun removeGenerator(location: Vec3)
    fun upgradeGeneratorTier()
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)

    override fun addGenerator(type: String, location: Vec3, dim: ResourceKey<Level>): Boolean {
        val config = DefaultGeneratorTypes.generators[type] ?: return false
        addGenerator(GeneratorFactory.createGenerator(config, location, dim))
        return true
    }

    override fun removeGenerator(location: Vec3) {
        getGenerators()
            .filter { it.place.location == location }
            .forEach { removeGenerator(it) }
    }

    override fun upgradeGeneratorTier() {
        getGenerators()
            .filterIsInstance<TieredGenerator>()
            .forEach(TieredGenerator::upgrade)
    }
}
