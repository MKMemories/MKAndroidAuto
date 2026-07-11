package com.mkmemories.copilot.feature.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mise à jour intégrée : ne proposer que les builds strictement plus récents. */
class UpdateCheckerTest {

    private fun release(tag: String, apkName: String = "mk-copilot-build-7.apk") = """
        {"tag_name":"$tag","name":"MK Copilot — APK d'essai (build 7)",
         "assets":[
           {"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"},
           {"name":"$apkName","browser_download_url":"https://github.com/MKMemories/MKAndroidAuto/releases/download/$tag/$apkName"}
         ]}
    """.trimIndent()

    @Test
    fun `un build plus recent est propose avec son lien APK direct`() {
        val update = UpdateChecker.parse(release("apk-build-7"), currentBuild = 3)!!
        assertEquals(7, update.buildNumber)
        assertEquals("MK Copilot — APK d'essai (build 7)", update.title)
        assertEquals(
            "https://github.com/MKMemories/MKAndroidAuto/releases/download/apk-build-7/mk-copilot-build-7.apk",
            update.downloadUrl,
        )
    }

    @Test
    fun `pas de proposition pour le meme build ou un plus ancien`() {
        assertNull(UpdateChecker.parse(release("apk-build-7"), currentBuild = 7))
        assertNull(UpdateChecker.parse(release("apk-build-7"), currentBuild = 12))
    }

    @Test
    fun `tag inattendu ou release sans APK = null, jamais d'erreur`() {
        assertNull(UpdateChecker.parse(release("v1.2.3"), currentBuild = 1))
        assertNull(
            UpdateChecker.parse(
                """{"tag_name":"apk-build-9","assets":[{"name":"notes.txt","browser_download_url":"x"}]}""",
                currentBuild = 1,
            ),
        )
    }
}
