package plugin.ai.general.scriptrepository

import core.game.interaction.DestinationFlag
import core.game.interaction.MovementPulse
import core.game.node.Node
import core.game.node.item.Item
import core.game.world.map.zone.ZoneBorders
import core.tools.Items
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler
import core.game.node.entity.skill.Skills
import core.game.system.SystemLogger

@PlayerCompatible
@ScriptName("Varrock Iron Miner")
@ScriptDescription("Start in Varrock West Bank with a pick equipped", "or in your inventory.")
@ScriptIdentifier("varrock_iron")
class VarrockIronMiner() : Script() {
    var state = State.INIT

    private val mine = ZoneBorders(3172, 3358, 3184, 3373) // Southwest Varrock iron mine
    private val bank = ZoneBorders(3180, 3433, 3185, 3447) // Varrock West Bank
    private val westZone = ZoneBorders(3160, 3420, 3176, 3438) // Small area west of the bank
    private var overlay: ScriptAPI.BottingOverlay? = null
    private var ironAmount = 0
    private var visitedWestSpot = false

    override fun tick() {
        when (state) {

            State.INIT -> {
                SystemLogger.log("State: INIT")
                overlay = scriptAPI.getOverlay()
                overlay?.init()
                overlay?.setTitle("Mining")
                overlay?.setTaskLabel("Iron Mined:")
                overlay?.setAmount(0)

                if (mine.insideBorder(bot)) {
                    SystemLogger.log("Bot is inside the mine. Switching to MINING state.")
                    state = State.MINING
                } else {
                    SystemLogger.log("Bot is not inside the mine. Switching to TO_MINE state.")
                    state = State.TO_MINE
                }
            }

            State.MINING -> {
                SystemLogger.log("State: MINING")
                if (bot.inventory.freeSlots() == 0) {
                    SystemLogger.log("Inventory full. Switching to TO_BANK state.")
                    state = State.TO_BANK
                    return
                }

                if (!mine.insideBorder(bot)) {
                    SystemLogger.log("Bot is not inside the mine. Walking to mine.")
                    scriptAPI.walkTo(mine.randomLoc)
                    return
                }

                // Refactored: Use getNearestNode with iron rock IDs
                val rock = getNearestIronRock()
                if (rock != null) {
                    SystemLogger.log("Found iron rock with ID: ${rock.id}. Starting to mine.")
                    rock.interaction.handle(bot, rock.interaction[0])
                } else {
                    SystemLogger.log("No iron rock found. Walking to a random location in the mine.")
                    // Walk to a random location in the mine to find iron rocks
                    scriptAPI.walkTo(mine.randomLoc)
                }

                // Update to track both iron ore types
                overlay?.setAmount(bot.inventory.getAmount(Items.IRON_ORE_440) +
                                   bot.inventory.getAmount(Items.IRON_ORE_441) +
                                   ironAmount)
            }

            State.TO_BANK -> {
                SystemLogger.log("State: TO_BANK")
                if (bank.insideBorder(bot)) {
                    SystemLogger.log("Bot is at the bank. Finding bank booth.")
                    val bankBooth = scriptAPI.getNearestNode("Bank booth", true)
                    if (bankBooth != null) {
                        SystemLogger.log("Bank booth found. Initiating banking process.")
                        bot.pulseManager.run(object : BankingPulse(this, bankBooth) {
                            override fun pulse(): Boolean {
                                SystemLogger.log("Banking pulse executed. Switching to BANKING state.")
                                state = State.BANKING
                                return super.pulse()
                            }
                        })
                    } else {
                        SystemLogger.log("No bank booth found.")
                    }

                } else {
                    SystemLogger.log("Bot is not at the bank. Walking to bank.")
                    scriptAPI.walkTo(bank.randomLoc)
                }
            }

            State.BANKING -> {
                SystemLogger.log("State: BANKING")
                val iron440 = bot.inventory.getAmount(Items.IRON_ORE_440)
                val iron441 = bot.inventory.getAmount(Items.IRON_ORE_441)
                ironAmount += iron440 + iron441

                SystemLogger.log("Depositing $iron440 iron ore (440) and $iron441 iron ore (441). Total iron mined: $ironAmount.")
                scriptAPI.bankItem(Items.IRON_ORE_440)
                scriptAPI.bankItem(Items.IRON_ORE_441)
                visitedWestSpot = false // Reset visitedWestSpot after banking
                SystemLogger.log("Resetting visitedWestSpot. Switching to TO_MINE state.")
                state = State.TO_MINE
            }

            State.TO_MINE -> {
                SystemLogger.log("State: TO_MINE")
                if (!mine.insideBorder(bot)) {
                    if (!visitedWestSpot) {
                        if (!westZone.insideBorder(bot)) {
                            SystemLogger.log("Bot is walking to the west spot.")
                            scriptAPI.walkTo(westZone.randomLoc)
                        } else {
                            SystemLogger.log("Bot visited west spot. Setting visitedWestSpot to true.")
                            visitedWestSpot = true
                        }
                    } else {
                        SystemLogger.log("Walking to mine after visiting west spot.")
                        scriptAPI.walkTo(mine.randomLoc)
                    }
                } else {
                    SystemLogger.log("Arrived at mine. Switching to MINING state.")
                    state = State.MINING
                }
            }

            State.TO_GE -> {
                SystemLogger.log("State: TO_GE")
                scriptAPI.teleportToGE()
                SystemLogger.log("Teleported to Grand Exchange. Switching to SELLING state.")
                state = State.SELLING
            }

            State.SELLING -> {
                SystemLogger.log("State: SELLING")
                scriptAPI.sellOnGE(Items.IRON_ORE_440)
                scriptAPI.sellOnGE(Items.IRON_ORE_441)
                SystemLogger.log("Sold iron at the Grand Exchange. Switching to GO_BACK state.")
                state = State.GO_BACK
            }

            State.GO_BACK -> {
                SystemLogger.log("State: GO_BACK")
                scriptAPI.teleport(bank.randomLoc)
                SystemLogger.log("Teleported back to bank area. Switching to TO_MINE state.")
                state = State.TO_MINE
            }

        }
    }

