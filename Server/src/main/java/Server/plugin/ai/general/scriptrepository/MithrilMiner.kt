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
        when(state){

            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                ladderSwitch = true
                overlay!!.init()
                overlay!!.setTitle("Mining")
                overlay!!.setTaskLabel("Mithril Mined:")
                overlay!!.setAmount(0)

                if (mine.insideBorder(bot)){
                    ladderSwitch = false
                    state = State.MINING
                } else {
                    state = State.TO_MINE
                }
            }

            State.MINING -> {
                if(bot.inventory.freeSlots() == 0){
                    state = State.TO_BANK
                }
                if(!mine.insideBorder(bot)){
                    scriptAPI.walkTo(mine.randomLoc)
                } else {
                    val rock = scriptAPI.getNearestNode("mithril rocks",true)
                    rock?.interaction?.handle(bot,rock.interaction[0])
                }
                overlay!!.setAmount(bot.inventory.getAmount(Items.MITHRIL_ORE_447) + mithrilAmount)
            }

            State.TO_BANK -> {
                if(bank.insideBorder(bot)){
                    val bank = scriptAPI.getNearestNode("bank booth",true)
                    if(bank != null) {
                        bot.pulseManager.run(object : BankingPulse(this, bank){
                            override fun pulse(): Boolean {
                                state = State.BANKING
                                return super.pulse()
                            }
                        })
                    }

                } else {
                    if(!ladderSwitch) {
                        val ladder = scriptAPI.getNearestNode(30941, true)
                        ladder ?: scriptAPI.walkTo(bottomLadder.randomLoc).also { return }
                        ladder?.interaction?.handle(bot, ladder.interaction[0]).also { ladderSwitch = true }
                    } else {
                        if (!bank.insideBorder(bot)) scriptAPI.walkTo(bank.randomLoc).also { return }
                    }
                }
            }

            State.BANKING -> {
                mithrilAmount += bot.inventory.getAmount(Items.MITHRIL_ORE_447)
                scriptAPI.bankItem(Items.MITHRIL_ORE_447)
                state = State.TO_MINE
            }

            State.TO_MINE -> {
                if(ladderSwitch){
                    if(!topLadder.insideBorder(bot.location)){
                        scriptAPI.walkTo(topLadder.randomLoc)
                    } else {
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
                        scriptAPI.walkTo(mine.randomLoc)
                    } else {
                        state = State.MINING
                    }
                }
            }

            State.TO_GE -> {
                scriptAPI.teleportToGE()
                state = State.SELLING
            }

            State.SELLING -> {
                scriptAPI.sellOnGE(Items.MITHRIL_ORE_447)
                state = State.GO_BACK
            }

            State.GO_BACK -> {
                scriptAPI.teleport(bank.randomLoc)
                state = State.TO_MINE
            }

        }
    }

    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot,bank, DestinationFlag.OBJECT){
        override fun pulse(): Boolean {
            script.bot.faceLocation(bank.location)
            return true
        }
    }

    override fun newInstance(): Script {
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
        equipment.add(Item(Items.IRON_PICKAXE_1267))
        skills.put(Skills.MINING,55)  // Adjusted for Mithril mining
    }

}
