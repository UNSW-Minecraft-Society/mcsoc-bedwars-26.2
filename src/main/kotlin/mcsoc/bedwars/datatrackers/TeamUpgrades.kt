package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.upgrades.TeamUpgrade
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.utils.Team

internal interface TeamUpgradesState {
    fun <T> getUpgrade(type: TeamUpgradeType<T>): T
    fun <T> upgrade(type: TeamUpgradeType<T>)
    fun popTrap(): TrapUpgrade?
    fun getTraps(): List<TrapUpgrade>
    fun addTrap(type: TrapUpgrade)
}

internal interface TeamUpgradesExposer {
    fun <T> getUpgrade(team: Team, type: TeamUpgradeType<T>): T
    fun <T> upgrade(team: Team, type: TeamUpgradeType<T>)
    fun popTrap(team: Team): TrapUpgrade?
    fun getTraps(team: Team): List<TrapUpgrade>
    fun addTrap(team: Team, type: TrapUpgrade)
}

internal interface TeamUpgradesHolder : TeamUpgradesExposer {
    fun getTeam(team: Team): TeamUpgradesState

    override fun <T> getUpgrade(team: Team, type: TeamUpgradeType<T>): T = getTeam(team).getUpgrade(type)
    override fun <T> upgrade(team: Team, type: TeamUpgradeType<T>) {
        getTeam(team).upgrade(type)
    }

    override fun getTraps(team: Team): List<TrapUpgrade> = getTeam(team).getTraps()
    override fun popTrap(team: Team): TrapUpgrade? = getTeam(team).popTrap()
    override fun addTrap(team: Team, type: TrapUpgrade) {
        getTeam(team).addTrap(type)
    }
}