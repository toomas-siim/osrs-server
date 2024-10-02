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
@ScriptDescription("Start in Falador East Bank with a pick equipped","or in your inventory.")
@ScriptIdentifier("fally_mithril")
class MithrilMiner() : Script() {
    var state = State.INIT
    var ladderSwitch = false

    val bottomLadder = ZoneBorders(3016,9736,3024,9742)
    val topLadder = ZoneBorders(3016,3336,3022,3342)
    val mine = ZoneBorders(3037,9733,3064,9743)
    val bank = ZoneBorders(3009,3355,3018,3358)

    // Corrected array initialization
    val mithrilOreIds = arrayOf(
        2103, 2102, 4988, 4989, 4990, 11943, 11942, 11945, 11944, 11947,
        11946, 14855, 14854, 14853, 16687, 20421, 20420, 20419, 20418, 19012,
        19013, 19014, 21278, 21279, 21280, 25369, 25368, 25370, 29236, 29237,
        29238, 32439, 32438, 32440, 31087, 31086, 31088, 31170, 31171, 31172,
        37692, 37693, 37694, 42036
    )

    var overlay: ScriptAPI.BottingOverlay? = null
    var mithrilAmount = 0

    override fun tick() {
        SystemLogger.log("Tick invoked with state: $state")

        when(state){
            State.INIT -> {
                SystemLogger.log("Initializing script...")
                overlay = scriptAPI.getOverlay()
                ladderSwitch = true
                overlay!!.init()
                overlay!!.setTitle("Mining")
                overlay!!.setTaskLabel("Mithril Mined:")
                overlay!!.setAmount(0)

                if (mine.insideBorder(bot)){
                    SystemLogger.log("Inside mining zone, switching to MINING state")
                    ladderSwitch = false
                    state = State.MINING
                } else {
                    SystemLogger.log("Not inside mining zone, switching to TO_MINE state")
                    state = State.TO_MINE
                }
            }

            State.MINING -> {
                SystemLogger.log("Current state: MINING")
                if(bot.inventory.freeSlots() == 0){
                    SystemLogger.log("Inventory full, switching to TO_BANK state")
                    state = State.TO_BANK
                }
                if(!mine.insideBorder(bot)){
                    SystemLogger.log("Bot not inside mine, walking to mine")
                    scriptAPI.walkTo(mine.randomLoc)
                } else {
                    SystemLogger.log("Mining Mithril...")

                    // Find nearest Mithril ore rock based on the defined IDs
                    val rock = scriptAPI.getNearestNode("rocks", true)
                    if (rock != null && mithrilOreIds.contains(rock.id)) {
                        SystemLogger.log("Found Mithril ore rock: ID ${rock.id}")
                        rock.interaction?.handle(bot, rock.interaction[0])
                    } else {
                        SystemLogger.log("No Mithril ore rock nearby.")
                    	scriptAPI.walkTo(mine.randomLoc)
                    }
                }
                overlay!!.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                    bot.inventory.getAmount(Items.MITHRIL_ORE_448) + mithrilAmount)
            }

            State.TO_BANK -> {
				if(bank.insideBorder(bot)){
					val bank = scriptAPI.getNearestNode("bank booth",true)
					if(bank != null) {
						bot.pulseManager.run(object : BankingPulse(this, bank){
							override fun pulse(): Boolean {
								state = State.BANKING
								return super.pulse()
							}
						})
					}

				} else {
					if(!ladderSwitch) {
						val ladder = scriptAPI.getNearestNode(30941, true)
						ladder ?: scriptAPI.walkTo(bottomLadder.randomLoc).also { return }
						ladder?.interaction?.handle(bot, ladder.interaction[0]).also { ladderSwitch = true }
					} else {
						if (!bank.insideBorder(bot)) scriptAPI.walkTo(bank.randomLoc).also { return }
					}
				}
			}

			State.BANKING -> {
				mithrilAmount += bot.inventory.getAmount(Items.MITHRIL_ORE_447)
				mithrilAmount += bot.inventory.getAmount(Items.MITHRIL_ORE_448)
				scriptAPI.bankItem(Items.MITHRIL_ORE_447)
				scriptAPI.bankItem(Items.MITHRIL_ORE_448)
				state = State.TO_MINE
			}

			State.TO_MINE -> {
				if(ladderSwitch){
					if(!topLadder.insideBorder(bot.location)){
						scriptAPI.walkTo(topLadder.randomLoc)
					} else {
						val ladder = scriptAPI.getNearestNode("Ladder",true)
						if(ladder != null){
							ladder.interaction.handle(bot,ladder.interaction[0])
							ladderSwitch = false
						} else {
							scriptAPI.walkTo(topLadder.randomLoc)
						}
					}
				} else {
					if(!mine.insideBorder(bot)){
						scriptAPI.walkTo(mine.randomLoc)
					} else {
						state = State.MINING
					}
				}
			}

			State.TO_GE -> {
				scriptAPI.teleportToGE()
				state = State.SELLING
			}

			State.SELLING -> {
				scriptAPI.sellOnGE(Items.MITHRIL_ORE_447)
				scriptAPI.sellOnGE(Items.MITHRIL_ORE_448)
				state = State.GO_BACK
			}

			State.GO_BACK -> {
				scriptAPI.teleport(bank.randomLoc)
				state = State.TO_MINE
			}
        }
    }
    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot,bank, DestinationFlag.OBJECT){
        override fun pulse(): Boolean {
            script.bot.faceLocation(bank.location)
            SystemLogger.log("Banking pulse in action")
            return true
        }
    }

    override fun newInstance(): Script {
        SystemLogger.log("Creating new instance of MithrilMiner script")
        val script = MithrilMiner()
        script.bot = SkillingBotAssembler().produce(SkillingBotAssembler.Wealth.POOR,bot.startLocation)
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
        SystemLogger.log("Initializing MithrilMiner with necessary equipment and skill level")
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING,55)  // Adjusted for Mithril mining
    }

}
