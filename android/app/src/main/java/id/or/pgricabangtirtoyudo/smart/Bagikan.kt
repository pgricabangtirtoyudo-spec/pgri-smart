package id.or.pgricabangtirtoyudo.smart

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

private fun logoBmp(c: Context): Bitmap? = try { BitmapFactory.decodeResource(c.resources, R.drawable.logo_pgri) } catch (e: Exception) { null }

private fun berkas(c: Context, nama: String): File {
    val d = File(c.cacheDir, "share"); d.mkdirs()
    return File(d, nama)
}

private fun kirim(c: Context, f: File, mime: String, judul: String) {
    val uri = FileProvider.getUriForFile(c, c.packageName + ".fileprovider", f)
    val i = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, judul)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    c.startActivity(Intent.createChooser(i, judul).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun bagikanKartu(c: Context, p: JSONObject, foto: String) {
    try {
        val bmp = gambarKartu(c, p, fotoDariData(foto))
        val f = berkas(c, "kartu-anggota.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        kirim(c, f, "image/png", "Kartu Anggota PGRI")
    } catch (_: Exception) {}
}

/** Kartu anggota sebagai gambar (rasio kartu ID), mirip tampilan di aplikasi. */
fun gambarKartu(c: Context, p: JSONObject, foto: Bitmap?): Bitmap {
    val w = 1080; val h = 680
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(out)
    val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    val klip = Path().apply { addRoundRect(rect, 44f, 44f, Path.Direction.CW) }
    cv.clipPath(klip)
    val pt = Paint(Paint.ANTI_ALIAS_FLAG)
    pt.shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), intArrayOf(0xFF0D2F68.toInt(), 0xFF1A4FA3.toInt(), 0xFF763769.toInt()), floatArrayOf(0f, 0.62f, 1f), Shader.TileMode.CLAMP)
    cv.drawRect(rect, pt)
    pt.shader = null
    pt.color = 0x12FFFFFF
    cv.drawCircle(w - 40f, 20f, 230f, pt)

    // logo
    pt.color = Color.WHITE
    cv.drawCircle(100f, 100f, 58f, pt)
    logoBmp(c)?.let { cv.drawBitmap(it, null, RectF(48f, 48f, 152f, 152f), Paint(Paint.FILTER_BITMAP_FLAG)) }
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 32f; letterSpacing = 0.06f }
    cv.drawText("KARTU ANGGOTA", 180f, 92f, tp)
    cv.drawText("PGRI CABANG TIRTOYUDO", 180f, 134f, tp)

    // QR
    val npa = p.optString("npa")
    if (npa.isNotEmpty()) {
        pt.color = Color.WHITE
        cv.drawRoundRect(RectF(w - 210f, 36f, w - 40f, 206f), 20f, 20f, pt)
        cv.drawBitmap(buatQr(Konfig.URL_VERIFIKASI + npa, 300), null, RectF(w - 198f, 48f, w - 52f, 194f), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    // foto
    val fx = 60f; val fy = 250f; val fs = 250f
    val fr = RectF(fx, fy, fx + fs, fy + fs)
    pt.color = 0xFF2A63B8.toInt()
    cv.drawRoundRect(fr, 26f, 26f, pt)
    if (foto != null) {
        val s = potongPersegi(foto, 400)
        cv.save(); cv.clipPath(Path().apply { addRoundRect(fr, 26f, 26f, Path.Direction.CW) })
        cv.drawBitmap(s, null, fr, Paint(Paint.FILTER_BITMAP_FLAG)); cv.restore()
    } else {
        val ip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 100f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        cv.drawText(inisial(p.optString("nama")), fx + fs / 2, fy + fs / 2 + 34f, ip)
    }
    val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = 0xE6FFFFFF.toInt() }
    cv.drawRoundRect(fr, 26f, 26f, bp)

    // teks
    var y = 300f; val x = fx + fs + 44f; val lebar = w - x - 50f
    val nm = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textSize = 50f }
    val baris = StaticLayoutBantu.pecah(p.optString("nama"), nm, lebar, 2)
    baris.forEach { cv.drawText(it, x, y, nm); y += 58f }
    val kc = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt(); textSize = 36f }
    y += 6f; cv.drawText("NPA $npa", x, y, kc); y += 50f
    kc.textSize = 31f
    for (t in listOf(p.optString("ranting"), p.optString("tempat"), p.optString("status"))) {
        if (t.isEmpty()) continue
        StaticLayoutBantu.pecah(t, kc, lebar, 2).forEach { cv.drawText(it, x, y, kc); y += 40f }
    }
    val ft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xC0FFFFFF.toInt(); textSize = 28f }
    cv.drawText("Persatuan Guru Republik Indonesia", 60f, h - 40f, ft)
    return out
}

