package plugin.ai.general.scriptrepository

import core.game.interaction.DestinationFlag
import core.game.interaction.MovementPulse
import core.game.node.Node
import core.game.node.item.Item
import core.game.system.SystemLogger
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
                SystemLogger.log("State: INIT")
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Mining")
                overlay!!.setTaskLabel("Coal Mined:")
                overlay!!.setAmount(0)

                // Initialize lastInventoryCount
                lastInventoryCount = bot.inventory.getAmount(Items.COAL_453)
                miningInProgress = false

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
                val currentInventoryCount = bot.inventory.getAmount(Items.COAL_453)
                SystemLogger.log("Current inventory count: $currentInventoryCount, Last inventory count: $lastInventoryCount")

                if (bot.inventory.freeSlots() == 0) {
                    SystemLogger.log("Inventory full. Switching to TO_BANK state.")
                    state = State.TO_BANK
                } else if (!mine.insideBorder(bot)) {
                    SystemLogger.log("Bot is not inside the mine. Walking to mine.")
                    scriptAPI.walkTo(mine.randomLoc)
                } else if (!miningInProgress) {
                    val rock = getNearestCoalRock()
                    if (rock != null) {
                        SystemLogger.log("Found coal rock with ID: ${rock.id}. Starting to mine.")
                        rock.interaction.handle(bot, rock.interaction[0])
                        miningInProgress = true
                    } else {
                        SystemLogger.log("No coal rock found. Walking to a random location in the mine.")
                        // Walk to a random location in the mine to find coal rocks
                        scriptAPI.walkTo(mine.randomLoc)
                    }
                } else if (currentInventoryCount > lastInventoryCount) {
                    // An ore has been mined
                    SystemLogger.log("Ore mined. Inventory count increased from $lastInventoryCount to $currentInventoryCount.")
                    lastInventoryCount = currentInventoryCount
                    miningInProgress = false
                }
                // Update overlay
                overlay!!.setAmount(currentInventoryCount + coalAmount)
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


