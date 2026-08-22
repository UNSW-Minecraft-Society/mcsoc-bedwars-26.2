package mcsoc.bedwars.datatrackers.configloader

import kotlinx.io.IOException
import kotlinx.serialization.KSerializer
import java.nio.file.Path

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import mcsoc.bedwars.BedwarsPlugin
import mcsoc.bedwars.utils.CylindricalBlockPos
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.notExists

import kotlin.math.PI


private const val base_structure_name = "base"
private const val mid_structure_name = "mid"
private const val diamond_structure_name = "diamond"
private const val misc_structure_name = "misc"


interface ConfigDataExposer {
    val debug: Boolean
    val map_data: Map<String, MapData>
}

@Serializable
internal data class ConfigData(
    @SerialName("plugin")
    val config_data: PluginConfigData = PluginConfigData(), 
    
    @SerialName("maps")
    override val map_data: Map<String, MapData> = mapOf(
        Pair("example", 
            MapData(
                IslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 0F, 0F, 0), mid_structure_name),
                listOf(
                    TeamIslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 50F, (PI / 2.0F).toFloat(), 6), base_structure_name, "rec"),
                    TeamIslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 50F, (3 * PI / 2.0F).toFloat(), 6), base_structure_name, "blue")
                ),
                listOf(
                    IslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 20F, (PI / 4.0F).toFloat(), 3), diamond_structure_name),
                    IslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 20F, (5 * PI / 4.0F).toFloat(), 3), diamond_structure_name)
                ),
                listOf(
                    IslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 20F, (3 * PI / 4.0F).toFloat(), -2), misc_structure_name),
                    IslandData(CylindricalBlockPos(BlockPos(0, 100, 0), 20F, (7 * PI / 4.0F).toFloat(), -2), misc_structure_name)
                )
            )
        )
    )
) : ConfigDataExposer {
    override val debug get() = config_data.debug
}


private interface Island {
    val cpos: CylindricalBlockPos
    val structure: String
    
    operator fun component1(): CylindricalBlockPos = cpos
}

data class IslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default"
) : Island

data class TeamIslandData(
    override val cpos: CylindricalBlockPos = CylindricalBlockPos(),
    override val structure: String = "default",
    val team: String
) : Island

@Serializable(with = MapDataSerialiser::class)
data class MapData(
    val mid_island: IslandData,
    val base_islands: List<TeamIslandData>,
    val diamond_islands: List<IslandData>,
    val misc_islands: List<IslandData>
)

@Serializable
internal data class PluginConfigData(
    val debug: Boolean = false
)

abstract class PluginConfigLoader {
    lateinit var loaded_config: ConfigDataExposer
    internal lateinit var config_path: Path
    
    fun initialise() {
        config_path = getConfigPath()
        config_path.createParentDirectories()
        config_path.takeIf{it.notExists()}?.createFile()
        loadDataFromFiles()
    }

    private fun loadConfigData() {
        loaded_config = try {
            getConfigData()
        } catch (e: Exception) {
            when (e) {
                is SerializationException -> BedwarsPlugin.LOGGER.error("Error while deserialising ConfigData: ", e)
                is IllegalArgumentException -> BedwarsPlugin.LOGGER.error("Invalid ConfigData: ", e)
                is IOException -> BedwarsPlugin.LOGGER.error("Cannot read config file: ", e)
                else -> throw e
            }
            ConfigData()
        }
        
        saveConfigData()
    }
    
    fun loadDataFromFiles() {
        this.loadConfigData()
        
        this.saveDataToFiles()
    }
    
    fun saveDataToFiles() {
        this.saveConfigData()
    }
    
    open fun getConfigPath(): Path = FabricLoader.getInstance().configDir / BedwarsPlugin.CONFIG_PATH
    abstract fun getConfigData(): ConfigDataExposer
    abstract fun saveConfigData()
}