/** Pemecah teks sederhana per lebar (tanpa StaticLayout agar ringan). */
object StaticLayoutBantu {
    fun pecah(t: String, p: Paint, lebar: Float, maksBaris: Int): List<String> {
        val kata = t.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val h = ArrayList<String>(); var cur = ""
        for (k in kata) {
            val coba = if (cur.isEmpty()) k else "$cur $k"
            if (p.measureText(coba) <= lebar) cur = coba else { if (cur.isNotEmpty()) h.add(cur); cur = k }
        }
        if (cur.isNotEmpty()) h.add(cur)
        if (h.size > maksBaris) { val s = h.take(maksBaris).toMutableList(); s[maksBaris - 1] = s[maksBaris - 1].trimEnd() + "…"; return s }
        return h
    }
}

/** PDF bukti/rekap kehadiran. Satu kegiatan = "Bukti Kehadiran"; banyak = "Rekap Kehadiran". */
fun bagikanBuktiHadir(c: Context, p: JSONObject, tahun: Int, daftar: List<JSONObject>) {
    if (daftar.isEmpty()) return
    try {
        val doc = PdfDocument()
        val lbr = 595; val tgi = 842
        var hal = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(lbr, tgi, hal).create())
        var cv = page.canvas
        val navy = 0xFF0D2F68.toInt()
        val judul = if (daftar.size == 1) "BUKTI KEHADIRAN" else "REKAP KEHADIRAN $tahun"
        val tp = Paint(Paint.ANTI_ALIAS_FLAG)
        fun kepala() {
            tp.color = navy; tp.style = Paint.Style.FILL
            cv.drawRect(0f, 0f, lbr.toFloat(), 96f, tp)
            val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            cv.drawCircle(58f, 48f, 32f, wp)
            logoBmp(c)?.let { cv.drawBitmap(it, null, RectF(30f, 20f, 86f, 76f), Paint(Paint.FILTER_BITMAP_FLAG)) }
            wp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); wp.textSize = 18f
            cv.drawText("PGRI CABANG TIRTOYUDO", 102f, 44f, wp)
            wp.typeface = Typeface.DEFAULT; wp.textSize = 12f
            cv.drawText("Persatuan Guru Republik Indonesia", 102f, 64f, wp)
        }
        kepala()
        tp.color = navy; tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); tp.textSize = 20f
        tp.textAlign = Paint.Align.CENTER
        cv.drawText(judul, lbr / 2f, 140f, tp)
        tp.textAlign = Paint.Align.LEFT
        tp.typeface = Typeface.DEFAULT; tp.textSize = 13f; tp.color = 0xFF222222.toInt()
        var y = 180f
        fun baris(k: String, v: String) { cv.drawText(k, 50f, y, tp); cv.drawText(": $v", 150f, y, tp); y += 22f }
        baris("Nama", p.optString("nama")); baris("NPA", p.optString("npa"))
        baris("Ranting", p.optString("ranting")); baris("Tempat tugas", p.optString("tempat"))
        y += 14f
        if (daftar.size == 1) {
            val k = daftar[0]
            baris("Kegiatan", k.optString("judul")); baris("Tanggal", k.optString("tanggal"))
            if (k.optString("lokasi").isNotEmpty()) baris("Lokasi", k.optString("lokasi"))
            if (k.optString("waktu").isNotEmpty()) baris("Waktu absen", k.optString("waktu"))
            y += 10f
            cv.drawText("Dengan ini tercatat bahwa yang bersangkutan hadir pada kegiatan di atas.", 50f, y, tp); y += 20f
        } else {
            tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            cv.drawText("No", 50f, y, tp); cv.drawText("Kegiatan", 80f, y, tp); cv.drawText("Tanggal", 400f, y, tp); y += 8f
            val ln = Paint().apply { color = 0xFFBBBBBB.toInt(); strokeWidth = 1f }
            cv.drawLine(50f, y, lbr - 50f, y, ln); y += 18f
            tp.typeface = Typeface.DEFAULT
            daftar.forEachIndexed { i, k ->
                if (y > tgi - 120f) {
                    doc.finishPage(page); hal++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(lbr, tgi, hal).create()); cv = page.canvas; kepala(); y = 140f
                }
                cv.drawText("${i + 1}", 50f, y, tp)
                val t = StaticLayoutBantu.pecah(k.optString("judul"), tp, 300f, 2)
                t.forEachIndexed { j, s -> cv.drawText(s, 80f, y + j * 16f, tp) }
                cv.drawText(k.optString("tanggal"), 400f, y, tp)
                y += 16f * t.size.coerceAtLeast(1) + 8f
            }
            y += 6f
            tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            cv.drawText("Total hadir: ${daftar.size} kegiatan", 50f, y, tp); y += 20f
        }
        tp.typeface = Typeface.DEFAULT; tp.textSize = 10f; tp.color = 0xFF666666.toInt()
        val tgl = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date())
        cv.drawText("Dicetak dari aplikasi PGRI SMART pada $tgl. Keabsahan dapat diverifikasi ke pengurus PGRI Cabang Tirtoyudo.", 50f, tgi - 40f, tp)
        doc.finishPage(page)
        val f = berkas(c, if (daftar.size == 1) "bukti-kehadiran.pdf" else "rekap-kehadiran-$tahun.pdf")
        f.outputStream().use { doc.writeTo(it) }
        doc.close()
        kirim(c, f, "application/pdf", judul)
    } catch (_: Exception) {}
}
