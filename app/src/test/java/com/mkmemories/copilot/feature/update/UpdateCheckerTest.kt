package com.mkmemories.copilot.feature.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mise à jour intégrée : ne proposer que les builds strictement plus récents. */
class UpdateCheckerTest {

    private fun release(build: Int, withApk: Boolean = true) = """
        {"tag_name":"apk-build-$build","name":"MK Copilot — APK d'essai (build $build)",
         "assets":[
           {"name":"notes.txt","browser_download_url":"https://example.com/notes.txt"}
           ${if (withApk) """,{"name":"mk-copilot-build-$build.apk",
             "browser_download_url":"https://github.com/MKMemories/MKAndroidAuto/releases/download/apk-build-$build/mk-copilot-build-$build.apk"}""" else ""}
         ]}
    """.trimIndent()

    @Test
    fun `le build le plus recent de la liste est propose avec son lien APK direct`() {
        val body = "[${release(7)},${release(5)},${release(3)}]"
        val update = UpdateChecker.parse(body, currentBuild = 3)!!
        assertEquals(7, update.buildNumber)
        assertEquals("MK Copilot — APK d'essai (build 7)", update.title)
        assertEquals(
            "https://github.com/MKMemories/MKAndroidAuto/releases/download/apk-build-7/mk-copilot-build-7.apk",
            update.downloadUrl,
        )
    }

    @Test
    fun `l'ordre de la liste n'importe pas - c'est le numero qui compte`() {
        val body = "[${release(2)},${release(9)},${release(4)}]"
        assertEquals(9, UpdateChecker.parse(body, currentBuild = 1)!!.buildNumber)
    }

    @Test
    fun `pas de proposition pour le meme build ou un plus ancien`() {
        assertNull(UpdateChecker.parse("[${release(7)}]", currentBuild = 7))
        assertNull(UpdateChecker.parse("[${release(7)}]", currentBuild = 12))
    }

    @Test
    fun `une release sans APK ou au tag inattendu est ignoree`() {
        assertNull(UpdateChecker.parse("[${release(9, withApk = false)}]", currentBuild = 1))
        assertNull(UpdateChecker.parse("""[{"tag_name":"v1.2.3","assets":[]}]""", currentBuild = 1))
        assertNull(UpdateChecker.parse("[]", currentBuild = 1))
    }
}
