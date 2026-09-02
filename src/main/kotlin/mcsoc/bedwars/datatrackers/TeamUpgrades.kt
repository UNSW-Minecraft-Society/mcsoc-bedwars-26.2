package mcsoc.bedwars.datatrackers

import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.utils.Team

internal interface TeamUpgradesState {
    fun getArmourProt(): Int?
    fun getFeatherFalling(): Int?
    fun getHasteLevel(): Int? // todo
    fun hasSharpess(): Boolean
    fun hasHealPool(): Boolean // todo
    fun popTrap(): TrapUpgrade?
    fun getTraps(): List<TrapUpgrade>

    fun getSharpness()
    fun upgradeArmourProt()
    fun upgradeFeatherFalling()
    fun upgradeHasteLevel()
    fun getHealPool()
    fun addTrap(type: TrapUpgrade)
}

internal interface TeamUpgradesExposer {
    fun getArmourProt(team: Team): Int?
    fun getFeatherFalling(team: Team): Int?
    fun getHasteLevel(team: Team): Int?
    fun hasSharpess(team: Team): Boolean
    fun hasHealPool(team: Team): Boolean
    fun popTrap(team: Team): TrapUpgrade?
    fun getTraps(team: Team): List<TrapUpgrade>

    fun upgradeArmourProt(team: Team)
    fun upgradeFeatherFalling(team: Team)
    fun upgradeHasteLevel(team: Team)
    fun getSharpness(team: Team)
    fun getHealPool(team: Team)
    fun addTrap(team: Team, type: TrapUpgrade)
}

internal interface TeamUpgradesHolder : TeamUpgradesExposer {
    fun getTeam(team: Team): TeamUpgradesState

    override fun getArmourProt(team: Team): Int? = getTeam(team).getArmourProt()
    override fun getFeatherFalling(team: Team): Int? = getTeam(team).getFeatherFalling()
    override fun getHasteLevel(team: Team): Int? = getTeam(team).getHasteLevel()
    override fun hasSharpess(team: Team): Boolean = getTeam(team).hasSharpess()
    override fun hasHealPool(team: Team): Boolean = getTeam(team).hasHealPool()
    override fun getTraps(team: Team): List<TrapUpgrade> = getTeam(team).getTraps()
    override fun popTrap(team: Team): TrapUpgrade? = getTeam(team).popTrap()

    override fun upgradeArmourProt(team: Team) {
        getTeam(team).upgradeArmourProt()
    }

    override fun upgradeFeatherFalling(team: Team) {
        getTeam(team).upgradeFeatherFalling()
    }

    override fun upgradeHasteLevel(team: Team) {
        getTeam(team).upgradeHasteLevel()
    }

    override fun getSharpness(team: Team) {
        getTeam(team).getSharpness()
    }

    override fun getHealPool(team: Team) {
        getTeam(team).getHealPool()
    }

    override fun addTrap(team: Team, type: TrapUpgrade) {
        getTeam(team).addTrap(type)
    }
}