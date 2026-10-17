package app.pournotes.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class PronoteException(message: String) : Exception(message)

data class Profile(val name: String, val className: String, val school: String)

data class Lesson(
    val id: String, val subject: String, val start: LocalDateTime, val end: LocalDateTime,
    val teacher: String?, val room: String?, val group: String?, val status: String?,
    val canceled: Boolean, val test: Boolean, val color: String?,
)

data class Homework(val id: String, val subject: String, val description: String, val due: LocalDate, val done: Boolean, val color: String?)

data class Grade(
    val subject: String, val value: String, val outOf: String, val date: LocalDate, val classAverage: String?,
    val min: String?, val max: String?, val coefficient: String?, val comment: String?, val color: String?,
)

data class Grades(val period: String, val overall: String?, val latest: List<Grade>)

/**
 * One authenticated session on a PRONOTE student space, speaking the encrypted JSON protocol of the web client.
 * Ported from pronotepy (bain3/pronotepy, revision for PRONOTE 2026.2.5). Blocking: call it off the main thread.
 */
class Pronote private constructor(val url: String, private val mobile: Boolean = false) {
    private val root = url.substringBefore('?').substringBeforeLast('/')
    private val page = url.substring(root.length + 1)
    private val cookies = CookieManager()
    private val aes = Aes()
    private var attrs = emptyMap<String, String>()
    private var order = 1
    private var encrypt = false
    private var compress = false
    private var onglets = emptySet<Int>()
    private lateinit var general: JSONObject
    private lateinit var user: JSONObject

    lateinit var username: String
        private set

    /** Mobile-app logins: the token for the next login. PRONOTE hands out a new one each time. */
    var token: String? = null
        private set

    companion object {
        /** With [uuid], a mobile-app login: [url] is a mobile page and [password] a token, from the QR code when [qr]. */
        fun connect(url: String, username: String, password: String, uuid: String? = null, qr: Boolean = false) =
            Pronote(if (uuid == null) eleveUrl(url) else url, uuid != null).apply { open(); login(username, password, uuid, qr) }

        /** The QR code PRONOTE shows under "Mes données > Connexion à l'application mobile", unlocked by its 4-digit [pin]. */
        fun fromQr(qr: String, pin: String, uuid: String): Pronote {
            val j = runCatching { JSONObject(qr) }.getOrNull()?.takeIf { it.has("login") && it.has("jeton") && it.has("url") }
                ?: throw PronoteException("Ce n'est pas un QR code de connexion PRONOTE.")
            val key = Aes(md5(pin.toByteArray()))
            val (login, jeton) = try {
                String(key.dec(j.getString("login").unhex())) to String(key.dec(j.getString("jeton").unhex()))
            } catch (e: PronoteException) {
                throw PronoteException("Code PIN incorrect.")
            }
            return connect(mobileUrl(j.getString("url")), login, jeton, uuid, qr = true)
        }

        /** The QR code points at the web page; the app talks to its "mobile." twin. */
        internal fun mobileUrl(url: String): String {
            val u = URI(url)
            val dir = u.path.substringBeforeLast('/')
            val page = u.path.substringAfterLast('/').let { if (it.startsWith("mobile.")) it else "mobile.$it" }
            return "${u.scheme}://${u.authority}$dir/$page?fd=1&bydlg=A6ABB224-12DD-4E31-AD3E-8A39A1C2C335&login=true"
        }

        /** Opens a session without logging in: enough to check the address and the protocol. */
        internal fun probe(url: String) = Pronote(eleveUrl(url)).apply { open() }

        /** Any address of a PRONOTE server → its student page, with the direct (non-ENT) login. */
        fun eleveUrl(input: String): String {
            val u = URI(input.trim().let { if ("://" in it) it else "https://$it" })
            val path = u.path.orEmpty()
            val dir = (if (path.endsWith(".html")) path.substringBeforeLast('/') else path.trimEnd('/')).ifEmpty { "/pronote" }
            return "${u.scheme}://${u.authority}$dir/eleve.html?fd=1&login=true"
        }

    }

