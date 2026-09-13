package mcsoc.bedwars.datatrackers.mapdata

import net.minecraft.core.BlockPos


internal interface LoadedMapExposer {
    var map_centre: BlockPos
    // var mapTeams: Iterable<Team>
}

internal interface LoadedMapHolder : LoadedMapExposer