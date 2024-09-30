package plugin.ai.general.scriptrepository

import core.game.interaction.MovementPulse
import core.game.node.item.Item
import core.game.world.map.zone.ZoneBorders
import core.tools.Items
import plugin.ai.general.ScriptAPI
import plugin.ai.skillingbot.SkillingBotAssembler
import core.game.node.entity.skill.Skills
import core.game.node.Node
import core.game.interaction.DestinationFlag


@PlayerCompatible
@ScriptName("Falador Ore Smelter")
@ScriptDescription("Start at Falador East Bank with ores in your inventory or bank.")
@ScriptIdentifier("fally_ore_smelter")
class OreSmelter() : Script() {
    var state = State.INIT
    var selectedOre = Items.BRONZE_BAR_2349 // Default ore for smelting
    var smeltedAmount = 0

    val smelterZone = ZoneBorders(2974, 3369, 2979, 3373) // Falador furnace location
    val bankZone = ZoneBorders(3009, 3355, 3018, 3358) // Falador East Bank

    var overlay: ScriptAPI.BottingOverlay? = null

    override fun tick() {
        when (state) {
            State.INIT -> {
                overlay = scriptAPI.getOverlay()
                overlay!!.init()
                overlay!!.setTitle("Ore Smelting")
                overlay!!.setTaskLabel("Bars Smelted:")
                overlay!!.setAmount(0)

                bot.dialogueInterpreter.sendOptions("Select Ore to Smelt", "Bronze", "Iron", "Steel", "Silver", "Gold", "Mithril", "Adamantite", "Runite")
                bot.dialogueInterpreter.addAction { _, button ->
                    selectedOre = when (button) {
                        1 -> Items.BRONZE_BAR_2349
						2 -> Items.IRON_BAR_2351
						3 -> Items.STEEL_BAR_2353
						4 -> Items.SILVER_BAR_2355
						5 -> Items.GOLD_BAR_2357
						6 -> Items.MITHRIL_BAR_2359
						7 -> Items.ADAMANTITE_BAR_2361
						8 -> Items.RUNITE_BAR_2363
						else -> Items.BRONZE_BAR_2349
                    }
                    state = State.TO_SMELTER
                }
            }

            State.TO_SMELTER -> {
                if (!smelterZone.insideBorder(bot)) {
                    scriptAPI.walkTo(smelterZone.randomLoc)
                } else {
                    state = State.SMELTING
                }
            }

            State.SMELTING -> {
                val furnace = scriptAPI.getNearestNode("Furnace", true)
                if (furnace != null) {
                    furnace.interaction.handle(bot, furnace.interaction[0])
                    bot.pulseManager.run(object : SmeltingPulse(this, furnace) {
                        override fun pulse(): Boolean {
                            // Check if the bot has no ores left to smelt
                            state = if (bot.inventory.getAmount(selectedOre) == 0) {
                                State.TO_BANK
                            } else {
                                State.SMELTING
                            }
                            return super.pulse()
                        }
                    })
                }
                overlay!!.setAmount(smeltedAmount)
            }


            State.TO_BANK -> {
                if (bankZone.insideBorder(bot)) {
                    val bank = scriptAPI.getNearestNode("bank booth", true)
                    if (bank != null) {
                        bot.pulseManager.run(object : BankingPulse(this, bank) {
                            override fun pulse(): Boolean {
                                state = State.BANKING
                                return super.pulse()
                            }
                        })
                    }
                } else {
                    scriptAPI.walkTo(bankZone.randomLoc)
                }
            }

            State.BANKING -> {
                smeltedAmount += bot.inventory.getAmount(selectedOre)
                scriptAPI.bankItem(selectedOre)
                state = State.TO_SMELTER
            }
        }
    }

    open class SmeltingPulse(val script: Script, val furnace: Node) : MovementPulse(script.bot, furnace, DestinationFlag.OBJECT) {
        override fun pulse(): Boolean {
            script.bot.faceLocation(furnace.location)
            return true
        }
    }

    open class BankingPulse(val script: Script, val bank: Node) : MovementPulse(script.bot, bank, DestinationFlag.OBJECT) {
        override fun pulse(): Boolean {
            script.bot.faceLocation(bank.location)
            return true
        }
    }

    override fun newInstance(): Script {
        val script = OreSmelter()
        script.bot = SkillingBotAssembler().produce(SkillingBotAssembler.Wealth.POOR, bot.startLocation)
        return script
    }

    enum class State {
        INIT,
        TO_SMELTER,
        SMELTING,
        TO_BANK,
        BANKING
    }

    init {
        equipment.add(Item(Items.IRON_PICKAXE_1267)) // Assumed pickaxe for mining beforehand
        skills.put(Skills.SMITHING, 30) // Example smithing level requirement
    }
}
