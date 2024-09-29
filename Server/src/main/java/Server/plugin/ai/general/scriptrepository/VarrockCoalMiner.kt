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

@PlayerCompatible
@ScriptName("Varrock Coal Miner")
@ScriptDescription("Start in Varrock West Bank with a pick equipped", "or in your inventory.")
@ScriptIdentifier("varrock_coal")
class VarrockCoalMiner() : Script() {
    var state = State.INIT

    val mine = ZoneBorders(3081, 3420, 3087, 3426) // Barbarian Village coal mine
    val bank = ZoneBorders(3180, 3433, 3185, 3447) // Varrock West Bank
    var overlay: ScriptAPI.BottingOverlay? = null
    var coalAmount = 0

    // Variables to track inventory and mining state
    var lastInventoryCount = 0
    var miningInProgress = false

    override fun tick() {
        when (state) {

            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Mining")
                overlay!!.setTaskLabel("Coal Mined:")
                overlay!!.setAmount(0)

                // Initialize lastInventoryCount
                lastInventoryCount = bot.inventory.getAmount(Items.COAL_453)
                miningInProgress = false

                if (mine.insideBorder(bot)) {
                    state = State.MINING
                } else {
                    state = State.TO_MINE
                }
            }

            State.MINING -> {
                val currentInventoryCount = bot.inventory.getAmount(Items.COAL_453)

                if (bot.inventory.freeSlots() == 0) {
                    state = State.TO_BANK
                } else if (!mine.insideBorder(bot)) {
                    scriptAPI.walkTo(mine.randomLoc)
                } else if (!miningInProgress) {
                    val rock = getNearestCoalRock()
                    if (rock != null) {
                        rock.interact(bot, "Mine")
                        miningInProgress = true
                    } else {
                        // Walk to a random location in the mine to find coal rocks
                        scriptAPI.walkTo(mine.randomLoc)
                    }
                } else if (currentInventoryCount > lastInventoryCount) {
                    // An ore has been mined
                    lastInventoryCount = currentInventoryCount
                    miningInProgress = false
                }
                // Update overlay
                overlay!!.setAmount(currentInventoryCount + coalAmount)
            }

            State.TO_BANK -> {
                if (bank.insideBorder(bot)) {
                    val bankBooth = scriptAPI.getNearestNode("Bank booth", true)
                    if (bankBooth != null) {
                        bot.pulseManager.run(object : BankingPulse(this, bankBooth) {
                            override fun pulse(): Boolean {
                                state = State.BANKING
                                return super.pulse()
                            }
                        })
                    }

                } else {
                    scriptAPI.walkTo(bank.randomLoc)
                }
            }

            State.BANKING -> {
                val depositedCoal = bot.inventory.getAmount(Items.COAL_453)
                coalAmount += depositedCoal
                scriptAPI.bankItem(Items.COAL_453)
                lastInventoryCount = 0
                miningInProgress = false
                state = State.TO_MINE
            }

            State.TO_MINE -> {
                if (!mine.insideBorder(bot)) {
                    scriptAPI.walkTo(mine.randomLoc)
                } else {
                    state = State.MINING
                }
            }

            State.TO_GE -> {
                scriptAPI.teleportToGE()
                state = State.SELLING
            }

            State.SELLING -> {
                scriptAPI.sellOnGE(Items.COAL_453)
                state = State.GO_BACK
            }

            State.GO_BACK -> {
                scriptAPI.teleport(bank.randomLoc)
                state = State.TO_MINE
            }

        }
    }

    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot, bank, DestinationFlag.OBJECT) {
        override fun pulse(): Boolean {
            script.bot.faceLocation(bank.location)
            return true
        }
    }

    override fun newInstance(): Script {
        val script = VarrockCoalMiner()
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
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING, 75)
    }

    /**
     * Helper function to check if the rock is a coal rock.
     * Uses the correct IDs for coal rocks in 2009scape.
     */
    private fun isCoalRock(rock: Node): Boolean {
        val coalRockIDs = listOf(2096, 2097) // IDs for coal rocks
        return coalRockIDs.contains(rock.id)
    }

    /**
     * Helper function to get the nearest coal rock that can be mined.
     */
    private fun getNearestCoalRock(): Node? {
        val nodes = scriptAPI.getNearbyNodes()
        val coalRocks = nodes.filter { isCoalRock(it) && it.hasAction("Mine") }
        return coalRocks.minByOrNull { it.location.distanceTo(bot.location) }
    }

}
