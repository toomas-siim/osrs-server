package plugin.ai.general.scriptrepository

import core.game.node.Node
import core.game.node.item.Item
import core.game.system.SystemLogger
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler

@PlayerCompatible
@ScriptName("Simple Logger")
@ScriptDescription("Logs all items and objects around the player.")
@ScriptIdentifier("simple_logger")
class SimpleLogger() : Script() {

    override fun tick() {
        SystemLogger.log("Tick invoked: Logging nearby entities.")

        // Log nearby items
        logNearbyItems()

        // Log nearby objects
        logNearbyObjects()
    }

    // Function to log all nearby items
    fun logNearbyItems() {
        val nearbyItems = scriptAPI.getNearbyItems() // Replace with actual method to get nearby items
        if (nearbyItems != null && nearbyItems.isNotEmpty()) {
            SystemLogger.log("Nearby items:")
            for (item in nearbyItems) {
                SystemLogger.log("Item: ${item.name} (ID: ${item.id}) at ${item.location}")
            }
        } else {
            SystemLogger.log("No nearby items found.")
        }
    }

    // Function to log all nearby objects
    fun logNearbyObjects() {
        val nearbyObjects = scriptAPI.getNearbyNodes() // Replace with actual method to get nearby objects
        if (nearbyObjects != null && nearbyObjects.isNotEmpty()) {
            SystemLogger.log("Nearby objects:")
            for (obj in nearbyObjects) {
                SystemLogger.log("Object: ${obj.name} (ID: ${obj.id}) at ${obj.location}")
            }
        } else {
            SystemLogger.log("No nearby objects found.")
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
