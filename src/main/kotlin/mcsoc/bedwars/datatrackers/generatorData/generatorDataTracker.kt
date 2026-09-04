package mcsoc.bedwars.datatrackers.generatorData

import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.generators.BaseGenerator
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

private class GeneratorDataStore: GeneratorsHolder {
    private val generators = HashMap<GeneratorType, MutableList<Generator>>()
    
    override fun addGenerator(gen: Generator, type: GeneratorType) {
        generators.getOrPut(type) { mutableListOf<Generator>() }.add(gen)
    }
    
    override fun addGenerator(location: Vec3, level: ServerLevel, team: Team): Int {
        val teamGenType = GeneratorType.BASE
        val gen = GeneratorFactory.createGenerator(teamGenType.getConfig(), location, level)
        if (gen !is BaseGenerator) throw Exception("generator is not base generator")
        addGenerator(gen, teamGenType)
        level.gameState.setGenerator(team, gen)
        return gen.id
    }
    
    override fun getGenerators(): List<Generator> = generators.flatMap { it.value }
    override fun getGenerators(type: GeneratorType) = generators.getOrPut(type) { mutableListOf<Generator>() }

    override fun removeGenerator(gen: Generator, type: GeneratorType) {
        gen.remove()
        generators[type]?.remove(gen)
    }
    
    override fun upgradeTeamGenerator(team: Team, level: ServerLevel) {
        level.gameState.upgradeGenerator(team)
    }

}

class GeneratorDataTracker: GeneratorsExposer {
    private val generator_data = GeneratorDataStore()
    
    override fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel) = generator_data.addGenerator(type, location, level)
    override fun addGenerator(location: Vec3, level: ServerLevel, team: Team) = generator_data.addGenerator(location, level, team)
    override fun removeGenerator(location: Vec3) = generator_data.removeGenerator(location)
    override fun removeGenerator(id: Int) = generator_data.removeGenerator(id)
    override fun upgradeGeneratorTier(type: GeneratorType) = generator_data.upgradeGeneratorTier(type)
    override fun upgradeTeamGenerator(team: Team, level: ServerLevel) = generator_data.upgradeTeamGenerator(team, level)
}