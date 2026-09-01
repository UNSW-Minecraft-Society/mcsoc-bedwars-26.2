package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.TieredGenerator
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

internal interface GeneratorsExposer {
    fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel): Int
    fun addGenerator(location: Vec3, level: ServerLevel, team: Team): Int
    fun upgradeTeamGenerator(team: Team)
    fun removeGenerator(location: Vec3)
    fun removeGenerator(id: Int)
    fun upgradeGeneratorTier(type: GeneratorType)
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun getGenerators(type: GeneratorType): List<Generator>
    fun addGenerator(gen: Generator, type: GeneratorType)
    fun removeGenerator(gen: Generator, type: GeneratorType)

    override fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel): Int {
        val gen = GeneratorFactory.createGenerator(type.getConfig(), location, level)
        addGenerator(gen, type)
        return gen.id
    }

    override fun removeGenerator(location: Vec3) {
        GeneratorType.entries.forEach { type ->
            getGenerators(type)
                .filter { it.place.location == location }
                .forEach { removeGenerator(it, type) }
        }
    }

    override fun removeGenerator(id: Int) {
        GeneratorType.entries.forEach { type ->
            getGenerators(type)
                .filter { it.id == id }
                .forEach { removeGenerator(it, type) }
        }
    }

    override fun upgradeGeneratorTier(type: GeneratorType) {
        getGenerators(type)
            .filterIsInstance<TieredGenerator>()
            .forEach(TieredGenerator::upgrade)
    }
}
