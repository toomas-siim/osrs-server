package plugin.ai.general.scriptrepository

import core.game.system.SystemLogger
import core.game.world.map.Location
import core.tools.Items
import plugin.ai.general.ScriptAPI
import core.game.world.map.zone.ZoneBorders

@PlayerCompatible
@ScriptName("Cow Killer")
@ScriptDescription("Kills cows, loots cowhides, and banks them. Start in any cow area.")
@ScriptIdentifier("cow_killer")
class CowKiller : Script() {
    var state = State.INIT
    var cowCounter = 0
    var overlay: ScriptAPI.BottingOverlay? = null
    var lootCowhide = true

    // Define the key zones
    val cowZone = ZoneBorders(3242, 3254, 3265, 3296) // Cow pen in Lumbridge
    val bankZone = ZoneBorders(3208, 3217, 3210, 3220) // Lumbridge bank

    override fun tick() {
        when (state) {
            State.INIT -> {
                // Initialize overlay to track kills
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Cows")
                overlay!!.setTaskLabel("Cows KO'd:")
                overlay!!.setAmount(0)

                // Configure looting cowhides
                state = State.CONFIG
                bot.dialogueInterpreter.sendOptions("Loot Cowhides?", "Yes", "No")
                bot.dialogueInterpreter.addAction { player, button ->
                    lootCowhide = button == 2
                    state = State.KILLING
                }
            }

            State.KILLING -> {
                // Use the same attack method as in ChickenKiller
                scriptAPI.attackNpcInRadius(bot, "Cow", 10) // Attacks a cow within a 10-tile radius
                state = State.LOOTING
            }

            State.LOOTING -> {
                if (lootCowhide) {
                    scriptAPI.takeNearestGroundItem(Items.COWHIDE_1739)
                }
                cowCounter++
                overlay!!.setAmount(cowCounter)

                // Check if inventory is full
                state = if (bot.inventory.getAmount(Items.COWHIDE_1739) >= 28) {
                    State.TO_BANK
                } else {
                    State.KILLING
                }
            }

            State.TO_BANK -> {
                scriptAPI.walkTo(bankZone.randomLoc) // Walk to the bank

                // Handle gate opening if necessary
                val closedGate = scriptAPI.getNearestNode(15516, true)
                if (closedGate != null && closedGate.location.withinDistance(bot.location, 2)) {
                    closedGate.interaction.handle(bot, closedGate.interaction[0])
                } else {
                    state = State.BANKING
                }
            }

            State.BANKING -> {
                if (bankZone.insideBorder(bot)) {
                    val bank = scriptAPI.getNearestNode(36786, true)
                    // Simplified banking logic
                    if (bank != null) {
                        scriptAPI.bankItem(Items.COWHIDE_1739)
                        state = State.BACK_TO_COWS
                    }
                }
            }

            State.BACK_TO_COWS -> {
                scriptAPI.walkTo(cowZone.randomLoc)
                state = State.KILLING
            }
        }
    }

    override fun newInstance(): Script {
        return this
    }

    enum class State {
        INIT,
        KILLING,
        LOOTING,
        TO_BANK,
        BANKING,
        BACK_TO_COWS
    }
}
