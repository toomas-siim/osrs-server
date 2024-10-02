package plugin.ai.general.scriptrepository

import core.game.interaction.DestinationFlag
import core.game.interaction.MovementPulse
import core.game.node.Node
import core.game.node.item.Item
import core.game.world.map.zone.ZoneBorders
import core.tools.Items
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler
import core.game.node.entity.skill.Skills
import core.game.system.SystemLogger

@PlayerCompatible
@ScriptName("Falador Mithril Miner")
@ScriptDescription("Start in Falador East Bank with a pick equipped","or in your inventory.")
@ScriptIdentifier("fally_mithril")
class MithrilMiner() : Script() {
    var state = State.INIT
    var ladderSwitch = false

    val bottomLadder = ZoneBorders(3016,9736,3024,9742)
    val topLadder = ZoneBorders(3016,3336,3022,3342)
    val mine = ZoneBorders(3027,9733,3054,9743)
    val bank = ZoneBorders(3009,3355,3018,3358)
    var overlay: ScriptAPI.BottingOverlay? = null
    var mithrilAmount = 0

    override fun tick() {
        SystemLogger.log("Tick invoked with state: $state")

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
                    logNearbyItems()
                    val rock = scriptAPI.getNearestNode("rocks",true)
                    //rock?.interaction?.handle(bot,rock.interaction[0])
                }
                overlay!!.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                    bot.inventory.getAmount(Items.MITHRIL_ORE_448) + mithrilAmount)
            }

            // Other states remain the same
        }
    }

    // New function to log all nearby items
    fun logNearbyItems() {
        val nearbyItems = scriptAPI.getNearbyEntities(bot)
        if (nearbyItems != null && nearbyItems.isNotEmpty()) {
            SystemLogger.log("Nearby items:")
            for (item in nearbyItems) {
                SystemLogger.log("Item: ${item.name} (ID: ${item.id}) at ${item.location}")
            }
        } else {
            SystemLogger.log("No nearby items found.")
        }
    }

    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot,bank, DestinationFlag.OBJECT){
        override fun pulse(): Boolean {
            script.bot.faceLocation(bank.location)
            SystemLogger.log("Banking pulse in action")
            return true
        }
    }

    override fun newInstance(): Script {
        SystemLogger.log("Creating new instance of MithrilMiner script")
        val script = MithrilMiner()
        script.bot = SkillingBotAssembler().produce(SkillingBotAssembler.Wealth.POOR,bot.startLocation)
        return script
    }

    enum class State {
        MINING,
        TO_MINE,
        TO_BANK,
        BANKING,
        TO_GE,
        SELLING,
        GO_BACK,
        INIT
    }

    init {
        SystemLogger.log("Initializing MithrilMiner with necessary equipment and skill level")
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING,55)  // Adjusted for Mithril mining
    }

}
