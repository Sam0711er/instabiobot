package au.jamal.instabiobot.control

import au.jamal.instabiobot.instagram.InstagramSession
import au.jamal.instabiobot.utilities.Delay
import au.jamal.instabiobot.utilities.Log
import au.jamal.instabiobot.utilities.Bio
import java.time.LocalDate
import kotlin.system.exitProcess

object SessionController {

    private var failCount: Int = 0
    val config = ConfigHandler.loadSettings()

    private fun bioUpdateHandler(session: InstagramSession) {
        Log.info("Starting bio update process...")

        val currentBio: String = session.getCurrentBio()
        val generatedBio: String = Bio.buildText()

        if (currentBio != generatedBio) {
            session.updateBio(generatedBio)
            Log.status("Bio successfully updated: $generatedBio")
        } else {
            Log.status("Bio is already up-to-date, no changes made.")
        }
    }

    fun mainSessionLoop() {
        if (failCount >= config.failLimit) {
            Log.alert("Fail limit reached, exiting...")
            exitProcess(0)
        }

        val session = InstagramSession()
        try {
            session.login()
            bioUpdateHandler(session)
        } catch (e: Exception) {
            Log.error(e)
            failCount += 1
            Log.alert("Session failed: $failCount/${config.failLimit}")
            Delay.sleep(60..120)
        } finally {
            session.end()
        }

        Log.alert("Exiting after one execution")
        exitProcess(0)  // Ensure the script runs only once
    }
}