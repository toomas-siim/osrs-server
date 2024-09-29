package org.rs09.client

import org.rs09.client.config.GameConfig
import org.runite.client.GameShell

object GameLaunch {

    /**
     * The main method.
     * r @param args the arguments casted on runtime.
     * r_game
     */
    @JvmStatic
    fun main(args: Array<String>) {
        for (i in args.indices) {
            val cmd = args[i].split("=").toTypedArray()
            when (cmd[0]) {
                "ip" -> GameConfig.IP_ADDRESS = cmd[1]
                "world" -> GameConfig.WORLD = cmd[1].toInt()
                else -> GameConfig.configLocation = cmd[0]
            }
        }
        try {
            System.out.println("Trying to parse config at " + GameConfig.configLocation)
            GameConfig.parse(GameConfig.configLocation)
            GameConfig.implementHoliday()
            GameConfig.extendRenderDistance()
        } catch (e: Exception){
            GameConfig.IP_ADDRESS = "play.2009scape.org"
            GameConfig.IP_MANAGEMENT = "play.2009scape.org"
            GameConfig.RCM_STYLE_PRESET = "classic"
            GameConfig.RCM_TITLE = "<col=5d5447>Choose Option</col>"
            GameConfig.HOLIDAYS_ENABLED = true
            GameConfig.implementHoliday()
            GameConfig.RENDER_DISTANCE_INCREASE = true
            GameConfig.extendRenderDistance()
            e.printStackTrace()
        }
        /**
         * Launches the client
         */
        GameShell.launchDesktop()
    }

}