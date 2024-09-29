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
@ScriptName("Varrock Iron Miner")
@ScriptDescription("Start in Varrock West Bank with a pick equipped", "or in your inventory.")
@ScriptIdentifier("varrock_iron")
class VarrockIronMiner() : Script() {
    var state = State.INIT

    val mine = ZoneBorders(3173, 3360, 3184, 3373) // South-west Varrock Mine
    val bank = ZoneBorders(3180, 3433, 3185, 3447) // Varrock West Bank
    var overlay: ScriptAPI.BottingOverlay? = null
    var ironAmount = 0

    override fun tick() {
        when (state) {

            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Mining")
                overlay!!.setTaskLabel("Iron Mined:")
                overlay!!.setAmount(0)

                if (mine.insideBorder(bot)) {
                    state = State.MINING
                } else {
                    state = State.TO_MINE
                }
            }

            State.MINING -> {
                if (bot.inventory.freeSlots() == 0) {
                    state = State.TO_BANK
                }
                if (!mine.insideBorder(bot)) {
                    scriptAPI.walkTo(mine.randomLoc)
                } else {
                    val rock = getNearestIronRock()
                    if (rock != null) {
                        rock.interaction.handle(bot, rock.interaction[0])
                    } else {
                        // No iron rocks available, wait or walk around
                        scriptAPI.wait(1000)
                    }
                }
                overlay!!.setAmount(bot.inventory.getAmount(Items.IRON_ORE_440) + ironAmount)
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
                ironAmount += bot.inventory.getAmount(Items.IRON_ORE_440)
                scriptAPI.bankItem(Items.IRON_ORE_440)
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
                scriptAPI.sellOnGE(Items.IRON_ORE_440)
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
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING, 75)
    }

    /**
     * Helper function to get the nearest iron rock.
     */
    private fun getNearestIronRock(): Node? {
        val rocks = scriptAPI.getNodesInArea("Rocks", mine)
        return rocks?.firstOrNull { isIronRock(it) }
    }

    /**
     * Helper function to check if the rock is an iron rock.
     */
    private fun isIronRock(rock: Node): Boolean {
        val ironRockIDs = listOf(2092, 2093) // IDs for iron rocks
        return ironRockIDs.contains(rock.id)
    }

}
