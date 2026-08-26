package mcsoc.bedwars.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import mcsoc.bedwars.datatrackers.configloader.maploader.StructureLoader.Companion.place
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import kotlin.math.PI
import kotlin.time.Duration


@Serializable
enum class LoadedGenerator(
    val base_cooldown: Duration,
    val products: Iterable<Pair<ItemStackTemplate, Int>>
) {  
    BASE(
        10.ticks,
        setOf(
            Pair(ItemStackTemplate(Items.IRON_INGOT), 1),
            Pair(ItemStackTemplate(Items.GOLD_INGOT), 5)
        )
    ),
    DIAMOND(
        100.ticks,
        setOf(
            Pair(ItemStackTemplate(Items.DIAMOND), 1),
        )
    ),
    EMERALD(
        200.ticks,
        setOf(
            Pair(ItemStackTemplate(Items.EMERALD), 1),
        )
    )
}

@Serializable
enum class LoadedShopkeeper {
    PERSONAL,
    TEAM
}

private interface Island {
    val cpos: CylindricalBlockPos
    val structure: String
    
    operator fun component1(): CylindricalBlockPos = cpos
    
    fun place(level: Level, origin: BlockPos) {
        level.place(structure, cpos.toBlockPos(origin))
    }
}

private interface GeneratorIsland : Island {
    val generators: Iterable<Pair<LoadedGenerator, BlockPos>>
}

@Serializable
data class IslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default"
) : Island

data class GeneratorIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val generators: Iterable<Pair<LoadedGenerator, BlockPos>> = listOf(Pair(LoadedGenerator.DIAMOND, BlockPos(0, 0, 0)))
) : GeneratorIsland

data class BaseIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    override val generators: Iterable<Pair<LoadedGenerator, BlockPos>> = listOf(Pair(LoadedGenerator.BASE, BlockPos(0, -2, 0))),
    val shops: Iterable<Pair<LoadedShopkeeper, BlockPos>> = listOf(Pair(LoadedShopkeeper.PERSONAL, BlockPos(2, 0, 0))),
    val team: String = "red"
) : GeneratorIsland


@Serializable
data class MapData(
    val mid_island: @Serializable(with=GeneratorIslandDataSerialiser::class) GeneratorIslandData = GeneratorIslandData(),
    val base_islands: List<@Serializable(with=BaseIslandDataSerialiser::class) BaseIslandData> = listOf(BaseIslandData()),
    val diamond_islands: List<@Serializable(with=GeneratorIslandDataSerialiser::class) GeneratorIslandData> = listOf(GeneratorIslandData()),
    val misc_islands: List<IslandData> = listOf(IslandData())
) {
    fun place(level: Level, origin: BlockPos) {
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
                0 -> x = decodeIntElement(descriptor, 0)
                1 -> y = decodeIntElement(descriptor, 1)
                2 -> z = decodeIntElement(descriptor, 2)
                else -> error("Unexpected index: $index")
            }
        }
        
        BlockPos(x, y, z)
    }
}


object GeneratorPositionSerialiser: KSerializer<Pair<LoadedGenerator, BlockPos>> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element<String>("type")
        element("pos", BlockPosSerialiser.descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: Pair<LoadedGenerator, BlockPos>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.first.name.lowercase())
            encodeSerializableElement(descriptor, 1, BlockPosSerialiser, value.second)
        }
    }
    
    override fun deserialize(decoder: Decoder): Pair<LoadedGenerator, BlockPos> = decoder.decodeStructure(descriptor) {
        var type: LoadedGenerator = LoadedGenerator.BASE
        var pos: BlockPos = BlockPos(0, 0, 0)
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> type = LoadedGenerator.valueOf(decodeStringElement(descriptor, index).uppercase())
                1 -> pos = decodeSerializableElement(descriptor, index, BlockPosSerialiser)
                else -> error("Unexpected index: $index")
            }
        }
        Pair(type, pos)
    }
}

object GeneratorIslandDataSerialiser: KSerializer<GeneratorIslandData> {
    override val descriptor = buildClassSerialDescriptor("GeneratorIslandData") {
        element("cpos", CylindricalBlockPos.serializer().descriptor)
        element<String>("structure")
        element("generators", ListSerializer(GeneratorPositionSerialiser).descriptor)
    }
    
    override fun serialize(encoder: Encoder, value: GeneratorIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(GeneratorPositionSerialiser), value.generators.toList())
        }
    }
    
    override fun deserialize(decoder: Decoder): GeneratorIslandData = decoder.decodeStructure(descriptor) {
        val default = GeneratorIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var generators = default.generators
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> generators = decodeSerializableElement(descriptor, index, ListSerializer(GeneratorPositionSerialiser))
                else -> error("Unexpected index: $index")
            }
        }

        GeneratorIslandData(cpos, structure, generators)
    }
}

object ShopkeeperPositionSerialiser: KSerializer<Pair<LoadedShopkeeper, BlockPos>> {
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

object BaseIslandDataSerialiser: KSerializer<BaseIslandData> {
    override val descriptor = buildClassSerialDescriptor("BaseIslandData") {
        element("cpos", CylindricalBlockPos.serializer().descriptor)
        element<String>("structure")
        element("generators", ListSerializer(GeneratorPositionSerialiser).descriptor)
        element("shopkeepers", ListSerializer(ShopkeeperPositionSerialiser).descriptor)
        element<String>("team")
    }
    
    override fun serialize(encoder: Encoder, value: BaseIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, CylindricalBlockPos.serializer(), value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeSerializableElement(descriptor, 2, ListSerializer(GeneratorPositionSerialiser), value.generators.toList())
            encodeSerializableElement(descriptor, 3, ListSerializer(ShopkeeperPositionSerialiser), value.shops.toList())
            encodeStringElement(descriptor, 4, value.team)
        }
    }
    
    override fun deserialize(decoder: Decoder): BaseIslandData = decoder.decodeStructure(descriptor) {
        val default = BaseIslandData()
        var cpos = default.cpos
        var structure = default.structure
        var generators = default.generators
        var shops = default.shops
        var team = default.team
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, index, CylindricalBlockPos.serializer())
                1 -> structure = decodeStringElement(descriptor, index)
                2 -> generators = decodeSerializableElement(descriptor, index, ListSerializer(GeneratorPositionSerialiser))
                3 -> shops = decodeSerializableElement(descriptor, index, ListSerializer(ShopkeeperPositionSerialiser))
                4 -> team = decodeStringElement(descriptor, index)
                else -> error("Unexpected index: $index")
            }
        }

        BaseIslandData(cpos, structure, generators, shops, team)
    }
}