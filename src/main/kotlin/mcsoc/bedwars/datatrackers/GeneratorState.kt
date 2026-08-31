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
    fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel, team: Team): Int
    fun upgradeTeamGenerators(team: Team)
    fun removeGenerator(location: Vec3)
    fun removeGenerator(id: Int)
    fun upgradeGeneratorTier()
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)

    override fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel): Int {
        val gen = GeneratorFactory.createGenerator(type.getConfig(), location, level)
        addGenerator(gen)
        return gen.id
    }

    override fun removeGenerator(location: Vec3) {
        getGenerators()
            .filter { it.place.location == location }
            .forEach { removeGenerator(it) }
    }

    override fun removeGenerator(id: Int) {
        getGenerators()
            .filter { it.id == id }
            .forEach { removeGenerator(it) }
    }

    override fun upgradeGeneratorTier() {
        getGenerators()
            .filterIsInstance<TieredGenerator>()
            .forEach(TieredGenerator::upgrade)
    }
}
