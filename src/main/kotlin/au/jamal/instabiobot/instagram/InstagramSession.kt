package au.jamal.instabiobot.instagram

import au.jamal.instabiobot.utilities.Delay
import au.jamal.instabiobot.utilities.Log
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.time.LocalDateTime

class InstagramSession() {

    private val session = BrowserManager()
    private val sessionInterface = InstagramInterface(session)
    private val credentials = CredentialManager()
    private val twoFactorSecret = System.getenv("IG_2FA_SECRET") // 2FA Secret Key

    fun login() {
        session.browser.get(INSTAGRAM_URL)
        Delay.sleep(5..10)

        // Enter username & password
        val usernameInput = sessionInterface.getUsernameElement()
        val passwordInput = sessionInterface.getPasswordElement()
        sendKeys(usernameInput, credentials.username)
        sendKeys(passwordInput, credentials.password)

        // Click login button
        val loginButton = sessionInterface.getLoginElement()
        clickButton(loginButton)
        Delay.sleep(5..10)

        // Handle 2FA Prompt
        handleTwoFactorAuth()

        // Verify login
        accessSettings()
        Log.status("Login successful at ${LocalDateTime.now()}")
    }

    private fun handleTwoFactorAuth() {
        val wait = WebDriverWait(session.browser, Duration.ofSeconds(10))

        try {
            // Check if 2FA input is present
            val twoFactorInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.name("verificationCode"))
            )

            if (twoFactorInput != null) {
                Log.status("2FA Prompt Detected. Generating code...")

                // Generate the 2FA code
                val twoFactorCode = generate2FACode(twoFactorSecret)

                // Enter the code & submit
                sendKeys(twoFactorInput, twoFactorCode)
                val submitButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//button[@type='submit']"))
                )
                clickButton(submitButton)

                Log.status("2FA code entered successfully.")
                Delay.sleep(5..10)
            }
        } catch (e: Exception) {
            Log.status("No 2FA prompt detected, continuing login.")
        }
    }

    private fun generate2FACode(secretKey: String): String {
        val generator = GoogleAuthenticator(secretKey)
        return generator.now()
    }

    fun getCurrentBio(): String {
        accessSettings()
        val bioText = sessionInterface.getBioTextAttribute()
        Log.status("Got current bio text [$bioText] at ${LocalDateTime.now()}")
        return bioText
    }

    fun updateBio(newBioText: String) {
        accessSettings()
        val bioElement = sessionInterface.getBioElement()
        sendKeys(bioElement, newBioText)
        val updateButton = sessionInterface.getUpdateElement()
        clickButton(updateButton)
        Delay.sleep(5..10)
        val updateButtonDisabled = sessionInterface.getUpdateButtonStatus()
        if (!updateButtonDisabled) {
            Log.alert("Bio update to [$newBioText] failed at ${LocalDateTime.now()}")
            throw IllegalStateException("Instagram bio update failed...")
        }
        Log.status("Updated bio text: [$newBioText] at ${LocalDateTime.now()}")
    }

    fun end() {
        session.end()
    }

    private fun sendKeys(element: WebElement, key: String) {
        try {
            element.clear()
            element.sendKeys(key)
        } catch (e: Exception) {
            Log.alert("Failed to send keys to element")
            Log.dump(element)
            throw IllegalStateException("Failed to send keys...", e)
        }
    }

    private fun clickButton(element: WebElement) {
        try {
            element.click()
        } catch (e: Exception) {
            Log.alert("Failed to click element")
            Log.dump(element)
            throw IllegalStateException("Failed to click element...", e)
        }
    }

    private fun accessSettings() {
        if (session.browser.currentUrl != INSTAGRAM_SETTINGS_URL) {
            session.browser.get(INSTAGRAM_SETTINGS_URL)
            Delay.sleep(5..10)
            if (session.browser.currentUrl != INSTAGRAM_SETTINGS_URL) {
                Log.alert("Failed to access settings")
                throw IllegalStateException("Session login issue...")
            }
        }
    }

    companion object {
        private const val INSTAGRAM_URL: String = "https://www.instagram.com/"
        private const val INSTAGRAM_SETTINGS_URL: String = "https://www.instagram.com/accounts/edit/"
    }
}