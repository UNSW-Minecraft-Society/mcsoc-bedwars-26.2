package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.generators.DefaultGeneratorTypes
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.TieredGenerator
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

internal interface GeneratorsExposer {
    fun addGenerator(type: String, location: Vec3): Boolean
    fun removeGenerator(location: Vec3)
    fun upgradeGeneratorTier()

    fun tickGenerators(level: ServerLevel)
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)

    override fun addGenerator(type: String, location: Vec3): Boolean {
        val config = DefaultGeneratorTypes.generators[type] ?: return false
        addGenerator(GeneratorFactory.createGenerator(config, location))
        return true
    }

    override fun removeGenerator(location: Vec3) {
        getGenerators()
            .filter { it.location == location }
            .forEach { removeGenerator(it) }
    }

    override fun upgradeGeneratorTier() {
        getGenerators()
            .filterIsInstance<TieredGenerator>()
            .forEach(TieredGenerator::upgrade)
    }

    override fun tickGenerators(level: ServerLevel) {
        getGenerators().forEach { it.tick(level) }
    }
}
