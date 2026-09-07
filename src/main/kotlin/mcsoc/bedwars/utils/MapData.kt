package mcsoc.bedwars.utils

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
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader.Companion.place
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import kotlin.collections.map
import kotlin.reflect.KClass


private sealed class GeneratorType(val name: String) {
    companion object {
        private val REGISTRY: Map<String, (Team?) -> GeneratorType> by lazy {
            GeneratorType::class.sealedSubclasses
                .mapNotNull(KClass<out GeneratorType>::objectInstance)
                .associateBy(GeneratorType::name)
                .mapValues<String, GeneratorType, (Team?) -> GeneratorType> { i -> { _ -> i.value}}
                .toMutableMap()
                .also { it[Base.NAME] = {team -> Base(team ?: throw IllegalArgumentException("Base GeneratorType must requires team"))} }
        }
        fun valueOf(id: String, team: Team? = null): GeneratorType = (REGISTRY[id] ?: throw IllegalArgumentException("Unknown LevelDataType: \"$id\""))(team)
    }
    
    class Base(override val team: Team) : GeneratorType(NAME) {
        companion object {
            const val NAME = "base"
        }
        override fun place(level: ServerLevel, pos: BlockPos) {
            
        }
    }
    object Diamond : GeneratorType("diamond") {
        override fun place(level: ServerLevel, pos: BlockPos) {
            
        }
    }
    object Emerald : GeneratorType("emerald") {
        override fun place(level: ServerLevel, pos: BlockPos) {
            
        }
    }
    
    open val team: Team? = null
    abstract fun place(level: ServerLevel, pos: BlockPos)
}


private enum class LoadedShopkeeper {
    PERSONAL {
        override fun place(level: ServerLevel, pos: BlockPos) {
            
        }
    },
    TEAM {
        override fun place(level: ServerLevel, pos: BlockPos) {

        }
    };
    
    abstract fun place(level: ServerLevel, pos: BlockPos)
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
    
    fun place(level: ServerLevel, origin: BlockPos) {
        level.place(structure, cpos.toBlockPos(origin))
    }
}

private interface GeneratorIsland : Island {
    val generators: Iterable<Pair<GeneratorType, BlockPos>>

    override fun place(level: ServerLevel, origin: BlockPos) {
        super.place(level, origin)
        for (generator_pos in generators) {
            val generator = generator_pos.first
            generator.place(level, generator_pos.second)
        }
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
    override val generators: Iterable<Pair<GeneratorType, BlockPos>> = listOf(Pair(GeneratorType.Diamond, BlockPos(0, 0, 0))),
    override val protection_zones: Iterable<ProtectionZoneData> = listOf(ProtectionZoneData())
) : GeneratorIsland

private data class BaseIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val generators: Iterable<Pair<GeneratorType, BlockPos>> = listOf(Pair(GeneratorType.Base(Team.RED), BlockPos(0, -2, 0))),
    override val protection_zones: Iterable<ProtectionZoneData> = listOf(ProtectionZoneData()),
    val shops: Iterable<Pair<LoadedShopkeeper, BlockPos>> = listOf(Pair(LoadedShopkeeper.PERSONAL, BlockPos(2, 0, 0))),
    val team: Team = Team.RED
) : GeneratorIsland {
    override fun place(level: ServerLevel, origin: BlockPos) {
        super.place(level, origin)
        
        for (shop_pos in shops) {
            val shop = shop_pos.first
            shop.place(level, shop_pos.second)
        }
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
        GeneratorIslandData(generators = listOf(Pair(GeneratorType.Emerald, BlockPos(0, 0, 0)))), 
        listOf(BaseIslandData()), 
        listOf(GeneratorIslandData()), 
        listOf(IslandData())
    )
    
    fun place(level: ServerLevel, origin: BlockPos) {
        // also register generators
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

private object GeneratorTypeSerialiser: KSerializer<GeneratorType> {
    override val descriptor = buildClassSerialDescriptor("GeneratorType") {
        element<String>("type") // 0
        element<String>("team") // 1
    }
    override fun serialize(encoder: Encoder, value: GeneratorType) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.name)
            val team = value.team
            if (team != null) encodeStringElement(descriptor, 1, team.name.lowercase())
        }
    }
    override fun deserialize(decoder: Decoder): GeneratorType = decoder.decodeStructure(descriptor) {
        var type: String = ""
        var team: Team? = null
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = decodeStringElement(descriptor, index)
                1 -> team = Team.valueOf(decodeStringElement(descriptor, index).uppercase())
                else -> error("Unexpected index: $index")
            }
        }

        GeneratorType.valueOf(type, team)
    }
}

private object GeneratorPositionSerialiser: KSerializer<Pair<GeneratorType, BlockPos>> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element("generator", GeneratorTypeSerialiser.descriptor)
        element("pos", BlockPosSerialiser.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Pair<GeneratorType, BlockPos>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, GeneratorTypeSerialiser, value.first)
            encodeSerializableElement(descriptor, 1, BlockPosSerialiser, value.second)
        }
    }
    
    override fun deserialize(decoder: Decoder): Pair<GeneratorType, BlockPos> = decoder.decodeStructure(descriptor) {
        var type: GeneratorType = GeneratorType.Emerald
        var pos: BlockPos = BlockPos(0, 0, 0)
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = decodeSerializableElement(descriptor, index, GeneratorTypeSerialiser)
                1 -> pos = decodeSerializableElement(descriptor, index, BlockPosSerialiser)
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

private object ShopkeeperPositionSerialiser: KSerializer<Pair<LoadedShopkeeper, BlockPos>> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element<String>("type")
        element("pos", BlockPosSerialiser.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Pair<LoadedShopkeeper, BlockPos>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.first.name.lowercase())
            encodeSerializableElement(descriptor, 1, BlockPosSerialiser, value.second)
        }
    }
    
    override fun deserialize(decoder: Decoder): Pair<LoadedShopkeeper, BlockPos> = decoder.decodeStructure(descriptor) {
        var type: LoadedShopkeeper = LoadedShopkeeper.PERSONAL
        var pos: BlockPos = BlockPos(0, 0, 0)
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = LoadedShopkeeper.valueOf(decodeStringElement(descriptor, index).uppercase())
                1 -> pos = decodeSerializableElement(descriptor, index, BlockPosSerialiser)
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
        element("protection_zones", ListSerializer(ProtectionZoneData.serializer()).descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: BaseIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(GeneratorPositionSerialiser), value.generators.toList())
            encodeSerializableElement(descriptor, 3, ListSerializer(ShopkeeperPositionSerialiser), value.shops.toList())
            encodeStringElement(descriptor, 4, value.team.name.lowercase())
            encodeSerializableElement(descriptor, 5, ListSerializer(ProtectionZoneData.serializer()), value.protection_zones.toList())
        }
    }
    
    override fun deserialize(decoder: Decoder): BaseIslandData = decoder.decodeStructure(descriptor) {
        val default = BaseIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var generators = default.generators
        var protection_zones = default.protection_zones
        var shops = default.shops
        var team = default.team
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> generators = decodeSerializableElement(descriptor, index, ListSerializer(GeneratorPositionSerialiser))
                3 -> shops = decodeSerializableElement(descriptor, index, ListSerializer(ShopkeeperPositionSerialiser))
                4 -> team = Team.valueOf(decodeStringElement(descriptor, index).uppercase())
                5 -> protection_zones = decodeSerializableElement(descriptor, index, ListSerializer(ProtectionZoneData.serializer()))
                else -> error("Unexpected index: $index")
            }
        }

        BaseIslandData(cpos, structure, generators, listOf(), shops, team)
    }
}