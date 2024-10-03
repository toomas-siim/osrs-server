package plugin.ai.general.scriptrepository

import java.util.Random
import core.game.system.SystemLogger
import core.game.world.map.Location
import core.tools.Items
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics
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

                // Predefined options for user to choose target entity
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
                val nearbyEntities = scriptAPI.getNearbyEntities(bot) // Fetch nearby entities from scriptAPI
                if (nearbyEntities.isEmpty()) {
                    // No nearby entities, walk back to the start location
                    scriptAPI.randomWalkTo(startLocation, 3)
                } else {
                    // Attack nearby entities that match the user's input
                    for (entity in nearbyEntities) {
                        val entityName = entity.name.toLowerCase() // Convert entity name to lowercase
                        if (entityName.contains(entityToKill)) { // Check if entity name contains the user's input
                            // 1/10 chance of attacking
                            val randomChance = Random().nextInt(10) // Generates a number between 0 and 9
                            if (randomChance == 0) { // 1/10 chance (when randomChance == 0)
                                scriptAPI.attackNpc(bot, entity.id) // Attack entity
                                break // Optionally stop after attacking the first matching entity
                            }
                        }
                    }
                    // If looting is enabled, transition to IDLE state
                    if (lootBones || lootItems) {
                        state = State.IDLE
                        idleTimer = 4
                        SystemLogger.log("Going to idle state after attacking")
                    }
                    killCounter += nearbyEntities.size // Increment kill counter by number of attacked entities
                    overlay!!.setAmount(killCounter) // Update overlay with new kill count
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
                                bot.inventory.remove(item)
                                bot.visualize(Animation(827), Graphics(0)) // Bury animation with no graphics effect
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
