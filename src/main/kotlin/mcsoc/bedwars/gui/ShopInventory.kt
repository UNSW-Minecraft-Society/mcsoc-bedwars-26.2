package mcsoc.bedwars.gui

import mcsoc.bedwars.items.BedwarsItems
import mcsoc.bedwars.items.BedwarsItems.ballOfBugsItemStack
import mcsoc.bedwars.items.BedwarsItems.bedBruteItemStack
import mcsoc.bedwars.items.BedwarsItems.bridgeEggItemStack
import mcsoc.bedwars.items.BedwarsItems.dreamDefenderItemStack
import mcsoc.bedwars.items.BedwarsItems.fireballItemStack
import mcsoc.bedwars.items.BedwarsItems.instantTNTItemStack
import mcsoc.bedwars.items.BedwarsItems.playerTrackerItemStack
import mcsoc.bedwars.items.BedwarsItems.popupTowerItemStack
import mcsoc.bedwars.upgrades.TeamUpgradeType
import mcsoc.bedwars.upgrades.TrapUpgrade
import mcsoc.bedwars.upgrades.UpgradeItemType
import mcsoc.bedwars.utils.Team
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potions

enum class ShopConfig {
    NORMAL,
    SILLY
}

enum class ShopType(val title: String) {
    PLAYER_SHOP("Player Shop"),
    TEAM_SHOP("Team Shop");
}

object ShopInventory {
    fun getShopMenuType(type: ShopType, config: ShopConfig): MenuType<ChestMenu> {
        when (config) {
            ShopConfig.NORMAL -> {
                return when (type) {
                    ShopType.PLAYER_SHOP -> MenuType.GENERIC_9x5
                    ShopType.TEAM_SHOP -> MenuType.GENERIC_9x3
                }
            }
            ShopConfig.SILLY -> {
                return when (type) {
                    ShopType.PLAYER_SHOP -> MenuType.GENERIC_9x6
                    ShopType.TEAM_SHOP -> MenuType.GENERIC_9x3
                }
            }
        }
    }

    /**
     * Gets the mapping between the ordering of products in the inventory and which slots they display in the menu.
     */
    fun getProductSlotIndex(type: ShopType, config: ShopConfig): Array<Int> {
        when (config) {
            ShopConfig.NORMAL -> {
                return when (type) {
                    ShopType.PLAYER_SHOP -> arrayOf(
                        1, 10, 19, 28, 37,
                        2, 11, 20, 29, 38,
                        3, 12, 21, 30, 39,
                        4, 13, 22, 31, 40,
                        5, 14, 23, 32, 41,
                        6, 15, 24, 33, 42,
                        7, 16, 25, 34, 43)
                    ShopType.TEAM_SHOP -> arrayOf(
                        1, 10, 19,
                        2, 11, 20,
                        3, 12, 21,
                        4, 13, 22,
                        5, 14, 23,
                        6, 15, 24,
                        7, 16, 25)
                }
            }
            ShopConfig.SILLY -> {
                return when (type) {
                    ShopType.PLAYER_SHOP -> arrayOf(
                        1, 10, 19, 28, 37, 46,
                        2, 11, 20, 29, 38, 47,
                        3, 12, 21, 30, 39, 48,
                        4, 13, 22, 31, 40, 49,
                        5, 14, 23, 32, 41, 50,
                        6, 15, 24, 33, 42, 51,
                        7, 16, 25, 34, 43, 52)
                    ShopType.TEAM_SHOP -> arrayOf(
                        1, 10, 19,
                        2, 11, 20,
                        3, 12, 21,
                        4, 13, 22,
                        5, 14, 23,
                        6, 15, 24,
                        7, 16, 25)
                }
            }
        }
    }