    private fun open() {
        val html = request("$root/$page", null)
        val start = Regex("""Start ?\(\{([^}]*)\}\)""").find(html)
            ?: throw PronoteException("Ce n'est pas une page PRONOTE : vérifie l'adresse.")
        attrs = start.groupValues[1].split(",").mapNotNull { kv ->
            val (k, v) = kv.split(":", limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            k.trim().trim('\'', '"') to v.trim().trim('\'', '"')
        }.toMap()
        if ("h" !in attrs || "a" !in attrs) throw PronoteException("Le serveur PRONOTE n'a pas ouvert de session.")
        encrypt = flag("CrA")
        compress = flag("CoA")

        val ivTemp = ByteArray(16).also(SecureRandom()::nextBytes)
        val uuid = Base64.getEncoder().encodeToString(if (flag("http")) rsa(ivTemp) else ivTemp)
        val data = JSONObject().put("Uuid", uuid).put("identifiantNav", JSONObject.NULL)
        // The reply is already encrypted with the new IV.
        general = post("FonctionParametres", JSONObject().put("data", data), newIv = md5(ivTemp))
            .optJSONObject("data")?.optJSONObject("General") ?: throw PronoteException("Paramètres PRONOTE illisibles.")
    }

    private fun login(username: String, password: String, uuid: String?, qr: Boolean) {
        this.username = username
        val espace = attrs.getValue("a").toInt()
        val id = call(
            "Identification", data = JSONObject()
                .put("genreConnexion", 0).put("genreEspace", espace).put("identifiant", username)
                .put("pourENT", false).put("enConnexionAuto", false).put("demandeConnexionAuto", false)
                .put("demandeConnexionAppliMobile", qr).put("demandeConnexionAppliMobileJeton", qr)
                .put("enConnexionAppliMobile", uuid != null && !qr).put("uuidAppliMobile", uuid.orEmpty()).put("loginTokenSAV", ""),
        )
        val u = if (truthy(id.opt("modeCompLog"))) username.lowercase() else username
        val p = if (truthy(id.opt("modeCompMdp"))) password.lowercase() else password
        val hash = sha256(id.optString("alea") + p).hex().uppercase()
        val auth = Aes(md5((u + hash).toByteArray()), aes.iv)
        val challenge = auth.enc(id.getString("challenge").toByteArray()).hex()

        val res = call("Authentification", data = JSONObject().put("connexion", 0).put("challenge", challenge).put("espace", espace))
        val cle = res.optString("cle").ifEmpty {
            throw PronoteException(
                when {
                    qr -> "QR code refusé : il expire 10 minutes après sa création, génères-en un nouveau."
                    uuid != null -> "Connexion par QR code expirée : scanne un nouveau QR code."
                    else -> "Identifiant ou mot de passe incorrect."
                },
            )
        }
        aes.key = auth.key
        aes.key = md5(String(aes.dec(cle.unhex())).split(",").map { it.trim().toInt().toByte() }.toByteArray())

        val mfa = res.optJSONObject("actionsDoubleAuth")?.optString("V")?.let { runCatching { JSONArray(it) }.getOrNull() }
        if (mfa != null && (0 until mfa.length()).any { mfa.optInt(it) in setOf(3, 5) })
            throw PronoteException("Ce compte demande la double authentification (code PIN), pas encore prise en charge.")

        token = res.optString("jetonConnexionAppliMobile").ifEmpty { null }
        user = call("ParametresUtilisateur")
        onglets = ints(user.opt("listeOnglets"))
    }

    // ---- Typed calls ----

    fun profile(): Profile {
        val r = user.getJSONObject("ressource")
        return Profile(
            r.optString("L"),
            r.optJSONObject("classeDEleve")?.optString("L").orEmpty(),
            r.optJSONObject("Etablissement")?.optJSONObject("V")?.optString("L").orEmpty(),
        )
    }

    fun week(date: LocalDate): Int =
        1 + (ChronoUnit.DAYS.between(dateTime(general.getJSONObject("PremierLundi").getString("V")).toLocalDate(), date) / 7).toInt()

    fun lessons(week: Int): List<Lesson> {
        val r = user.getJSONObject("ressource")
        val data = JSONObject().put("ressource", r).put("Ressource", r)
            .put("avecAbsencesEleve", false).put("avecConseilDeClasse", true).put("estEDTPermanence", false)
            .put("avecAbsencesRessource", true).put("avecDisponibilites", true).put("avecInfosPrefsGrille", true)
            .put("NumeroSemaine", week).put("numeroSemaine", week)
        return objects(call("PageEmploiDuTemps", 16, data).opt("ListeCours")).mapNotNull(::lesson)
    }

    private fun lesson(j: JSONObject): Lesson? {
        val contents = objects(j.opt("ListeContenus")).takeIf { it.isNotEmpty() } ?: return null
        val start = j.v("DateDuCours")?.let(::dateTime) ?: return null
        fun of(g: Int) = contents.filter { it.optInt("G") == g }.map { it.optString("L") }.takeIf { it.isNotEmpty() }?.joinToString(", ")
        return Lesson(
            id = j.optString("N"),
            subject = of(16) ?: j.optString("memo").ifEmpty { "Cours" },
            start = start,
            end = j.v("DateDuCoursFin")?.let(::dateTime) ?: endFromGrid(start, j),
            teacher = of(3), room = of(17), group = of(2),
            status = j.optString("Statut").ifEmpty { null },
            canceled = truthy(j.opt("estAnnule")),
            test = truthy(j.optJSONObject("cahierDeTextes")?.optJSONObject("V")?.opt("estDevoir")),
            color = j.optString("CouleurFond").ifEmpty { null },
        )
    }

    /** Without an explicit end, PRONOTE gives a slot in the weekly grid and a length in slots. */
    private fun endFromGrid(start: LocalDateTime, j: JSONObject): LocalDateTime {
        val ends = objects(general.opt("ListeHeuresFin"))
        if (ends.size < 2) return start.plusHours(1)
        var place = j.optInt("place") % (ends.size - 1) + j.optInt("duree") - 1
        if (place > ends.size) place %= ends.size - 1
        val label = ends.firstOrNull { it.optInt("G") == place }?.optString("L") ?: return start.plusHours(1)
        return start.with(LocalTime.parse(label, DateTimeFormatter.ofPattern("H'h'mm")))
    }

    fun homework(fromWeek: Int, toWeek: Int): List<Homework> {
        val data = JSONObject().put("domaine", JSONObject().put("_T", 8).put("V", "[$fromWeek..$toWeek]"))
        return objects(call("PageCahierDeTexte", 88, data).opt("ListeTravauxAFaire")).map {
            Homework(
                id = it.optString("N"),
                subject = it.optJSONObject("Matiere")?.optJSONObject("V")?.optString("L").orEmpty(),
                description = htmlToText(it.v("descriptif").orEmpty()),
                due = dateTime(it.v("PourLe")!!).toLocalDate(),
                done = truthy(it.opt("TAFFait")),
                color = it.optString("CouleurFond").ifEmpty { null },
            )
        }
    }

    fun setDone(id: String, done: Boolean) {
        call("SaisieTAFFaitEleve", 88, JSONObject().put("listeTAF", JSONArray().put(JSONObject().put("N", id).put("TAFFait", done))))
    }

    /** The default period of the grades tab, its general average and its grades, newest first. */
    fun grades(): Grades {
        val tabs = objects(user.getJSONObject("ressource").opt("listeOngletsPourPeriodes"))
        val tab = tabs.firstOrNull { it.optInt("G") == 198 } ?: tabs.firstOrNull() ?: throw PronoteException("Pas de période de notes.")
        val periodId = tab.optJSONObject("periodeParDefaut")?.optJSONObject("V")?.optString("N")
        val period = objects(general.opt("ListePeriodes")).firstOrNull { it.optString("N") == periodId }
            ?: throw PronoteException("Période de notes introuvable.")
        val data = call("DernieresNotes", 198, JSONObject().put("Periode", JSONObject().put("N", period.optString("N")).put("L", period.optString("L"))))
        val grades = objects(data.opt("listeDevoirs")).map {
            val service = it.optJSONObject("service")?.optJSONObject("V")
            Grade(
                subject = service?.optString("L").orEmpty(),
                value = gradeLabel(it.v("note").orEmpty()),
                outOf = gradeLabel(it.v("bareme").orEmpty()),
                date = dateTime(it.v("date")!!).toLocalDate(),
                classAverage = it.v("moyenne")?.let(::gradeLabel),
                min = it.v("noteMin")?.let(::gradeLabel),
                max = it.v("noteMax")?.let(::gradeLabel),
                coefficient = it.opt("coefficient")?.toString(),
                comment = it.optString("commentaire").ifEmpty { null },
                color = service?.optString("couleur")?.ifEmpty { null },
            )
        }
        return Grades(period.optString("L"), data.v("moyGenerale"), grades.sortedByDescending { it.date })
    }

    // ---- Transport ----

    private fun call(name: String, onglet: Int? = null, data: JSONObject? = null): JSONObject {
        if (onglet != null && onglet !in onglets) throw PronoteException("Rubrique non ouverte pour ce compte.")
        val payload = JSONObject()
        onglet?.let { payload.put("Signature", JSONObject().put("onglet", it)) }
        data?.let { payload.put("data", it) }
        return post(name, payload).optJSONObject("data") ?: JSONObject()
    }

    /** Requests are numbered (odd from the client), so they go one at a time. */
    @Synchronized
    private fun post(name: String, payload: JSONObject, newIv: ByteArray? = null): JSONObject {
        var bytes: ByteArray? = null
        if (compress) bytes = deflate(payload.toString().toByteArray().hex().toByteArray())
        if (encrypt) bytes = aes.enc(bytes ?: payload.toString().toByteArray())
        val no = aes.enc(order.toString().toByteArray()).hex()
        val body = JSONObject().put("session", attrs.getValue("h").toInt()).put("no", no).put("id", name)
            .put("dataSec", bytes?.hex()?.uppercase() ?: payload)
        val res = JSONObject(request("$root/appelfonction/${attrs["a"]}/${attrs["h"]}/$no", body.toString()))
        order += 2
        res.optJSONObject("Erreur")?.let { e ->
            throw PronoteException(
                when (e.optInt("G")) {
                    10, 22 -> "Session PRONOTE expirée."
                    25 -> "Trop de tentatives de connexion : réessaie dans quelques minutes."
                    else -> e.optString("Titre").ifEmpty { "Erreur PRONOTE ${e.optInt("G")}." }
                },
            )
        }
        newIv?.let { aes.iv = it }
        val sec = res.opt("dataSec")
        if (sec is JSONObject) return sec
        var raw = (sec as? String)?.unhex() ?: throw PronoteException("Réponse PRONOTE vide.")
        if (encrypt) raw = aes.dec(raw)
        if (compress) raw = inflate(raw)
        return JSONObject(String(raw))
    }

    private fun request(url: String, json: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", if (mobile) "iPhone" else UA)
        cookies.get(c.url.toURI(), emptyMap())["Cookie"]?.takeIf { it.isNotEmpty() }?.let { c.setRequestProperty("Cookie", it.joinToString("; ")) }
        if (json != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(json.toByteArray()) }
        }
        try {
            val code = c.responseCode
            cookies.put(c.url.toURI(), c.headerFields.filterKeys { it != null })
            if (code !in 200..299) throw PronoteException("Le serveur PRONOTE a répondu $code.")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun flag(key: String) = attrs[key].let { it != null && it != "false" && it != "0" }
}

// ---- Crypto & encoding ----

private class Aes(var key: ByteArray = md5(ByteArray(0)), var iv: ByteArray = ByteArray(16)) {
    fun enc(d: ByteArray): ByteArray = run(Cipher.ENCRYPT_MODE, d)
    fun dec(d: ByteArray): ByteArray = try {
        run(Cipher.DECRYPT_MODE, d)
    } catch (e: java.security.GeneralSecurityException) {
        throw PronoteException("Déchiffrement impossible : identifiants incorrects ou session expirée.")
    }

    private fun run(mode: Int, d: ByteArray) =
        Cipher.getInstance("AES/CBC/PKCS5Padding").also { it.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }.doFinal(d)
}

private fun md5(b: ByteArray): ByteArray = MessageDigest.getInstance("MD5").digest(b)
private fun sha256(s: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())

// PRONOTE turns away browsers it does not know.
private const val UA = "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0"

// eleve.js's public key, used to hand over the AES IV on servers without HTTPS.
private val RSA_MOD = BigInteger("130337874517286041778445012253514395801341480334668979416920989365464528904618150245388048105865059387076357492684573172203245221386376405947824377827224846860699130638566643129067735803555082190977267155957271492183684665050351182476506458843580431717209261903043895605014125081521285387341454154194253026277")

private fun rsa(d: ByteArray): ByteArray {
    val key = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(RSA_MOD, BigInteger.valueOf(65537)))
    return Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.ENCRYPT_MODE, key) }.doFinal(d)
}

