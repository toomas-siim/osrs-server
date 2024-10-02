package plugin.ai.general.scriptrepository

import core.game.node.Node
import core.game.node.item.Item
import core.game.system.SystemLogger
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler

@PlayerCompatible
@ScriptName("Simple Logger")
@ScriptDescription("Logs all unique item and object IDs around the player.")
@ScriptIdentifier("simple_logger")
class SimpleLogger() : Script() {

    private val loggedIds = mutableSetOf<Int>() // Set to store unique IDs

    override fun tick() {
        SystemLogger.log("Tick invoked: Logging unique nearby entities.")

        // Reset the set of logged IDs each tick
        loggedIds.clear()

        logInventory()
        logUniqueNearbyNodes()
    }

	fun logInventory() {
		for (item in bot.inventory.toArray()) {
			SystemLogger.log("Item: ${item.name} (ID: ${item.id})")
		}
	}

    // Function to log all nearby items and objects with unique IDs
    fun logUniqueNearbyNodes() {
        val nearbyItems = scriptAPI.getNearbyEntities(bot)
        if (nearbyItems.isNotEmpty()) {
            for (item in nearbyItems) {
                if (item.id !in loggedIds) {
                    loggedIds.add(item.id) // Add ID to the set if not already present
                    SystemLogger.log("Unique Node: ${item.name} (ID: ${item.id}) at ${item.location}")
                }
            }
        } else {
            SystemLogger.log("No nearby items found.")
        }
    }

    override fun newInstance(): Script {
        SystemLogger.log("Creating new instance of SimpleLogger script")
        return SimpleLogger()
    }

    init {
        SystemLogger.log("Initializing SimpleLogger script.")
    }
}
