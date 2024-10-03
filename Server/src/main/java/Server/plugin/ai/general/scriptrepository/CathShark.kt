package plugin.ai.general.scriptrepository

import core.game.interaction.DestinationFlag
import core.game.interaction.MovementPulse
import core.game.node.item.Item
import core.game.system.task.Pulse
import core.game.world.GameWorld
import core.game.world.map.Location
import core.game.world.map.path.Pathfinder
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics
import core.tools.Items
import core.tools.RandomFunction
import plugin.ai.AIPlayer
import plugin.ai.general.ScriptAPI
import core.game.system.SystemLogger

import core.game.node.entity.skill.Skills
import kotlin.random.Random

@PlayerCompatible
@ScriptName("Catherby Sharks")
@ScriptDescription("Start in Catherby bank with a harpoon in your inventory.")
@ScriptIdentifier("cath_sharks")
class CathShark : Script() {
    private val ANIMATION = Animation(618) // Update the animation if sharks use a different one
    val offers = HashMap<Int, Int>()
    val limit = 2000
    var myCounter = 0

    /**
     * Represents the graphics to use.
     */
    private val GRAPHICS = Graphics(308, 100, 50)
    internal enum class Sets(val equipment: List<Item>) {
        SET_1(listOf(Item(2643), Item(9470), Item(10756), Item(10394), Item(88), Item(9793))),
        SET_2(listOf(Item(2643), Item(6585), Item(10750), Item(10394), Item(88), Item(9793))),
        SET_3(listOf(Item(9472), Item(9470), Item(10750), Item(10394), Item(88), Item(9786))),
        SET_4(listOf(Item(2639), Item(6585), Item(10752), Item(10394), Item(88), Item(9786))),
        SET_5(listOf(Item(2639), Item(9470), Item(10750), Item(10394), Item(88), Item(9784))),
        SET_6(listOf(Item(2639), Item(6585), Item(10750), Item(10394), Item(88), Item(9784)));

    }

    private var bots = 0
    private var sharkstopper = false
    var overlay: ScriptAPI.BottingOverlay? = null
    var fishCounter = 0

