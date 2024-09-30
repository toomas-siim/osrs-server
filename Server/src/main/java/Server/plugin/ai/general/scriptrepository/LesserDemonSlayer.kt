package plugin.ai.general.scriptrepository

import core.game.system.SystemLogger
import core.game.world.map.Location
import plugin.ai.general.ScriptAPI

@PlayerCompatible
@ScriptName("Lesser Demon Slayer")
@ScriptDescription("Kills Lesser Demons in the Mage Tower. Start near the Mage Tower.")
@ScriptIdentifier("lesser_demon_slayer")
class LesserDemonSlayer : Script(){
    var state = State.INIT
    var demonCounter = 0
    var overlay: ScriptAPI.BottingOverlay? = null
    var startLocation = Location(0, 0, 0)
    var timer = 3

    override fun tick() {
        when(state){

            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Lesser Demons")
                overlay!!.setTaskLabel("Lesser Demons slain:")
                overlay!!.setAmount(0)
                state = State.KILLING
                startLocation = bot.location
            }

            State.KILLING -> {
                val lesserDemon = scriptAPI.getNearestNode("Lesser Demon")
                if (lesserDemon == null) {
                    scriptAPI.randomWalkTo(startLocation, 3)
                } else {
                    scriptAPI.attackNpcInRadius(bot, "Lesser Demon", 10)
                    demonCounter++
                    overlay!!.setAmount(demonCounter)
                }
            }

            State.IDLE -> {
                if (timer-- <= 0) {
                    state = State.KILLING
                }
            }

        }
    }

    override fun newInstance(): Script {
        return this
    }

    enum class State {
        IDLE,
        INIT,
        KILLING,
        RETURN
    }
}
