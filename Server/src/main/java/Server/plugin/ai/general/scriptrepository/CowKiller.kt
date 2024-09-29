package plugin.ai.general.scriptrepository

import core.game.system.SystemLogger
import core.game.world.map.Location
import core.tools.Items
import plugin.ai.general.ScriptAPI
import core.game.world.map.zone.ZoneBorders

@PlayerCompatible
@ScriptName("Cow Killer")
@ScriptDescription("Kills cows and loots cowhides. Start in any cow area.")
@ScriptIdentifier("cow_killer")
class CowKiller : Script() {
    var state = State.INIT
    var cowCounter = 0
    var overlay: ScriptAPI.BottingOverlay? = null
    var timer = 3
    var lootCowhide = false

    // Define the key zones
    val cowZone = ZoneBorders(3242, 3254, 3265, 3296)
    val bankZone = ZoneBorders(3208, 3217, 3210, 3220)

    override fun tick() {
        when (state) {

            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Cows")
                overlay!!.setTaskLabel("Cows KO'd:")
                overlay!!.setAmount(0)
                state = State.CONFIG
                bot.dialogueInterpreter.sendOptions("Loot Cowhides?", "Yes", "No")
                bot.dialogueInterpreter.addAction { player, button ->
                    lootCowhide = button == 2
                    state = State.KILLING
                }
            }

            State.KILLING -> {
                val cow = scriptAPI.getNearestNode("Cow")
                if (cow == null) {
                    scriptAPI.randomWalkTo(bot.location, 3)
                } else {
                    scriptAPI.attackNpcInRadius(bot, "Cow", 10)
                    if (lootCowhide) {
                        state = State.IDLE
                        timer = 4
                        SystemLogger.log("Going to idle state")
                    }
                    cowCounter++
                    overlay!!.setAmount(cowCounter)
                }
            }

            State.IDLE -> {
                if (timer-- <= 0) {
                    state = State.LOOTING
                }
            }

            State.LOOTING -> {
                scriptAPI.takeNearestGroundItem(Items.COWHIDE_1739)
                state = if (bot.inventory.getAmount(Items.COWHIDE_1739) > 22) {
                    State.TO_BANK
                } else {
                    State.KILLING
                }
            }

            State.TO_BANK -> {
                if (cowZone.insideBorder(bot)) {
                    scriptAPI.walkTo(Location.create(3253, 3267, 0))
                } else {
                    handleGateOrWalkToBank()
                }
            }

            State.BANKING -> {
                if (bankZone.insideBorder(bot)) {
                    val bank = scriptAPI.getNearestNode(36786, true)
                    bot.pulseManager.run(object : MovementPulse(bot, bank, DestinationFlag.OBJECT) {
                        override fun pulse(): Boolean {
                            scriptAPI.bankItem(Items.COWHIDE_1739)
                            state = if (bot.bank.getAmount(Items.COWHIDE_1739) > 75) {
                                State.TELE_GE
                            } else {
                                State.BACK_TO_COWS
                            }
                            return true
                        }
                    })
                }
            }

            State.BACK_TO_COWS -> {
                scriptAPI.walkTo(cowZone.randomLoc)
                state = State.KILLING
            }

            State.TELE_GE -> {
                scriptAPI.teleport(Location.create(3165, 3482, 0))
                state = State.SELL_GE
            }

            State.SELL_GE -> {
                scriptAPI.sellOnGE(Items.COWHIDE_1739)
                state = State.TELE_LUM
            }

            State.TELE_LUM -> {
                scriptAPI.teleport(Location.create(3222, 3218, 0))
                state = State.BACK_TO_COWS
            }
        }
    }

    private fun handleGateOrWalkToBank() {
        val closedGate = scriptAPI.getNearestNode(15516, true)
        if (closedGate != null && closedGate.location.withinDistance(bot.location, 2)) {
            closedGate.interaction.handle(bot, closedGate.interaction[0])
        } else {
            scriptAPI.walkTo(bankZone.randomLoc)
            state = State.BANKING
        }
    }

    override fun newInstance(): Script {
        return this
    }

    enum class State {
        INIT,
        KILLING,
        IDLE,
        LOOTING,
        TO_BANK,
        BANKING,
        BACK_TO_COWS,
        TELE_GE,
        SELL_GE,
        TELE_LUM
    }
}
