package mcsoc.bedwars.datatrackers.configloader

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.datatrackers.CustomEntityType
import mcsoc.bedwars.datatrackers.blockProtection
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader.Companion.place
import mcsoc.bedwars.datatrackers.gameState
import mcsoc.bedwars.datatrackers.generatorState
import mcsoc.bedwars.entities.spawnShopkeeper
import mcsoc.bedwars.generators.GeneratorType
import mcsoc.bedwars.utils.CylindricalBlockPos
import mcsoc.bedwars.utils.CylindricalBlockPos.Companion.toCylindricalBlockPos
import mcsoc.bedwars.utils.FLOAT_PI
import mcsoc.bedwars.utils.Team
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.reflect.KClass


private fun CylindricalBlockPos.relToMapOrigin(island_origin: BlockPos, island_pos: CylindricalBlockPos): BlockPos =
    rotated(FLOAT_PI - island_pos.angle).toBlockPos(island_origin)
    
    
private sealed class LoadedGeneratorType(val name: String, protected val type: GeneratorType) {
    companion object {
        private val REGISTRY: Map<String, (Team?) -> LoadedGeneratorType> by lazy {
            LoadedGeneratorType::class.sealedSubclasses
                .mapNotNull(KClass<out LoadedGeneratorType>::objectInstance)
                .associateBy(LoadedGeneratorType::name)
                .mapValues<String, LoadedGeneratorType, (Team?) -> LoadedGeneratorType> { i -> { _ -> i.value}}
                .toMutableMap()
                .also { it[Base.NAME] = {team -> Base(team ?: throw IllegalArgumentException("Base GeneratorType requires team"))} }
        }
        fun valueOf(id: String, team: Team? = null): LoadedGeneratorType = (REGISTRY[id] ?: throw IllegalArgumentException("Unknown LevelDataType: \"$id\""))(team)
    }
    
    class Base(override var team: Team) : LoadedGeneratorType(NAME, GeneratorType.BASE(team)) {
        companion object {
            const val NAME = "base"
        }
    }
    object Diamond : LoadedGeneratorType("diamond", GeneratorType.DIAMOND)
    object Emerald : LoadedGeneratorType("emerald", GeneratorType.EMERALD)
    
    open val team: Team? = null
    fun place(level: ServerLevel, pos: BlockPos) {
        level.generatorState.addGenerator(level.server, Vec3.atBottomCenterOf(pos), level.dimension(), type)
    }
}


private enum class LoadedShopkeeper(private val type: CustomEntityType) {
    PERSONAL(CustomEntityType.PLAYER_SHOPKEEPER),
    TEAM(CustomEntityType.TEAM_SHOPKEEPER);
    
    fun place(level: ServerLevel, pos: BlockPos) {
        spawnShopkeeper(level, Vec3.atBottomCenterOf(pos), type)
    }
} 


@Serializable
private data class ProtectionZoneData(
    val c1: @Serializable(with=BlockPosSerialiser::class) BlockPos = BlockPos(0, 0, 0), 
    val c2: @Serializable(with=BlockPosSerialiser::class) BlockPos = BlockPos(0, 0, 0)
)

private interface Island {
    val cpos: CylindricalBlockPos
    val structure: String
    val protection_zones: Iterable<ProtectionZoneData>
    open val rotation: (Float) -> Double get() = {0.0}
    
    fun place(level: ServerLevel, origin: BlockPos): BlockPos {
        val pos = cpos.toBlockPos(origin)
        BedwarsPlugin.LOGGER.info("island at")
        BedwarsPlugin.LOGGER.info("  cpos: {}", cpos)
        BedwarsPlugin.LOGGER.info("  pos : {}", pos)
        level.place(structure, pos, rotation(cpos.angle))
        
        for (zone in protection_zones) {
            level.blockProtection.registerProtectionZone(zone.c1.offset(pos), zone.c2.offset(pos))
        }
        
        return pos
    }
}

private interface GeneratorIsland : Island {
    val generators: Iterable<Pair<LoadedGeneratorType, CylindricalBlockPos>>

