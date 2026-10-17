package app.pournotes.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** [uuid] set: a mobile-app login, [pass] being its current token. */
data class Creds(val url: String, val user: String, val pass: String, val uuid: String? = null)

/** "Rester connecté": the credentials, sealed with an AES key that never leaves the Android Keystore. */
class Vault(context: Context) {
    private val prefs = context.getSharedPreferences("vault", Context.MODE_PRIVATE)

    fun load(): Creds? = runCatching {
        val iv = Base64.decode(prefs.getString("iv", null) ?: return null, Base64.NO_WRAP)
        val sealed = Base64.decode(prefs.getString("data", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(GCM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        val j = JSONObject(String(cipher.doFinal(sealed)))
        Creds(j.getString("url"), j.getString("user"), j.getString("pass"), j.optString("uuid").ifEmpty { null })
    }.getOrNull()

    fun save(c: Creds) {
        val cipher = Cipher.getInstance(GCM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val plain = JSONObject().put("url", c.url).put("user", c.user).put("pass", c.pass).put("uuid", c.uuid).toString()
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("data", Base64.encodeToString(cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP))
            .apply()
    }

    fun clear() = prefs.edit().remove("iv").remove("data").apply()

    /** Stable per install: PRONOTE ties its mobile tokens to it. */
    val deviceId: String
        get() = prefs.getString("device", null) ?: java.util.UUID.randomUUID().toString().also { prefs.edit().putString("device", it).apply() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec) }.generateKey()
    }

    private companion object {
        const val ALIAS = "creds"
        const val GCM = "AES/GCM/NoPadding"
    }
}