internal fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
internal fun String.unhex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

/** Raw deflate (zlib without header nor checksum), as PRONOTE expects. */
internal fun deflate(b: ByteArray): ByteArray {
    val d = Deflater(6, true).apply { setInput(b); finish() }
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (!d.finished()) out.write(buf, 0, d.deflate(buf))
    d.end()
    return out.toByteArray()
}

internal fun inflate(b: ByteArray): ByteArray {
    val i = Inflater(true).apply { setInput(b) }
    val out = ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (!i.finished()) {
        val n = i.inflate(buf)
        if (n == 0 && i.needsInput()) break
        out.write(buf, 0, n)
    }
    i.end()
    return out.toByteArray()
}

// ---- JSON & values ----

/** PRONOTE wraps most values as {"V": …}. */
private fun JSONObject.v(key: String): String? {
    val x = opt(key)
    val s = if (x is JSONObject) x.opt("V")?.toString() else if (x is String || x is Number) x.toString() else null
    return s?.ifEmpty { null }
}

/** Lists come bare or wrapped in {"V": [...]}. */
private fun objects(x: Any?): List<JSONObject> {
    val a = x as? JSONArray ?: (x as? JSONObject)?.optJSONArray("V") ?: return emptyList()
    return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}

/** Every number in a nested structure: the tabs this account may open. */
private fun ints(x: Any?): Set<Int> = when (x) {
    is Int -> setOf(x)
    is JSONArray -> (0 until x.length()).flatMap { ints(x.opt(it)) }.toSet()
    is JSONObject -> x.keys().asSequence().flatMap { ints(x.opt(it)) }.toSet()
    else -> emptySet()
}