    override fun place(level: ServerLevel, origin: BlockPos): BlockPos {
        val pos = super.place(level, origin)
        for (generator_pos in generators) {
            val generator = generator_pos.first
            generator.place(level, generator_pos.second.relToMapOrigin(pos, cpos))
        }
        return pos
    } 
}

private data class IslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val protection_zones: Iterable<ProtectionZoneData> = listOf(ProtectionZoneData())
) : Island

private data class GeneratorIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val generators: Iterable<Pair<LoadedGeneratorType, CylindricalBlockPos>> = listOf(Pair(LoadedGeneratorType.Diamond, CylindricalBlockPos(0F, 0F, 0))),
    override val protection_zones: Iterable<ProtectionZoneData> = listOf(ProtectionZoneData())
) : GeneratorIsland

private data class BaseIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val generators: Iterable<Pair<LoadedGeneratorType, CylindricalBlockPos>> = listOf(Pair(LoadedGeneratorType.Base(Team.RED), CylindricalBlockPos(0F, -2F, 0))),
    override val protection_zones: Iterable<ProtectionZoneData> = listOf(ProtectionZoneData()),
    val shops: Iterable<Pair<LoadedShopkeeper, CylindricalBlockPos>> = listOf(Pair(LoadedShopkeeper.PERSONAL, CylindricalBlockPos(2F, 0F, 0))),
    val spawn_position: CylindricalBlockPos = CylindricalBlockPos(0F, 0F, 0),
    val bed_position: CylindricalBlockPos = CylindricalBlockPos(0F, 0F, 0),
    val team: Team = Team.RED
) : GeneratorIsland {
    override val rotation: (Float) -> Double get() = Float::toDouble
    override fun place(level: ServerLevel, origin: BlockPos): BlockPos {
        val pos = super.place(level, origin)
        level.gameState.setTeamSpawn(team, Vec3.atBottomCenterOf(spawn_position.relToMapOrigin(pos, cpos)))
        level.gameState.setTeamBedPosition(team, bed_position.relToMapOrigin(pos, cpos))
        for (shop_pos in shops) {
            val shop = shop_pos.first
            shop.place(level, shop_pos.second.relToMapOrigin(pos, cpos))
        }
        return pos
    }
}


@ConsistentCopyVisibility
@Serializable
data class MapData private constructor(
    private val mid_island: @Serializable(with=GeneratorIslandDataSerialiser::class) GeneratorIslandData,
    private val base_islands: List<@Serializable(with=BaseIslandDataSerialiser::class) BaseIslandData>,
    private val diamond_islands: List<@Serializable(with=GeneratorIslandDataSerialiser::class) GeneratorIslandData>,
    private val misc_islands: List<@Serializable(with=IslandDataSerialiser::class) IslandData>,
) {
    constructor() : this(
        GeneratorIslandData(generators = listOf(Pair(LoadedGeneratorType.Emerald, CylindricalBlockPos(0F, 0F, 0)))), 
        listOf(BaseIslandData()), 
        listOf(GeneratorIslandData()), 
        listOf(IslandData())
    )
    
    fun place(level: ServerLevel, origin: BlockPos) {
        // also register generators
        level.gameState.initialiseTeams(base_islands.map(BaseIslandData::team).toSet(), level.scoreboard)
        base_islands.forEach{it.place(level, origin)}
        diamond_islands.forEach{it.place(level, origin)}
        misc_islands.forEach{it.place(level, origin)}
        mid_island.place(level, origin)
    }
}

/*
 *  mid: IslandData
 *    cpos: CylindricalBlockPos
 *      radius: Float
 *      theta: Float
 *      height: Int
 *    structure: String
 * 
 *  bases: List<TeamIslandData> [
 *      cpos: CylindricalBlockPos
 *        radius: Float
 *        theta: Float
 *        height: Int
 *      structure: String
 *      team: String
 *  ]
 *  diamonds: List<IslandData> [
 *      cpos: CylindricalBlockPos
 *        radius: Float
 *        theta: Float
 *        height: Int
 *      structure: String
 *  ]
 *  misc: List<IslandData> [
 *      cpos: CylindricalBlockPos
 *        radius: Float
 *        theta: Float
 *        height: Int
 *      structure: String
 *  ]
 */

