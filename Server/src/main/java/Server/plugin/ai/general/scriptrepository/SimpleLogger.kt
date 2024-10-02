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

        logNearbyNodes()
    }

    // Function to log all nearby items
    fun logNearbyNodes() {
        val nearbyItems = scriptAPI.getNearbyEntities(bot)
        if (nearbyItems.isNotEmpty()) {
            SystemLogger.log("Nearby node:")
            for (item in nearbyItems) {
                SystemLogger.log("Node: ${item.name} (ID: ${item.id}) at ${item.location}")
                SystemLogger.log("${item}")
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
