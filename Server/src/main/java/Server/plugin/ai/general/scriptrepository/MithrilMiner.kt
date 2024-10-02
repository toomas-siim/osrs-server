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
    val mine = ZoneBorders(2967,9733,2994,9743)
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
                    val rock = scriptAPI.getNearestNode("rocks",true)
                    // @TODO: Is mithril ids ?
                    rock?.interaction?.handle(bot,rock.interaction[0])
                }
                overlay!!.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                    bot.inventory.getAmount(Items.MITHRIL_ORE_448) + mithrilAmount)
            }

            State.TO_BANK -> {
                SystemLogger.log("Current state: TO_BANK")
                if(bank.insideBorder(bot)){
                    SystemLogger.log("Inside bank zone, interacting with the bank")
                    val bank = scriptAPI.getNearestNode("bank booth",true)
                    if(bank != null) {
                        bot.pulseManager.run(object : BankingPulse(this, bank){
                            override fun pulse(): Boolean {
                                state = State.BANKING
                                SystemLogger.log("Switched to BANKING state")
                                return super.pulse()
                            }
                        })
                    }

                } else {
                    if(!ladderSwitch) {
                        SystemLogger.log("Finding and using the ladder to go up")
                        val ladder = scriptAPI.getNearestNode(30941, true)
                        ladder ?: scriptAPI.walkTo(bottomLadder.randomLoc).also { return }
                        ladder?.interaction?.handle(bot, ladder.interaction[0]).also { ladderSwitch = true }
                    } else {
                        if (!bank.insideBorder(bot)) scriptAPI.walkTo(bank.randomLoc).also { return }
                    }
                }
            }

            State.BANKING -> {
                SystemLogger.log("Current state: BANKING")
                mithrilAmount += bot.inventory.getAmount(Items.MITHRIL_ORE_447) +
                                 bot.inventory.getAmount(Items.MITHRIL_ORE_448)
                SystemLogger.log("Banking Mithril ore...")
                scriptAPI.bankItem(Items.MITHRIL_ORE_447)
                scriptAPI.bankItem(Items.MITHRIL_ORE_448)
                state = State.TO_MINE
                SystemLogger.log("Switching to TO_MINE state")
            }

            State.TO_MINE -> {
                SystemLogger.log("Current state: TO_MINE")
                if(ladderSwitch){
                    if(!topLadder.insideBorder(bot.location)){
                        SystemLogger.log("Walking to top ladder zone")
                        scriptAPI.walkTo(topLadder.randomLoc)
                    } else {
                        SystemLogger.log("Interacting with ladder to go down")
                        val ladder = scriptAPI.getNearestNode("Ladder",true)
                        if(ladder != null){
                            ladder.interaction.handle(bot,ladder.interaction[0])
                            ladderSwitch = false
                        } else {
                            scriptAPI.walkTo(topLadder.randomLoc)
                        }
                    }
                } else {
                    if(!mine.insideBorder(bot)){
                        SystemLogger.log("Walking to mine zone")
                        scriptAPI.walkTo(mine.randomLoc)
                    } else {
                        SystemLogger.log("Switching to MINING state")
                        state = State.MINING
                    }
                }
            }

            State.TO_GE -> {
                SystemLogger.log("Teleporting to Grand Exchange...")
                scriptAPI.teleportToGE()
                state = State.SELLING
            }

            State.SELLING -> {
                SystemLogger.log("Selling Mithril ore...")
                scriptAPI.sellOnGE(Items.MITHRIL_ORE_447)
                scriptAPI.sellOnGE(Items.MITHRIL_ORE_448)
                state = State.GO_BACK
            }

            State.GO_BACK -> {
                SystemLogger.log("Teleporting back to the bank...")
                scriptAPI.teleport(bank.randomLoc)
                state = State.TO_MINE
            }

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
