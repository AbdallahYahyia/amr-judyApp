package com.amrjudy.kids

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup / restore of everything the parent set up: settings (PIN, stars, chosen voice, videos list,
 * recorded-voice list) plus the recorded voices (.pcm) and the family photo. One .zip file.
 */
object Backup {
    private const val PREFS = "amrjudy"
    private val FILE_NAME = Regex("^(voice_-?\\d+\\.pcm|family\\.jpg)$")

    fun write(ctx: Context, out: OutputStream) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val js = JSONObject()
        for ((k, v) in prefs.all) {
            val e = JSONObject()
            when (v) {
                is String -> e.put("t", "s").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is Boolean -> e.put("t", "b").put("v", v)
                is Set<*> -> e.put("t", "set").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            js.put(k, e)
        }
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("prefs.json"))
            z.write(js.toString().toByteArray(Charsets.UTF_8))
            z.closeEntry()
            val files = ctx.filesDir.listFiles { f -> FILE_NAME.matches(f.name) } ?: emptyArray()
            for (f in files) {
                z.putNextEntry(ZipEntry("files/" + f.name))
                f.inputStream().use { it.copyTo(z) }
                z.closeEntry()
            }
        }
    }

    /** Returns the number of restored voice files, or -1 if the file is not a valid backup. */
    fun read(ctx: Context, input: InputStream): Int {
        var prefsJson: String? = null
        val files = HashMap<String, ByteArray>()
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val bytes = z.readBytes()
                if (e.name == "prefs.json") prefsJson = String(bytes, Charsets.UTF_8)
                else if (e.name.startsWith("files/")) {
                    val n = e.name.removePrefix("files/")
                    if (FILE_NAME.matches(n)) files[n] = bytes     // ignore anything with odd names (no path tricks)
                }
            }
        }
        val json = try { JSONObject(prefsJson ?: return -1) } catch (e: Exception) { return -1 }

        for ((n, b) in files) File(ctx.filesDir, n).writeBytes(b)

        val ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        ed.clear()
        for (k in json.keys()) {
            val e = json.getJSONObject(k)
            when (e.getString("t")) {
                "s" -> ed.putString(k, e.getString("v"))
                "i" -> ed.putInt(k, e.getInt("v"))
                "l" -> ed.putLong(k, e.getLong("v"))
                "f" -> ed.putFloat(k, e.getDouble("v").toFloat())
                "b" -> ed.putBoolean(k, e.getBoolean("v"))
                "set" -> {
                    val a = e.getJSONArray("v")
                    ed.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet())
                }
            }
        }
        ed.commit()
        return files.keys.count { it.startsWith("voice_") }
    }
}
