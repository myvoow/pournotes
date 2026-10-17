package app.pournotes.data

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PronoteTest {
    @Test fun studentUrl() {
        val want = "https://x.index-education.net/pronote/eleve.html?fd=1&login=true"
        assertEquals(want, Pronote.eleveUrl("x.index-education.net"))
        assertEquals(want, Pronote.eleveUrl("https://x.index-education.net/pronote/"))
        assertEquals(want, Pronote.eleveUrl("https://x.index-education.net/pronote/parent.html?identifiant=abc"))
        assertEquals(want, Pronote.eleveUrl(" https://x.index-education.net/pronote/mobile.eleve.html "))
    }

    @Test fun qrUrl() {
        assertEquals(
            "https://x.index-education.net/pronote/mobile.eleve.html?fd=1&bydlg=A6ABB224-12DD-4E31-AD3E-8A39A1C2C335&login=true",
            Pronote.mobileUrl("https://x.index-education.net/pronote/eleve.html?login=true"),
        )
        assertEquals(
            "https://x.index-education.net/pronote/mobile.eleve.html?fd=1&bydlg=A6ABB224-12DD-4E31-AD3E-8A39A1C2C335&login=true",
            Pronote.mobileUrl("https://x.index-education.net/pronote/mobile.eleve.html"),
        )
    }

    @Test fun deflateRoundTrip() {
        val s = "{\"a\":1}".toByteArray().hex().toByteArray()
        assertEquals(String(s), String(inflate(deflate(s))))
    }

    @Test fun values() {
        assertEquals(LocalDateTime.of(2026, 10, 19, 8, 0), dateTime("19/10/2026 08:00:00"))
        assertEquals(LocalDate.of(2026, 10, 19).atStartOfDay(), dateTime("19/10/2026"))
        assertEquals(LocalDateTime.of(2026, 10, 19, 14, 5), dateTime("19/10/26 14h05"))
        assertEquals("Absent", gradeLabel("|1"))
        assertEquals("15,5", gradeLabel("15,5"))
        assertEquals("Lire p. 12\nExercice <3> é", htmlToText("<p>Lire&nbsp;p. 12</p><div>Exercice &lt;3&gt; &#233;</div>"))
    }

    /** PRONOTE_LIVE=1: opens a session on Index Education's demo server (no login), which checks the whole encrypted exchange. */
    @Test fun liveHandshake() {
        assumeTrue(System.getenv("PRONOTE_LIVE") != null)
        val p = Pronote.probe("https://demo.index-education.net/pronote/")
        assertTrue(p.week(LocalDate.now()) > -60)
    }

    /** PRONOTE_URL, PRONOTE_USER, PRONOTE_PASS: full login and the home screen's calls on a real account. */
    @Test fun liveAccount() {
        val url = System.getenv("PRONOTE_URL")
        assumeTrue(url != null)
        val p = Pronote.connect(url!!, System.getenv("PRONOTE_USER")!!, System.getenv("PRONOTE_PASS")!!)
        val w = p.week(LocalDate.now())
        println(p.profile())
        println(p.lessons(w).take(3))
        println(p.homework(w, w + 1).take(3))
        println(p.grades().let { it.copy(latest = it.latest.take(3)) })
    }
}
