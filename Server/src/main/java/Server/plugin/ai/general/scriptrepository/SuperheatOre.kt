package plugin.ai.general.scriptrepository

import core.game.interaction.DestinationFlag
import core.game.interaction.MovementPulse
import core.game.node.Node
import core.game.node.entity.skill.Skills
import core.game.system.SystemLogger
import core.tools.Items
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler
import core.game.node.entity.player.Player

@PlayerCompatible
@ScriptName("Superheat Ore")
@ScriptDescription("Superheats the requested ore using nearby bankers.")
@ScriptIdentifier("superheat_ore")
class SuperheatOre(private val oreId: Int, private val oreAmount: Int) : Script() {
    var state = State.INIT
    var superheatedOre = 0
    var overlay: ScriptAPI.BottingOverlay? = null
    val superheatSpellId = 1173 // ID for the Superheat spell
    val natureRuneId = Items.NATURE_RUNE_561
    val fireRuneId = Items.FIRE_RUNE_554

    override fun tick() {
        when (state) {

            State.INIT -> {
                SystemLogger.log("State: INIT")
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Superheating Ore")
                overlay!!.setTaskLabel("Ore Superheated:")
                overlay!!.setAmount(0)

                SystemLogger.log("Looking for a nearby banker.")
                val banker = scriptAPI.getNearestNode("Banker", false)
                if (banker != null) {
                    SystemLogger.log("Banker found. Switching to BANKING state.")
                    state = State.BANKING
                } else {
                    SystemLogger.log("No banker found. Walking to find a banker.")
                    scriptAPI.walkTo(scriptAPI.getNearestNode("Bank booth", true)?.location ?: return)
                }
            }

            State.BANKING -> {
                SystemLogger.log("State: BANKING")
                scriptAPI.withdraw(oreId, oreAmount)
                scriptAPI.withdraw(natureRuneId, oreAmount)

                if (bot.skills.getStaticLevel(Skills.MAGIC) >= 43) {
                    SystemLogger.log("Enough Magic level for Superheat. Switching to SUPERHEATING state.")
                    state = State.SUPERHEATING
                } else {
                    SystemLogger.log("Not enough Magic level to cast Superheat. Stopping script.")
                    stop()
                }
            }

            State.SUPERHEATING -> {
                SystemLogger.log("State: SUPERHEATING")
                val ore = bot.inventory.getAmount(oreId)
                val natureRunes = bot.inventory.getAmount(natureRuneId)
                val fireRunes = bot.inventory.getAmount(fireRuneId)

                if (ore > 0 && natureRunes > 0 && fireRunes >= 4) {
                    SystemLogger.log("Superheating ore. Inventory: $ore ores, $natureRunes nature runes, $fireRunes fire runes.")
                    bot.spellbook.cast(bot, superheatSpellId, oreId)
                    superheatedOre++
                    overlay!!.setAmount(superheatedOre)
                } else {
                    SystemLogger.log("Out of resources for Superheating. Returning to BANKING state.")
                    state = State.BANKING
                }
            }
        }
    }

    override fun newInstance(): Script {
        return SuperheatOre(oreId, oreAmount)
    }

    enum class State {
        INIT,
        BANKING,
        SUPERHEATING
    }
}