object BlockPosSerialiser: KSerializer<BlockPos> {
    override val descriptor = buildClassSerialDescriptor("BlockPos") {
        element<Int>("x") // 0
        element<Int>("y") // 1
        element<Int>("z") // 2
    }
    override fun serialize(encoder: Encoder, value: BlockPos) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.x)
            encodeIntElement(descriptor, 1, value.y)
            encodeIntElement(descriptor, 2, value.z)
        }
    }
    override fun deserialize(decoder: Decoder): BlockPos = decoder.decodeStructure(descriptor) {
        var x = 0
        var y = 0
        var z = 0
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> x = decodeIntElement(descriptor, index)
                1 -> y = decodeIntElement(descriptor, index)
                2 -> z = decodeIntElement(descriptor, index)
                else -> error("Unexpected index: $index")
            }
        }
        
        BlockPos(x, y, z)
    }
}

private object GeneratorTypeSerialiser: KSerializer<LoadedGeneratorType> {
    override val descriptor = buildClassSerialDescriptor("GeneratorType") {
        element<String>("type") // 0
    }
    override fun serialize(encoder: Encoder, value: LoadedGeneratorType) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            val team = value.team
        }
    }
    override fun deserialize(decoder: Decoder): LoadedGeneratorType = decoder.decodeStructure(descriptor) {
        var type: String = ""
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = decodeStringElement(descriptor, index)
                else -> error("Unexpected index: $index")
            }
        }
        
        try {
            LoadedGeneratorType.valueOf(type, null)
        } catch(e: IllegalArgumentException) {
            LoadedGeneratorType.valueOf(type, Team.RED)
        }
    }
}

private object GeneratorPositionSerialiser: KSerializer<Pair<LoadedGeneratorType, CylindricalBlockPos>> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element("generator", GeneratorTypeSerialiser.descriptor)
        element("pos", CylindricalBlockPos.serializer().descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Pair<LoadedGeneratorType, CylindricalBlockPos>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, GeneratorTypeSerialiser, value.first)
            encodeSerializableElement(descriptor, 1, CylindricalBlockPos.serializer(), value.second)
        }
    }
    
    override fun deserialize(decoder: Decoder): Pair<LoadedGeneratorType, CylindricalBlockPos> = decoder.decodeStructure(descriptor) {
        var type: LoadedGeneratorType = LoadedGeneratorType.Emerald
        var pos: CylindricalBlockPos = CylindricalBlockPos(0F, 0F, 0)
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = decodeSerializableElement(descriptor, index, GeneratorTypeSerialiser)
                1 -> pos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                else -> error("Unexpected index: $index")
            }
        }
        Pair(type, pos)
    }
}


private object IslandDataSerialiser: KSerializer<IslandData> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element("cpos", CylindricalBlockPos.serializer().descriptor)
        element<String>("structure")
        element("protection_zones", ListSerializer(ProtectionZoneData.serializer()).descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: IslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(ProtectionZoneData.serializer()), value.protection_zones.toList())
        }
    }
    
    override fun deserialize(decoder: Decoder): IslandData = decoder.decodeStructure(descriptor) {
        val default = GeneratorIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var protection_zones = default.protection_zones
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> protection_zones = decodeSerializableElement(descriptor, index, ListSerializer(ProtectionZoneData.serializer()))
                else -> error("Unexpected index: $index")
            }
        }

        IslandData(cpos, structure, protection_zones)
    }
}


