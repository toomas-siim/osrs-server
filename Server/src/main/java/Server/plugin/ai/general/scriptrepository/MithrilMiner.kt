override fun tick() {
    SystemLogger.log("Tick invoked with state: $state")

    // Log nearby items
    logNearbyItems()

    when(state){
        State.INIT -> {
            SystemLogger.log("Initializing script...")
            overlay = scriptAPI.getOverlay()
            ladderSwitch = true
            overlay!!.init()
            overlay!!.setTitle("Mining")
            overlay!!.setTaskLabel("Mithril Mined:")
            overlay!!.setAmount(0)

            if (mine.insideBorder(bot)){
                SystemLogger.log("Inside mining zone, switching to MINING state")
                ladderSwitch = false
                state = State.MINING
            } else {
                SystemLogger.log("Not inside mining zone, switching to TO_MINE state")
                state = State.TO_MINE
            }
        }

        State.MINING -> {
            SystemLogger.log("Current state: MINING")
            if(bot.inventory.freeSlots() == 0){
                SystemLogger.log("Inventory full, switching to TO_BANK state")
                state = State.TO_BANK
            }
            if(!mine.insideBorder(bot)){
                SystemLogger.log("Bot not inside mine, walking to mine")
                scriptAPI.walkTo(mine.randomLoc)
            } else {
                SystemLogger.log("Mining Mithril...")
                val rock = scriptAPI.getNearestNode("rocks",true)
                rock?.interaction?.handle(bot,rock.interaction[0])
            }
            overlay!!.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                bot.inventory.getAmount(Items.MITHRIL_ORE_448) + mithrilAmount)
        }

        // Other states remain the same
    }
}

// New function to log all nearby items
fun logNearbyItems() {
    val nearbyItems = scriptAPI.getNearbyItems() // Assuming such a method exists in your API
    if (nearbyItems != null && nearbyItems.isNotEmpty()) {
        SystemLogger.log("Nearby items:")
        for (item in nearbyItems) {
            SystemLogger.log("Item: ${item.name} (ID: ${item.id}) at ${item.location}")
        }
    } else {
        SystemLogger.log("No nearby items found.")
    }
}
