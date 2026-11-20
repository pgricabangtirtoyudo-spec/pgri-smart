package id.or.pgricabangtirtoyudo.smart

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Pemanggil pintu API Apps Script (Tahap39.gs). */
object Api {
    class GalatApi(pesan: String) : Exception(pesan)

    suspend fun panggil(fn: String, vararg args: Any?): Any? = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        for (a in args) arr.put(a ?: JSONObject.NULL)
        val body = JSONObject().put("fn", fn).put("args", arr).toString()
        val teks: String
        try {
            val c = URL(Konfig.API_URL).openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.connectTimeout = 20000
            c.readTimeout = 60000
            c.doOutput = true
            c.instanceFollowRedirects = true
            c.setRequestProperty("Content-Type", "text/plain;charset=utf-8")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val s = if (c.responseCode in 200..299) c.inputStream else c.errorStream
            teks = s.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw GalatApi("Tidak dapat terhubung. Periksa sambungan internet.")
        }
        val j = try { JSONObject(teks) } catch (e: Exception) { throw GalatApi("Jawaban server tidak dikenal. Coba lagi.") }
        if (!j.optBoolean("ok")) throw GalatApi(j.optString("error", "Gagal."))
        j.opt("data")
    }
}

fun Any?.obj(): JSONObject = this as? JSONObject ?: JSONObject()

fun JSONArray?.daftar(): List<JSONObject> {
    val h = ArrayList<JSONObject>()
    if (this == null) return h
    for (i in 0 until length()) optJSONObject(i)?.let { h.add(it) }
    return h
}

/** Penyimpanan sesi masuk di perangkat. */
object Sesi {
    private fun p(c: Context) = c.getSharedPreferences("pgri_native", Context.MODE_PRIVATE)
    fun token(c: Context): String = p(c).getString("token", "") ?: ""
    fun npa(c: Context): String = p(c).getString("npa", "") ?: ""
    fun simpan(c: Context, token: String, npa: String) { p(c).edit().putString("token", token).putString("npa", npa).apply() }
    fun hapusToken(c: Context) { p(c).edit().remove("token").apply() }

    /** Sesi admin: hanya token (bukan sandi) yang disimpan; username boleh diingat. */
    fun admin(c: Context): JSONObject? = try { p(c).getString("admin", "")?.takeIf { it.isNotEmpty() }?.let { JSONObject(it) } } catch (e: Exception) { null }
    fun simpanAdmin(c: Context, o: JSONObject) { p(c).edit().putString("admin", o.toString()).apply() }
    fun hapusAdmin(c: Context) { p(c).edit().remove("admin").apply() }
    fun adminUser(c: Context): String = p(c).getString("adminUser", "") ?: ""
    fun simpanAdminUser(c: Context, u: String) { p(c).edit().putString("adminUser", u).apply() }

    /** "Ingat saya": NPA dan sandi disimpan terenkripsi (Android Keystore) di perangkat ini. */
    private fun aman(c: Context) = try {
        val k = androidx.security.crypto.MasterKey.Builder(c).setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build()
        androidx.security.crypto.EncryptedSharedPreferences.create(
            c, "pgri_ingat", k,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) { null }
    fun ingatNpa(c: Context): String = try { aman(c)?.getString("npa", "") ?: "" } catch (e: Exception) { "" }
    fun ingatSandi(c: Context): String = try { aman(c)?.getString("sandi", "") ?: "" } catch (e: Exception) { "" }
    fun simpanIngat(c: Context, npa: String, sandi: String) { try { aman(c)?.edit()?.putString("npa", npa)?.putString("sandi", sandi)?.apply() } catch (_: Exception) {} }
    fun perbaruiSandiIngat(c: Context, sandi: String) { try { if (ingatSandi(c).isNotEmpty()) aman(c)?.edit()?.putString("sandi", sandi)?.apply() } catch (_: Exception) {} }
    /** Saat keluar: sandi dihapus, NPA tetap terisi di layar masuk. */
    fun lupakanSandi(c: Context) { try { aman(c)?.edit()?.remove("sandi")?.apply() } catch (_: Exception) {} }
}


/** Salinan data terakhir agar aplikasi tetap bisa dibuka tanpa sinyal. */
object Cache {
    private fun p(c: Context) = c.getSharedPreferences("pgri_cache", Context.MODE_PRIVATE)
    fun simpan(c: Context, kunci: String, isi: String) { p(c).edit().putString(kunci, isi).apply() }
    fun baca(c: Context, kunci: String): String = p(c).getString(kunci, "") ?: ""
    fun obj(c: Context, kunci: String): JSONObject? = try { baca(c, kunci).takeIf { it.isNotEmpty() }?.let { JSONObject(it) } } catch (e: Exception) { null }
}