/*
 *  pos: BlockPos
 * 
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

object ReducedCylindricalBlockPosSerialiser: KSerializer<CylindricalBlockPos> {
    override val descriptor = buildClassSerialDescriptor("ReducedCylindricalBlockPos") {
        element<Float>("radius")
        element<Float>("angle")
        element<Int>("height")
    }
    
    override fun serialize(encoder: Encoder, value: CylindricalBlockPos) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.radius)
            encodeFloatElement(descriptor, 1, value.angle)
            encodeIntElement(descriptor, 2, value.height)
        }
    }
    override fun deserialize(decoder: Decoder): CylindricalBlockPos = decoder.decodeStructure(descriptor) {
        var r = 0F
        var t = 0F
        var h = 0
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> r = decodeFloatElement(descriptor, 0)
                1 -> t = decodeFloatElement(descriptor, 1)
                2 -> h = decodeIntElement(descriptor, 2)
                else -> error("Unexpected index: $index")
            }
        }
        
        CylindricalBlockPos(BlockPos(0, 0, 0), r, t, h)
    }
}

object IslandDataSerialiser: KSerializer<IslandData> {
    override val descriptor = buildClassSerialDescriptor("IslandDataReduced") {
        element("cpos", ReducedCylindricalBlockPosSerialiser.descriptor)
        element<String>("structure")
    }
    
    override fun serialize(encoder: Encoder, value: IslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ReducedCylindricalBlockPosSerialiser, value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
        }
    }
    
    override fun deserialize(decoder: Decoder): IslandData = decoder.decodeStructure(descriptor) {
        var cpos = CylindricalBlockPos()
        var structure = ""
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, 0, ReducedCylindricalBlockPosSerialiser)
                1 -> structure = decodeStringElement(descriptor, 1)
                else -> error("Unexpected index: $index")
            }
        }
        
        IslandData(cpos, structure)
    }
}

object TeamIslandDataSerialiser: KSerializer<TeamIslandData> {
    override val descriptor = buildClassSerialDescriptor("IslandDataReduced") {
        element("cpos", ReducedCylindricalBlockPosSerialiser.descriptor)
        element<String>("structure")
        element<String>("team")
    }
    
    override fun serialize(encoder: Encoder, value: TeamIslandData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, ReducedCylindricalBlockPosSerialiser, value.cpos)
            encodeStringElement(descriptor, 1, value.structure)
            encodeStringElement(descriptor, 2, value.team)
        }
    }
    
    override fun deserialize(decoder: Decoder): TeamIslandData = decoder.decodeStructure(descriptor) {
        var cpos = CylindricalBlockPos()
        var structure = ""
        var team = ""
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> cpos = decodeSerializableElement(descriptor, 0, ReducedCylindricalBlockPosSerialiser)
                1 -> structure = decodeStringElement(descriptor, 1)
                2 -> team = decodeStringElement(descriptor, 2)
                else -> error("Unexpected index: $index")
            }
        }
        
        TeamIslandData(cpos, structure, team)
    }
}

object MapDataSerialiser : KSerializer<MapData> {
    
    override val descriptor = buildClassSerialDescriptor("MapDataReduced") {
        element("pos", BlockPosSerialiser.descriptor)
        element("mid_island", IslandDataSerialiser.descriptor)
        element("base_islands", ListSerializer(TeamIslandDataSerialiser).descriptor)
        element("diamond_islands", ListSerializer(IslandDataSerialiser).descriptor)
        element("misc_islands", ListSerializer(IslandDataSerialiser).descriptor)
    }
    
    // pos: value.mid_island.pos.origin
    override fun serialize(encoder: Encoder, value: MapData) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, BlockPosSerialiser, value.mid_island.cpos.origin)
            encodeSerializableElement(descriptor, 1, IslandDataSerialiser, value.mid_island)
            encodeSerializableElement(descriptor, 2, ListSerializer(TeamIslandDataSerialiser), value.base_islands)
            encodeSerializableElement(descriptor, 3, ListSerializer(IslandDataSerialiser), value.diamond_islands)
            encodeSerializableElement(descriptor, 4, ListSerializer(IslandDataSerialiser), value.misc_islands)
        }
    }
    
    override fun deserialize(decoder: Decoder): MapData = decoder.decodeStructure(descriptor) {
        var pos: BlockPos = BlockPos(0, 0, 0)
        var mid_island: IslandData = IslandData()
        var base_islands: List<TeamIslandData> = listOf()
        var diamond_islands: List<IslandData> = listOf()
        var misc_islands: List<IslandData> = listOf()
        
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> pos = decodeSerializableElement(descriptor, 0, BlockPosSerialiser)
                1 -> mid_island = decodeSerializableElement(descriptor, 1, IslandDataSerialiser)
                2 -> base_islands = decodeSerializableElement(descriptor, 2, ListSerializer(TeamIslandDataSerialiser))
                3 -> diamond_islands = decodeSerializableElement(descriptor, 3, ListSerializer(IslandDataSerialiser))
                4 -> misc_islands = decodeSerializableElement(descriptor, 4, ListSerializer(IslandDataSerialiser))
                else -> error("Unexpected index: $index")
            }
        }
        
        mid_island.cpos.origin = pos
        for ((cpos) in sequenceOf(base_islands, diamond_islands, misc_islands).flatten()) {
            cpos.origin = pos
        }
        
        MapData(mid_island, base_islands, diamond_islands, misc_islands)
    }
}

val module = SerializersModule {
    contextual(BlockPosSerialiser)
}