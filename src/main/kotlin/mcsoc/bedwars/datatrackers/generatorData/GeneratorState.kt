package mcsoc.bedwars.datatrackers.generatorData

import mcsoc.bedwars.datatrackers.TeamStateRecord
import mcsoc.bedwars.generators.BaseGenerator
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
import mcsoc.bedwars.generators.TieredGenerator
import mcsoc.bedwars.utils.Team
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

// mod data or team related
internal interface TeamGeneratorState {
    fun getGenerator(): BaseGenerator?
    fun setGenerator(gen: BaseGenerator)
    fun upgradeGenerator()
}

internal interface TeamGeneratorExposer {
    fun getGenerator(team: Team): BaseGenerator?
    fun setGenerator(team: Team, gen: BaseGenerator)
    fun upgradeGenerator(team: Team)
}

internal interface TeamGeneratorHolder : TeamGeneratorExposer {
    fun getTeam(team: Team): TeamGeneratorState
    override fun getGenerator(team: Team) = getTeam(team).getGenerator()
    override fun setGenerator(team: Team, gen: BaseGenerator) = getTeam(team).setGenerator(gen)
    override fun upgradeGenerator(team: Team) = getTeam(team).upgradeGenerator()
}

// generator related
internal interface GeneratorsExposer {
    fun addGenerator(type: GeneratorType, location: Vec3, level: ServerLevel): Int
    fun addGenerator(location: Vec3, level: ServerLevel, team: Team): Int
    fun upgradeTeamGenerator(team: Team, level: ServerLevel)
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