private object GeneratorIslandDataSerialiser: KSerializer<GeneratorIslandData> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element("cpos", CylindricalBlockPos.serializer().descriptor)
        element<String>("structure")
        element("generators", ListSerializer(GeneratorPositionSerialiser).descriptor)
        element("protection_zones", ListSerializer(ProtectionZoneData.serializer()).descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: GeneratorIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(GeneratorPositionSerialiser), value.generators.toList())
            encodeSerializableElement(descriptor, 3, ListSerializer(ProtectionZoneData.serializer()), value.protection_zones.toList())
        }
    }
    
    override fun deserialize(decoder: Decoder): GeneratorIslandData = decoder.decodeStructure(descriptor) {
        val default = GeneratorIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var generators = default.generators
        var protection_zones = default.protection_zones
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> generators = decodeSerializableElement(descriptor, index, ListSerializer(GeneratorPositionSerialiser))
                3 -> protection_zones = decodeSerializableElement(descriptor, index, ListSerializer(ProtectionZoneData.serializer()))
                else -> error("Unexpected index: $index")
            }
        }

        GeneratorIslandData(cpos, structure, generators, protection_zones)
    }
}

private object ShopkeeperPositionSerialiser: KSerializer<Pair<LoadedShopkeeper, CylindricalBlockPos>> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element<String>("type")
        element("pos", CylindricalBlockPos.serializer().descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Pair<LoadedShopkeeper, CylindricalBlockPos>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.first.name.lowercase())
            encodeSerializableElement(descriptor, 1, CylindricalBlockPos.serializer(), value.second)
        }
    }
    
    override fun deserialize(decoder: Decoder): Pair<LoadedShopkeeper, CylindricalBlockPos> = decoder.decodeStructure(descriptor) {
        var type: LoadedShopkeeper = LoadedShopkeeper.PERSONAL
        var pos: CylindricalBlockPos = CylindricalBlockPos(0F, 0F, 0)
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = LoadedShopkeeper.valueOf(decodeStringElement(descriptor, index).uppercase())
                1 -> pos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                else -> error("Unexpected index: $index")
            }
        }
        Pair(type, pos)
    }
}

private object BaseIslandDataSerialiser: KSerializer<BaseIslandData> {
    override val descriptor = buildClassSerialDescriptor("BaseIslandData") {
        element("cpos", CylindricalBlockPos.serializer().descriptor)
        element<String>("structure")
        element("generators", ListSerializer(GeneratorPositionSerialiser).descriptor)
        element("shopkeepers", ListSerializer(ShopkeeperPositionSerialiser).descriptor)
        element<String>("team")
        element("spawn_position", CylindricalBlockPos.serializer().descriptor)
        element("bed_position", CylindricalBlockPos.serializer().descriptor)
        element("protection_zones", ListSerializer(ProtectionZoneData.serializer()).descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: BaseIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(GeneratorPositionSerialiser), value.generators.toList())
            encodeSerializableElement(descriptor, 3, ListSerializer(ShopkeeperPositionSerialiser), value.shops.toList())
            encodeStringElement(descriptor, 4, value.team.name.lowercase())
            encodeSerializableElement(descriptor, 5, CylindricalBlockPos.serializer(), value.spawn_position)
            encodeSerializableElement(descriptor, 6, CylindricalBlockPos.serializer(), value.bed_position)
            encodeSerializableElement(descriptor, 7, ListSerializer(ProtectionZoneData.serializer()), value.protection_zones.toList())
        }
    }
    
    override fun deserialize(decoder: Decoder): BaseIslandData = decoder.decodeStructure(descriptor) {
        val default = BaseIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var generators = default.generators
        var protection_zones = default.protection_zones
        var shops = default.shops
        var spawn_pos = default.spawn_position
        var bed_pos = default.bed_position
        var team = default.team
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> generators = decodeSerializableElement(descriptor, index, ListSerializer(GeneratorPositionSerialiser))
                3 -> shops = decodeSerializableElement(descriptor, index, ListSerializer(ShopkeeperPositionSerialiser))
                4 -> team = Team.valueOf(decodeStringElement(descriptor, index).uppercase())
                5 -> spawn_pos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                6 -> bed_pos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                7 -> protection_zones = decodeSerializableElement(descriptor, index, ListSerializer(ProtectionZoneData.serializer()))
                else -> error("Unexpected index: $index")
            }
        }
        
        val teamed_generators = generators.map{Pair(LoadedGeneratorType.Base(team), it.second)}
        BaseIslandData(cpos, structure, teamed_generators, listOf(), shops, spawn_pos, bed_pos, team)
    }
}