    /**
     * Retrieves the nearest iron rock node using specific iron rock IDs.
     * @return The nearest iron rock Node or null if none found.
     */
    private fun getNearestIronRock(): Node? {
        for (id in ironRockIDs) {
            val rock = scriptAPI.getNearestNode(id, true)
            if (rock != null) {
                return rock
            }
        }
        return null
    }

    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot, bank, DestinationFlag.OBJECT) {
        override fun pulse(): Boolean {
            SystemLogger.log("BankingPulse: Facing bank location.")
            script.bot.faceLocation(bank.location)
            return true
        }
    }

    override fun newInstance(): Script {
        SystemLogger.log("Creating new instance of VarrockIronMiner script.")
        val script = VarrockIronMiner()
        script.bot = SkillingBotAssembler().produce(SkillingBotAssembler.Wealth.POOR, bot.startLocation)
        return script
    }

    enum class State {
        MINING,
        TO_MINE,
        TO_BANK,
        BANKING,
        TO_GE,
        SELLING,
        GO_BACK,
        INIT
    }

    init {
        SystemLogger.log("Initializing VarrockIronMiner script.")
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING, 75)
    }

    /**
     * Helper function to check if the rock is an iron rock.
     * Uses the correct IDs for iron rocks in OSRS.
     */
    private fun isIronRock(rock: Node): Boolean {
        val isIron = ironRockIDs.contains(rock.id)
        SystemLogger.log("Checking if rock with ID: ${rock.id} is iron rock: $isIron")
        return isIron
    }

    companion object {
        private val ironRockIDs = listOf(11955) // OSRS IDs for iron rocks
    }
}
