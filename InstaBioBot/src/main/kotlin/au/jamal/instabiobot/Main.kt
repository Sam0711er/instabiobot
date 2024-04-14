package au.jamal.instabiobot

import au.jamal.instabiobot.control.ConfigHandler
import au.jamal.instabiobot.control.SessionController

fun main() {
    val config = ConfigHandler.loadSettings()
    SessionController.mainSessionLoop(production = false, debug = true)
} // TODO: pass config into loop