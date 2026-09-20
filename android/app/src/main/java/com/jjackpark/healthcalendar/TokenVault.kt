package com.jjackpark.healthcalendar

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TokenVault(context: Context) {
 private val preferences = context.getSharedPreferences("calendar_private", Context.MODE_PRIVATE)
 private fun key(): SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  return (store.getKey("calendar-sync", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder("calendar-sync", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  }.generateKey()
 }
 fun save(token: String) {
  require(token.matches(Regex("[a-f0-9]{64}")))
  val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
  val encoded = Base64.encodeToString(cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
  check(preferences.edit().putString("token", encoded).commit())
 }
 fun token(): String? = try {
  preferences.getString("token", null)?.let { encoded ->
   val bytes = Base64.decode(encoded, Base64.NO_WRAP)
   val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
   String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
  }
 } catch (_: Exception) { null }
 fun clear() { preferences.edit().remove("token").remove("last_sync").apply() }
 fun status(message: String) { preferences.edit().putString("status", message).apply() }
 fun status(): String = preferences.getString("status", "아직 동기화 전입니다.")!!
 fun synced() { preferences.edit().putLong("last_sync", System.currentTimeMillis()).apply() }
 fun lastSync(): Long = preferences.getLong("last_sync", 0)
 fun syncMinutes(): Int = preferences.getInt("sync_minutes", 60)
 fun syncMinutes(value: Int) { preferences.edit().putInt("sync_minutes", value).apply() }
}