private fun truthy(x: Any?) = when (x) {
    is Boolean -> x
    is Number -> x.toInt() != 0
    is String -> x == "true" || x == "1"
    else -> false
}

private val DATE_TIMES = listOf("dd/MM/yyyy HH:mm:ss", "dd/MM/yy HH'h'mm").map(DateTimeFormatter::ofPattern)
private val DATES = listOf("dd/MM/yyyy", "dd/MM/yy").map(DateTimeFormatter::ofPattern)

internal fun dateTime(s: String): LocalDateTime =
    DATE_TIMES.firstNotNullOfOrNull { runCatching { LocalDateTime.parse(s, it) }.getOrNull() }
        ?: DATES.firstNotNullOfOrNull { runCatching { LocalDate.parse(s, it).atStartOfDay() }.getOrNull() }
        ?: throw PronoteException("Date PRONOTE illisible : $s")

private val GRADE_CODES = listOf("Absent", "Dispensé", "Non noté", "Inapte", "Non rendu", "Absent (0)", "Non rendu (0)", "Félicitations")

/** "|1" and friends are codes for special marks. */
internal fun gradeLabel(s: String) = if (s.startsWith("|")) GRADE_CODES.getOrElse((s.drop(1).toIntOrNull() ?: 0) - 1) { s } else s

internal fun htmlToText(html: String): String = html
    .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>"), "\n")
    .replace(Regex("<[^>]*>"), "")
    .replace(Regex("&#(\\d+);")) { it.groupValues[1].toInt().toChar().toString() }
    .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
    .lines().joinToString("\n") { it.trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