    /**
     * Gets an array of shop products for a particular shop `type` and `config`.
     */
    fun getProducts(type: ShopType, config: ShopConfig): Array<ShopProduct> {
        when (config) {
            ShopConfig.NORMAL -> {
                when (type) {
                    ShopType.PLAYER_SHOP -> { return arrayOf(
                        // These are ShopPlayerUpgrades
                        ShopPlayerUpgrade(UpgradeItemType.ARMOUR,
                            arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD),
                            arrayOf(20, 12, 6),
                            arrayOf("Chainmail Armor", "Iron Armor", "Diamond Armor")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.SWORD,
                            arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD),
                            arrayOf(10, 7, 3),
                            arrayOf("Stone Sword", "Iron Sword", "Diamond Sword")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.PICKAXE,
                            arrayOf(Items.IRON_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT),
                            arrayOf(5, 10, 3, 6),
                            arrayOf("Wooden Pickaxe", "Iron Pickaxe", "Golden Pickaxe", "Diamond Pickaxe")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.AXE,
                            arrayOf(Items.IRON_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT),
                            arrayOf(5, 10, 3, 6),
                            arrayOf("Wooden Axe", "Stone Axe", "Iron Axe", "Diamond Axe")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.SHEARS,
                            arrayOf(Items.IRON_INGOT), arrayOf(15), arrayOf("Shears")
                        ),

                        // These are ShopItems
                        ShopItem(Items.BOW, 1, Items.GOLD_INGOT, 12),
                        ShopPlayerCustomItem({player -> BedwarsItems.powerBowItemStack(player.level())}, Items.GOLD_INGOT, 24),
                        ShopPlayerCustomItem({player -> BedwarsItems.punchBowItemStack(player.level())}, Items.EMERALD, 6),
                        ShopPlayerCustomItem({player -> BedwarsItems.knockbackStickItemStack(player.level())}, Items.GOLD_INGOT, 5),
                        ShopItem(Items.WATER_BUCKET, 1, Items.GOLD_INGOT, 2),

                        ShopItem(Items.ARROW, 8, Items.GOLD_INGOT, 2),
                        ShopItem(Items.GOLDEN_APPLE, 1, Items.GOLD_INGOT, 3),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.STRONG_SWIFTNESS, 0.5f)}, Items.EMERALD, 1),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.STRONG_LEAPING, 0.5f)}, Items.EMERALD, 1),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.INVISIBILITY, 0.25f)}, Items.EMERALD, 2),

                        ShopTeamItem(Team.entries.associateWith { Items.WOOL.pick(it.dyeColour) },
                            16, Items.IRON_INGOT, 4),
                        ShopItem(Items.SANDSTONE, 16, Items.IRON_INGOT, 16),
                        ShopItem(Items.END_STONE, 16, Items.IRON_INGOT, 24),
                        ShopItem(Items.OBSIDIAN, 4, Items.EMERALD, 4),
                        ShopItem(Items.OAK_PLANKS, 16, Items.GOLD_INGOT, 4),

                        ShopTeamItem(Team.entries.associateWith { Items.STAINED_GLASS.pick(it.dyeColour) },
                            4, Items.IRON_INGOT, 12),
                        ShopTeamItem(Team.entries.associateWith { Items.DYED_TERRACOTTA.pick(it.dyeColour) }, 16, Items.IRON_INGOT, 12),
                        ShopItem(Items.PACKED_ICE, 8, Items.IRON_INGOT, 8),
                        ShopItem(Items.LADDER, 16, Items.IRON_INGOT, 4),
                        EmptyShopProduct(),

                        ShopCustomItem(BedwarsItems::popupTowerItemStack, Items.IRON_INGOT, 24),
                        ShopCustomItem(BedwarsItems::bridgeEggItemStack, Items.EMERALD, 1),
                        ShopItem(Items.WIND_CHARGE, 12, Items.EMERALD, 1),
                        ShopItem(Items.ENDER_PEARL, 1, Items.EMERALD, 2),
                        ShopCustomItem(BedwarsItems::playerTrackerItemStack, Items.EMERALD, 3),

                        ShopCustomItem(BedwarsItems::ballOfBugsItemStack, Items.IRON_INGOT, 24),
                        ShopCustomItem(BedwarsItems::fireballItemStack, Items.IRON_INGOT, 36),
                        ShopCustomItem(BedwarsItems::instantTNTItemStack, Items.GOLD_INGOT, 4),
                        ShopCustomItem(BedwarsItems::dreamDefenderItemStack, Items.IRON_INGOT, 120),
                        ShopCustomItem(BedwarsItems::bedBruteItemStack, Items.EMERALD, 2),
                    )}
                    ShopType.TEAM_SHOP -> { return arrayOf(
                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),

                        IntShopTeamUpgrade(TeamUpgradeType.PROTECTION, Items.SHIELD,
                            Array(4) {Items.DIAMOND},
                            arrayOf(5,10,20,30),
                            "Protection"
                        ).addDescriptionLine("Applies protection to your team's armour for more defense"),
                        IntShopTeamUpgrade(TeamUpgradeType.FEATHER_FALLING, Items.FEATHER,
                            Array(2) {Items.DIAMOND},
                            arrayOf(1,2),
                            "Feather Falling"
                        ).addDescriptionLine("Applies feather falling to your team's boots for less fall damage"),
                        IntShopTeamUpgrade(TeamUpgradeType.HASTE, Items.GOLDEN_PICKAXE,
                            Array(2) {Items.DIAMOND},
                            arrayOf(2,3),
                            "Haste"
                        ).addDescriptionLine("Applies haste to your team's equipment for faster block breaking"),

                        BooleanShopTeamUpgrade(TeamUpgradeType.SHARPNESS, Items.IRON_SWORD, Items.DIAMOND, 8, "Sharpness")
                            .addDescriptionLine("Applies sharpness to your team's swords to deal more damage"),
                        BooleanShopTeamUpgrade(TeamUpgradeType.HEAL_POOL, Items.GOLDEN_APPLE, Items.DIAMOND, 3, "Heal Pool")
                            .addDescriptionLine("Applies faster healing for your team at your island"),
                        EmptyShopProduct(),

                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),

                        ShopTrapUpgrade(TrapUpgrade.BLINDNESS, Items.DYE.black, Items.DIAMOND, 2, "Blindness Trap")
                            .addDescriptionLine("Applies blindness to an intruder on your island"),
                        ShopTrapUpgrade(TrapUpgrade.COUNTER, Items.POTION, Items.DIAMOND, 2, "Counter Trap")
                            .addDescriptionLine("Applies buffs to your team when an intruder enters your island"),
                        EmptyShopProduct(),

                        ShopTrapUpgrade(TrapUpgrade.REVEAL, Items.ENDER_EYE, Items.DIAMOND, 2, "Reveal Trap")
                            .addDescriptionLine("Applies glowing to an intruder on your island"),
                        ShopTrapUpgrade(TrapUpgrade.MINING, Items.ELDER_GUARDIAN_SPAWN_EGG, Items.DIAMOND, 2, "Mining Fatigue Trap")
                            .addDescriptionLine("Applies mining fatigue to an intruder on your island"),
                        EmptyShopProduct(),

                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),
                    )}
                }
            }

            ShopConfig.SILLY -> {
                when (type) {
                    ShopType.PLAYER_SHOP -> { return arrayOf(
                        // These are ShopPlayerUpgrades
                        ShopPlayerUpgrade(UpgradeItemType.ARMOUR,
                            arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD),
                            arrayOf(20, 12, 6),
                            arrayOf("Chainmail Armor", "Iron Armor", "Diamond Armor")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.SWORD,
                            arrayOf(Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD),
                            arrayOf(10, 7, 3),
                            arrayOf("Stone Sword", "Iron Sword", "Diamond Sword")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.PICKAXE,
                            arrayOf(Items.IRON_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT),
                            arrayOf(5, 10, 3, 6),
                            arrayOf("Wooden Pickaxe", "Iron Pickaxe", "Golden Pickaxe", "Diamond Pickaxe")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.AXE,
                            arrayOf(Items.IRON_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.GOLD_INGOT),
                            arrayOf(5, 10, 3, 6),
                            arrayOf("Wooden Axe", "Stone Axe", "Iron Axe", "Diamond Axe")
                        ),
                        ShopPlayerUpgrade(UpgradeItemType.SHEARS,
                            arrayOf(Items.IRON_INGOT), arrayOf(15), arrayOf("Shears")
                        ),
                        ShopPlayerCustomItem({player -> BedwarsItems.lungeIronSpearItemStack(player.level())}, Items.EMERALD, 8),

                        // These are ShopItems
                        ShopItem(Items.ARROW, 8, Items.GOLD_INGOT, 2),
                        ShopItem(Items.BOW, 1, Items.GOLD_INGOT, 12),
                        ShopPlayerCustomItem({player -> BedwarsItems.powerBowItemStack(player.level())}, Items.GOLD_INGOT, 24),
                        ShopPlayerCustomItem({player -> BedwarsItems.punchBowItemStack(player.level())}, Items.EMERALD, 6),
                        ShopPlayerCustomItem({player -> BedwarsItems.knockbackStickItemStack(player.level())}, Items.GOLD_INGOT, 5),
                        ShopPlayerCustomItem({player -> BedwarsItems.windBurstMaceItemStack(player.level())}, Items.EMERALD, 6),

                        ShopPlayerCustomItem({player -> BedwarsItems.loyaltyTridentItemStack(player.level())}, Items.EMERALD, 2),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.STRONG_SWIFTNESS, 0.5f)}, Items.EMERALD, 1),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.STRONG_LEAPING, 0.5f)}, Items.EMERALD, 1),
                        ShopCustomItem({BedwarsItems.potionItemStack(Potions.INVISIBILITY, 0.25f)}, Items.EMERALD, 2),
                        EmptyShopProduct(),
                        EmptyShopProduct(),

                        ShopTeamItem(Team.entries.associateWith { Items.WOOL.pick(it.dyeColour) },
                            16, Items.IRON_INGOT, 4),
                        ShopTeamItem(Team.entries.associateWith { Items.STAINED_GLASS.pick(it.dyeColour) },
                            4, Items.IRON_INGOT, 12),
                        ShopTeamItem(Team.entries.associateWith { Items.GLAZED_TERRACOTTA.pick(it.dyeColour) }, 64, Items.IRON_INGOT, 12),
                        ShopItem(Items.END_STONE, 16, Items.IRON_INGOT, 24),
                        ShopItem(Items.OBSIDIAN, 4, Items.EMERALD, 4),
                        ShopPlayerCustomItem({player -> BedwarsItems.randomWoodPlanks(player.random.nextInt())}, Items.GOLD_INGOT, 4),

                        ShopItem(Items.LADDER, 16, Items.IRON_INGOT, 4),
                        ShopItem(Items.WATER_BUCKET, 1, Items.GOLD_INGOT, 2),
                        ShopItem(Items.GOLDEN_APPLE, 1, Items.GOLD_INGOT, 3),
                        ShopItem(Items.WIND_CHARGE, 16, Items.GOLD_INGOT, 16),
                        ShopItem(Items.LAVA_BUCKET, 1, Items.EMERALD, 1),
                        ShopItem(Items.ENDER_PEARL, 1, Items.EMERALD, 1),

                        ShopCustomItem(BedwarsItems::popupTowerItemStack, Items.IRON_INGOT, 8),
                        ShopCustomItem(BedwarsItems::fireballItemStack, Items.IRON_INGOT, 18),
                        ShopCustomItem(BedwarsItems::instantTNTItemStack, Items.GOLD_INGOT, 2),
                        ShopCustomItem(BedwarsItems::bridgeEggItemStack, Items.GOLD_INGOT, 4),
                        ShopCustomItem(BedwarsItems::playerTrackerItemStack, Items.EMERALD, 3),
                        ShopCustomItem(BedwarsItems::skyWandItemStack, Items.EMERALD, 3),

                        ShopCustomItem(BedwarsItems::ballOfBugsItemStack, Items.IRON_INGOT, 4),
                        ShopItem(Items.HAPPY_GHAST_SPAWN_EGG, 1, Items.GOLD_INGOT, 16),
                        ShopTeamItem(Team.entries.associateWith { Items.HARNESS.pick(it.dyeColour) },
                            1, Items.IRON_INGOT, 4),
                        ShopCustomItem(BedwarsItems::dreamDefenderItemStack, Items.IRON_INGOT, 60),
                        ShopCustomItem(BedwarsItems::bedBruteItemStack, Items.EMERALD, 1),
                        EmptyShopProduct(),
                    )}
                    ShopType.TEAM_SHOP -> { return arrayOf(
                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),

                        IntShopTeamUpgrade(TeamUpgradeType.PROTECTION, Items.SHIELD,
                            Array(4) {Items.DIAMOND},
                            arrayOf(5,10,20,30),
                            "Protection"
                        ).addDescriptionLine("Applies protection to your team's armour for more defense"),
                        IntShopTeamUpgrade(TeamUpgradeType.FEATHER_FALLING, Items.FEATHER,
                            Array(2) {Items.DIAMOND},
                            arrayOf(1,2),
                            "Feather Falling"
                        ).addDescriptionLine("Applies feather falling to your team's boots for less fall damage"),
                        IntShopTeamUpgrade(TeamUpgradeType.HASTE, Items.GOLDEN_PICKAXE,
                            Array(2) {Items.DIAMOND},
                            arrayOf(2,3),
                            "Haste"
                        ).addDescriptionLine("Applies haste to your team's equipment for faster block breaking"),

                        BooleanShopTeamUpgrade(TeamUpgradeType.SHARPNESS, Items.IRON_SWORD, Items.DIAMOND, 8, "Sharpness")
                            .addDescriptionLine("Applies sharpness to your team's swords to deal more damage"),
                        BooleanShopTeamUpgrade(TeamUpgradeType.HEAL_POOL, Items.GOLDEN_APPLE, Items.DIAMOND, 3, "Heal Pool")
                            .addDescriptionLine("Applies faster healing for your team at your island"),
                        EmptyShopProduct(),

                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),

                        ShopTrapUpgrade(TrapUpgrade.BLINDNESS, Items.DYE.black, Items.DIAMOND, 2, "Blindness Trap")
                            .addDescriptionLine("Applies blindness to an intruder on your island"),
                        ShopTrapUpgrade(TrapUpgrade.COUNTER, Items.POTION, Items.DIAMOND, 2, "Counter Trap")
                            .addDescriptionLine("Applies buffs to your team when an intruder enters your island"),
                        EmptyShopProduct(),

                        ShopTrapUpgrade(TrapUpgrade.REVEAL, Items.ENDER_EYE, Items.DIAMOND, 2, "Reveal Trap")
                            .addDescriptionLine("Applies glowing to an intruder on your island"),
                        ShopTrapUpgrade(TrapUpgrade.MINING, Items.ELDER_GUARDIAN_SPAWN_EGG, Items.DIAMOND, 2, "Mining Fatigue Trap")
                            .addDescriptionLine("Applies mining fatigue to an intruder on your island"),
                        EmptyShopProduct(),

                        EmptyShopProduct(), EmptyShopProduct(), EmptyShopProduct(),
                    )}
                }
            }
        }
    }
}