    private var state = State.INIT
    private var tick = 0
    override fun tick() {
        SystemLogger.log("Tick called. Current state: $state")
        when(state){

            State.INIT -> {
                SystemLogger.log("Initializing overlay.")
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Fishing")
                overlay!!.setTaskLabel("Sharks Caught:")
                overlay!!.setAmount(0)
                state = State.FIND_SPOT
                SystemLogger.log("State changed to FIND_SPOT")
            }

            State.BANKING -> {
                SystemLogger.log("Banking raw sharks.")
                fishCounter += bot.inventory.getAmount(Items.RAW_SHARK_383)
                fishCounter += bot.inventory.getAmount(Items.RAW_SHARK_384)
                scriptAPI.bankItem(Items.RAW_SHARK_383)
                scriptAPI.bankItem(Items.RAW_SHARK_384)
                state = State.IDLE
                SystemLogger.log("State changed to IDLE")
            }

            State.FISHING -> {
                SystemLogger.log("Fishing started.")
                val spot = scriptAPI.getNearestNode(313, false) // Use shark fishing spot ID
                if(spot == null){
                    state = State.IDLE
                    SystemLogger.log("No fishing spot found. State changed to IDLE")
                } else {
                    SystemLogger.log("Possible interactions with fishing spot:")
                    for (i in spot.interaction.getOptions()) {
                    	SystemLogger.log("Interaction ${i.getName()}")
                    }
                    spot.interaction.handle(bot, spot.interaction[0])
                    SystemLogger.log("Interacting with fishing spot using interaction 0.")
                }

                if(bot.inventory.isFull){
                    state = State.FIND_BANK
                    SystemLogger.log("Inventory is full. State changed to FIND_BANK")
                }
                overlay!!.setAmount(fishCounter + bot.inventory.getAmount(Items.RAW_SHARK_383))
                overlay!!.setAmount(fishCounter + bot.inventory.getAmount(Items.RAW_SHARK_384))
            }

            State.IDLE -> {
                SystemLogger.log("Bot is idle.")
                if (Random.nextBoolean()){
                    state = State.FIND_SPOT
                    SystemLogger.log("Randomly choosing to find a fishing spot. State changed to FIND_SPOT")
                }
                else if(myCounter++ >= RandomFunction.random(1,25)){
                    state = State.FIND_SPOT
                    SystemLogger.log("Counter triggered. State changed to FIND_SPOT")
                }
            }

            State.FIND_SPOT -> {
                SystemLogger.log("Searching for a fishing spot.")
                val spot = scriptAPI.getNearestNode(313, false) // Shark fishing spot ID
                if (spot != null) {
                    bot.walkingQueue.reset()
                    state = State.FISHING
                    SystemLogger.log("Fishing spot found. State changed to FISHING")
                } else {
                    SystemLogger.log("No spot found, walking to predefined locations.")
                    if (bot.location.x < 2837) {
                        Pathfinder.find(bot, Location.create(2837, 3435, 0)).walk(bot)
                    } else {
                        Pathfinder.find(bot, Location.create(2854, 3427, 0)).walk(bot)
                    }
                }
            }

            State.FIND_BANK -> {
                SystemLogger.log("Searching for nearest bank.")
                val bank = scriptAPI.getNearestGameObject(bot.location, 2213)
                class BankingPulse : MovementPulse(bot, bank, DestinationFlag.OBJECT) {
                    override fun pulse(): Boolean {
                        bot.faceLocation(bank!!.location)
                        state = State.BANKING
                        SystemLogger.log("Arrived at the bank. State changed to BANKING")
                        return true
                    }
                }
                if(bank != null){
                    bot.pulseManager.run(BankingPulse())
                    SystemLogger.log("Running banking pulse.")
                } else {
                    SystemLogger.log("No bank found, walking to predefined locations.")
                    if (bot.location.x > 2837) {
                        Pathfinder.find(bot, Location.create(2837, 3435, 0)).walk(bot)
                    } else if (bot.location.x > 2821) {
                        Pathfinder.find(bot, Location.create(2821, 3435, 0)).walk(bot)
                    } else if (bot.location.x > 2809){
                        Pathfinder.find(bot,Location.create(2809, 3436, 0)).walk(bot)
                    }
                }
            }

            State.TELEPORT_GE -> {
                SystemLogger.log("Teleporting to Grand Exchange.")
                scriptAPI.teleportToGE()
                state = State.SELL_GE
                SystemLogger.log("State changed to SELL_GE")
            }

            State.SELL_GE -> {
                SystemLogger.log("Selling sharks on Grand Exchange.")
                scriptAPI.sellOnGE(Items.RAW_SHARK_383)
                state = State.TELE_CATH
                SystemLogger.log("State changed to TELE_CATH")
            }

            State.TELE_CATH -> {
                SystemLogger.log("Teleporting to Catherby.")
                if(tick++ == 10) {
                    bot.lock()
                    bot.visualize(ANIMATION, GRAPHICS)
                    bot.impactHandler.disabledTicks = 4
                    val location = Location.create(2819, 3437, 0)
                    GameWorld.Pulser.submit(object : Pulse(4, bot) {
                        override fun pulse(): Boolean {
                            bot.unlock()
                            bot.properties.teleportLocation = location
                            bot.animator.reset()
                            state = State.IDLE
                            SystemLogger.log("Teleport complete. State changed to IDLE")
                            return true
                        }
                    })
                }
            }
        }
    }

    init {
        SystemLogger.log("Initializing script and randomizing equipment.")
        val setUp = RandomFunction.random(Sets.values().size)
        equipment.addAll(Sets.values()[setUp].equipment)
        inventory.add(Item(311)) // Change lobster pot to harpoon (if needed)
        skills[Skills.FISHING] = 76 // Sharks require level 76 Fishing
    }

    enum class State{
        FISHING,
        BANKING,
        FIND_BANK,
        FIND_SPOT,
        TELEPORT_GE,
        SELL_GE,
        TELE_CATH,
        IDLE,
        INIT
    }

    override fun newInstance(): Script {
        SystemLogger.log("Creating new instance of script.")
        if (!sharkstopper && bots <= 0) {
            val script = CathShark()
            script.bot = AIPlayer(bot.startLocation)
            script.state = State.FIND_SPOT
            bots = 1
            SystemLogger.log("Bot started. State changed to FIND_SPOT")
            return script
        } else if (tick++ == 6000 && sharkstopper) {
            tick = 0
            sharkstopper = false
        }
        return newInstance()
    }
}
