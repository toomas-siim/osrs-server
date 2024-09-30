package plugin.ai.general.scriptrepository

import core.game.system.SystemLogger
import core.game.world.map.Location
import core.tools.Items
import core.game.world.update.flag.context.Animation
import plugin.ai.general.ScriptAPI

@PlayerCompatible
@ScriptName("Attack Loot Bot")
@ScriptDescription("Attacks any entity around it, buries bones, and loots nearby items.")
@ScriptIdentifier("attack_loot_bot")
class AttackLootBot : Script() {
    var state = State.INIT
    var killCounter = 0
    var overlay: ScriptAPI.BottingOverlay? = null
    var startLocation = Location(0, 0, 0)
    var lootBones = true
    var lootItems = true
    var idleTimer = 3

    override fun tick() {
        when (state) {
            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Attack and Loot")
                overlay!!.setTaskLabel("Kills:")
                overlay!!.setAmount(0)
                state = State.CONFIG
                bot.dialogueInterpreter.sendOptions("Loot bones and other items?", "Yes", "No")
                bot.dialogueInterpreter.addAction { player, button ->
                    if (button == 1) {
                        lootBones = true
                        lootItems = true
                    } else {
                        lootBones = false
                        lootItems = false
                    }
                    state = State.ATTACKING
                }
                startLocation = bot.location
            }

            State.ATTACKING -> {
                val target = scriptAPI.getNearestNode("Any")
                if (target == null) {
                    scriptAPI.randomWalkTo(startLocation, 3)
                } else {
                    scriptAPI.attackNpcsInRadius(bot, 10)
                    if (lootBones || lootItems) {
                        state = State.IDLE
                        idleTimer = 4
                        SystemLogger.log("Going to idle state")
                    }
                    killCounter++
                    overlay!!.setAmount(killCounter)
                }
            }

            State.IDLE -> {
                if (idleTimer-- <= 0) {
                    state = State.LOOTING
                }
            }

            State.LOOTING -> {
                if (lootBones) {
                    scriptAPI.takeNearestGroundItem(Items.BONES_526)
                    scriptAPI.takeNearestGroundItem(Items.BIG_BONES_532)
                    scriptAPI.takeNearestGroundItem(Items.BABYDRAGON_BONES_534)
                    scriptAPI.takeNearestGroundItem(Items.DRAGON_BONES_536)
                }
                if (lootItems) {
                    scriptAPI.takeNearestGroundItem(Items.FEATHER_314)
                    scriptAPI.takeNearestGroundItem(Items.COINS_995)
                    scriptAPI.takeNearestGroundItem(Items.LOBSTER_379)
                }
                state = State.BURYING
            }

            State.BURYING -> {
                val inventory = bot.inventory
                for (i in 0 until inventory.size()) {
                    val item = inventory.getItem(i)
                    item?.let {
                        when (it.id) {
                            Items.BONES_526, Items.BIG_BONES_532, Items.BABYDRAGON_BONES_534, Items.DRAGON_BONES_536 -> {
                                inventory.remove(item)
                                bot.visualize(Animation(827)) // Bury animation
                            }
                        }
                    }
                }
                state = State.ATTACKING
            }
        }
    }

    override fun newInstance(): Script {
        return this
    }

    enum class State {
        INIT,
        CONFIG,
        ATTACKING,
        IDLE,
        LOOTING,
        BURYING
    }
}
