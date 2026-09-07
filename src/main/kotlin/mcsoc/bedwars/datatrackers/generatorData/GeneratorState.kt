package mcsoc.bedwars.datatrackers.generatordata

import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.utils.Team
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

// mod data or team related
internal interface TeamGeneratorState {
    fun upgradeGen()
    fun getGenUpgrade(): Int
}

internal interface TeamGeneratorExposer {
    fun upgradeGen(team: Team)
    fun getGenUpgrade(team: Team): Int
}

internal interface TeamGeneratorHolder : TeamGeneratorExposer {
    fun getTeam(team: Team): TeamGeneratorState
    override fun upgradeGen(team: Team) = getTeam(team).upgradeGen()
    override fun getGenUpgrade(team: Team) = getTeam(team).getGenUpgrade()
}

// generator related
internal interface GeneratorsExposer {
    fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, type: GeneratorType): Int
    fun addTeamGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, team: Team): Int
    fun removeGenerator(location: Vec3)
    fun removeGenerator(id: Int)
    fun getGeneratorUpgrade(type: GeneratorType): Int
    fun upgradeGenerator(type: GeneratorType)
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(): List<Generator>
    fun addGenerator(gen: Generator)
    fun removeGenerator(gen: Generator)

    override fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, type: GeneratorType): Int {
        val gen = Generator(location, level, type)
        gen.place(server)
        addGenerator(gen)
        return gen.id
    }

    override fun addTeamGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, team: Team): Int {
        return addGenerator(server, location, level, GeneratorType.BASE(team))
    }

    override fun removeGenerator(location: Vec3) {
        getGenerators()
            .filter { it.location == location }
            .forEach { removeGenerator(it) }
    }

    override fun removeGenerator(id: Int) {
        getGenerators()
            .filter { it.id == id }
            .forEach { removeGenerator(it) }
    }
}
