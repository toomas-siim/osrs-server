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
@ScriptName("Falador Mithril Miner")
@ScriptDescription("Start in Falador East Bank with a pick equipped", "or in your inventory.")
@ScriptIdentifier("fally_mithril")
class MithrilMiner() : Script() {
    var state = State.INIT

    private val mine = ZoneBorders(3027, 9733, 3054, 9743) // Falador Mithril mine
    private val bank = ZoneBorders(3009, 3355, 3018, 3358) // Falador East Bank
    private var overlay: ScriptAPI.BottingOverlay? = null
    private var mithrilAmount = 0

    override fun tick() {
        when (state) {

            State.INIT -> {
                SystemLogger.log("State: INIT")
                overlay = scriptAPI.getOverlay()
                overlay?.init()
                overlay?.setTitle("Mining")
                overlay?.setTaskLabel("Mithril Mined:")
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

                // Refactored: Use getNearestNode with Mithril rock IDs
                val rock = getNearestMithrilRock()
                if (rock != null) {
                    SystemLogger.log("Found Mithril rock with ID: ${rock.id}. Starting to mine.")
                    rock.interaction.handle(bot, rock.interaction[0])
                } else {
                    SystemLogger.log("No Mithril rock found. Walking to a random location in the mine.")
                    // Walk to a random location in the mine to find Mithril rocks
                    scriptAPI.walkTo(mine.randomLoc)
                }

                // Update to track both Mithril ore types
                overlay?.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                   bot.inventory.getAmount(Items.MITHRIL_ORE_448) +
                                   mithrilAmount)
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
                val mithril447 = bot.inventory.getAmount(Items.MITHRIL_ORE_447)
                val mithril448 = bot.inventory.getAmount(Items.MITHRIL_ORE_448)
                mithrilAmount += mithril447 + mithril448

                SystemLogger.log("Depositing $mithril447 Mithril ore (447) and $mithril448 Mithril ore (448). Total Mithril mined: $mithrilAmount.")
                scriptAPI.bankItem(Items.MITHRIL_ORE_447)
                scriptAPI.bankItem(Items.MITHRIL_ORE_448)
                SystemLogger.log("Switching to TO_MINE state.")
                state = State.TO_MINE
            }

            State.TO_MINE -> {
                SystemLogger.log("State: TO_MINE")
                if (!mine.insideBorder(bot)) {
                    SystemLogger.log("Walking to mine.")
                    scriptAPI.walkTo(mine.randomLoc)
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
                scriptAPI.sellOnGE(Items.MITHRIL_ORE_447)
                scriptAPI.sellOnGE(Items.MITHRIL_ORE_448)
                SystemLogger.log("Sold Mithril at the Grand Exchange. Switching to GO_BACK state.")
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
     * Retrieves the nearest Mithril rock node using specific Mithril rock IDs.
     * @return The nearest Mithril rock Node or null if none found.
     */
    private fun getNearestMithrilRock(): Node? {
        for (id in mithrilRockIDs) {
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
        SystemLogger.log("Creating new instance of MithrilMiner script.")
        val script = MithrilMiner()
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
        SystemLogger.log("Initializing MithrilMiner script.")
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING, 55)  // Adjusted for Mithril mining
    }

    companion object {
        private val mithrilRockIDs = listOf(11373) // OSRS IDs for Mithril rocks
    }
}
