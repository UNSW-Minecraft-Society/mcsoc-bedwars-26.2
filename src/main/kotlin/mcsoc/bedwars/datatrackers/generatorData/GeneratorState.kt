package mcsoc.bedwars.datatrackers.generatorData

import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorFactory
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
    fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, team: Team): Int
    fun removeGenerator(location: Vec3)
    fun removeGenerator(id: Int)
    fun getGeneratorUpgrade(type: GeneratorType): Int
    fun upgradeGenerator(type: GeneratorType)
}

internal interface GeneratorsHolder : GeneratorsExposer {
    fun getGenerators(type: GeneratorType): List<Generator>
    fun addGenerator(gen: Generator, type: GeneratorType)
    fun removeGenerator(gen: Generator, type: GeneratorType)

    override fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, type: GeneratorType): Int {
        val gen = GeneratorFactory.createGenerator(type, location, level)
        gen.place(server)
        addGenerator(gen, type)
        return gen.id
    }

    override fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, team: Team): Int {
        val gen = GeneratorFactory.createGenerator(GeneratorType.BASE, location, level, team)
        gen.place(server)
        addGenerator(gen, GeneratorType.BASE)
        return gen.id
    }

    override fun removeGenerator(location: Vec3) {
        GeneratorType.entries.forEach { type ->
            getGenerators(type)
                .filter { it.location == location }
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
}
