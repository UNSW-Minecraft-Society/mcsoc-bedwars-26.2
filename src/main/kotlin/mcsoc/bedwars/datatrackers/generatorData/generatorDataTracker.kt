package mcsoc.bedwars.datatrackers.generatorData

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mcsoc.bedwars.datatrackers.LevelDataType
import mcsoc.bedwars.datatrackers.LevelTiedData
import mcsoc.bedwars.generators.Generator
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.Team
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

private class GeneratorDataStore() : GeneratorsHolder {
    companion object {
        private val genListCodec = Generator.CODEC.listOf().xmap({ l -> l.toMutableList() }, { l -> l.toList() })
        
        val CODEC: Codec<GeneratorDataStore> = RecordCodecBuilder.create {it.group(
                Codec.unboundedMap(GeneratorType.CODEC, genListCodec)
                    .fieldOf("generators")
                    .forGetter(GeneratorDataStore::generators),

                Codec.unboundedMap(GeneratorType.CODEC, Codec.INT)
                    .fieldOf("upgrades")
                    .forGetter(GeneratorDataStore::genUpgrades),
            ).apply(it, ::GeneratorDataStore)
        }
    }

    private val generators = HashMap<GeneratorType, MutableList<Generator>>()
    private val genUpgrades = HashMap<GeneratorType, Int>()

    private constructor(
        generators: Map<GeneratorType, MutableList<Generator>>,
        genUpgrades: Map<GeneratorType, Int>,
    ) : this() {
        this.generators.putAll(generators)
        this.genUpgrades.putAll(genUpgrades)
    }

    override fun getGenerators(type: GeneratorType) = generators[type] ?: mutableListOf()

    override fun addGenerator(gen: Generator, type: GeneratorType) {
        generators.getOrPut(type) { mutableListOf<Generator>() }.add(gen)
    }

    override fun removeGenerator(gen: Generator, type: GeneratorType) {
        gen.remove()
        generators[type]?.remove(gen)
    }

    override fun getGeneratorUpgrade(type: GeneratorType) = genUpgrades[type] ?: 0

    override fun upgradeGenerator(type: GeneratorType) {
        genUpgrades[type] = (genUpgrades[type] ?: 0) + 1
    }

    fun tick(server: MinecraftServer) {
        generators.flatMap { it.value }.forEach {
            if (!it.placed) it.place(server)
            it.tick()
        }
    }
}

class GeneratorDataTracker : LevelTiedData, GeneratorsExposer {
    companion object {
        val CODEC: MapCodec<GeneratorDataTracker> = RecordCodecBuilder.mapCodec {it.group(
                GeneratorDataStore.CODEC.fieldOf("generator_data").forGetter(GeneratorDataTracker::generator_data)
            ).apply(it, ::GeneratorDataTracker)
        }
    }

    override val type get() = LevelDataType.GeneratorState

    private val generator_data: GeneratorDataStore

    private constructor(gen_data: GeneratorDataStore) {
        this.generator_data = gen_data
    }

    internal constructor() : this(GeneratorDataStore())

    override fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, type: GeneratorType): Int {
        setDirty()
        return generator_data.addGenerator(server, location, level, type)
    }
    override fun addGenerator(server: MinecraftServer, location: Vec3, level: ResourceKey<Level>, team: Team): Int {
        setDirty()
        return generator_data.addGenerator(server, location, level, team)
    }
    override fun removeGenerator(location: Vec3) {
        setDirty()
        generator_data.removeGenerator(location)
    }
    override fun removeGenerator(id: Int) {
        setDirty()
        generator_data.removeGenerator(id)
    }
    override fun getGeneratorUpgrade(type: GeneratorType) = generator_data.getGeneratorUpgrade(type)
    override fun upgradeGenerator(type: GeneratorType) {
        setDirty()
        generator_data.upgradeGenerator(type)
    }
    fun tick(server: MinecraftServer) {
        setDirty()
        generator_data.tick(server)
    }
}