package com.dshmulti.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 一个核心 = 一个 DSH 服务地址 */
data class Core(val name: String, val url: String)

object CoreStore {

    private const val PREFS = "dsh_multi"
    private const val K_CORES = "cores"
    private const val K_LAST = "last"
    private const val K_AUTO = "auto_open"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Core> {
        val out = mutableListOf<Core>()
        val raw = prefs(c).getString(K_CORES, null) ?: return out
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("name").ifBlank { "未命名" }
                val url = o.optString("url")
                if (url.isNotBlank()) out.add(Core(name, url))
            }
        }
        return out
    }

    fun save(c: Context, cores: List<Core>) {
        val arr = JSONArray()
        cores.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
        prefs(c).edit().putString(K_CORES, arr.toString()).apply()
    }

    fun upsert(c: Context, core: Core) {
        val list = load(c)
        val i = list.indexOfFirst { it.url == core.url }
        if (i >= 0) list[i] = core else list.add(core)
        save(c, list)
    }

    fun remove(c: Context, url: String) {
        val list = load(c).filterNot { it.url == url }
        save(c, list)
        if (last(c) == url) prefs(c).edit().remove(K_LAST).apply()
    }

    fun last(c: Context): String? = prefs(c).getString(K_LAST, null)

    fun setLast(c: Context, url: String) {
        prefs(c).edit().putString(K_LAST, url).apply()
    }

    fun autoOpen(c: Context): Boolean = prefs(c).getBoolean(K_AUTO, true)

    fun setAutoOpen(c: Context, v: Boolean) {
        prefs(c).edit().putBoolean(K_AUTO, v).apply()
    }

    fun nameOf(c: Context, url: String): String? =
        load(c).firstOrNull { it.url == url }?.name

    /** 192.168.1.5:3080 -> http://192.168.1.5:3080 */
    fun normalize(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        val scheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
        if (!scheme.containsMatchIn(s)) s = "http://$s"
        while (s.length > 8 && s.endsWith("/")) s = s.dropLast(1)
        return s
    }

    /** 只用来显示：http://127.0.0.1:3080 -> 127.0.0.1:3080 */
    fun pretty(url: String): String = url
        .removePrefix("http://")
        .removePrefix("https://")
        .trimEnd('/')
}
