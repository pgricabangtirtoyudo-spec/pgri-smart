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
}
