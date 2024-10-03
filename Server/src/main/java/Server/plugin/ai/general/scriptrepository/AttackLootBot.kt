package plugin.ai.general.scriptrepository

import core.game.node.item.Item
import core.game.system.SystemLogger
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
    var entityToKill = "" // User input for entity name to kill

    override fun tick() {
        when (state) {
            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Attack and Loot")
                overlay!!.setTaskLabel("Kills:")
                overlay!!.setAmount(0)
                state = State.CONFIG

                bot.dialogueInterpreter.sendOptions("Which entity should I attack?", "Goblin", "Zombie", "Cow")
                bot.dialogueInterpreter.addAction { player, button ->
                    entityToKill = when (button) {
                        1 -> "goblin"
                        2 -> "zombie"
                        3 -> "cow"
                        else -> ""
                    }
                    state = State.CONFIG_LOOTING_OPTIONS
                }
                startLocation = bot.location
            }

            State.CONFIG_LOOTING_OPTIONS -> {
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
            }

            State.ATTACKING -> {
                val nearbyEntities = scriptAPI.getNearbyEntities(bot)
                if (nearbyEntities.isEmpty()) {
                    scriptAPI.randomWalkTo(startLocation, 3)
                } else {
                    for (entity in nearbyEntities) {
                        val entityName = entity.name.toLowerCase()
                        if (entityName.contains(entityToKill)) {
                            val randomChance = Random().nextInt(10)
                            if (randomChance == 0) {
                                scriptAPI.attackNpc(bot, entity.id)
                                break
                            }
                        }
                    }
                    if (lootBones || lootItems) {
                        state = State.IDLE
                        idleTimer = 4
                        SystemLogger.log("Going to idle state after attacking")
                    }
                    killCounter += nearbyEntities.size
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
                val inventoryItems = bot.inventory.toArray()
                for (item in inventoryItems) {
                    item?.let {
                        when (it.id) {
                            Items.BONES_526, Items.BIG_BONES_532, Items.BABYDRAGON_BONES_534, Items.DRAGON_BONES_536 -> {
                                // Bury using BoneBuryingOptionPlugin's logic
                                buryBones(it)
                            }
                            else -> {
                                // Handle other items or do nothing
                            }
                        }
                    }
                }
                state = State.ATTACKING
            }
        }
    }

    private fun buryBones(item: Item) {
        // Use the logic from BoneBuryingOptionPlugin
        val bone = Bones.forId(item.id) ?: Bones.BONES
        val player = bot // Assume bot as player reference
        if (player.getAttribute("delay:bury", -1) > GameWorld.getTicks()) {
            return
        }
        player.setAttribute("delay:bury", GameWorld.getTicks() + 2)
        player.lock(2)
        player.animate(Animation(827))
        player.getPacketDispatch().sendMessage("You dig a hole in the ground...")
        player.getAudioManager().send(Audio(2738, 10, 1))

        GameWorld.getPulser().submit(object : Pulse(2, player) {
            override fun pulse(): Boolean {
                player.getPacketDispatch().sendMessage("You bury the bones.")
                player.getSkills().addExperience(Skills.PRAYER, bone.getExperience(), true)
                player.getInventory().remove(item) // Remove item after burying
                return true
            }
        })
    }

    override fun newInstance(): Script {
        return this
    }

    enum class State {
        INIT,
        CONFIG,
        CONFIG_LOOTING_OPTIONS,
        ATTACKING,
        IDLE,
        LOOTING,
        BURYING
    }
}
