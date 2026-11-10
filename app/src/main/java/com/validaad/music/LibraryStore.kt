package com.validaad.music
import android.content.Context
import org.json.JSONArray
private const val PREFS = "validaad_music_library"
private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
private fun readIds(c: Context, key: String): List<Long> {
    val a = JSONArray(prefs(c).getString(key, "[]") ?: "[]")
    return buildList { for (i in 0 until a.length()) add(a.optLong(i)) }
}
private fun writeIds(c: Context, key: String, ids: List<Long>) {
    val a = JSONArray(); ids.forEach { a.put(it) }
    prefs(c).edit().putString(key, a.toString()).apply()
}
fun loadFavoriteIds(c: Context): Set<Long> = readIds(c, "favorites").toSet()
fun loadRecentIds(c: Context): List<Long> = readIds(c, "recent")
fun toggleFavorite(c: Context, current: Set<Long>, id: Long): Set<Long> {
    val result = current.toMutableSet().apply { if (!add(id)) remove(id) }
    writeIds(c, "favorites", result.toList()); return result
}
fun saveRecent(c: Context, current: List<Long>, id: Long): List<Long> {
    val result = (listOf(id) + current.filterNot { it == id }).take(30)
    writeIds(c, "recent", result); return result
}
fun loadPlaylists(c: Context): List<String> {
    val a = JSONArray(prefs(c).getString("playlists", "[]") ?: "[]")
    return buildList { for (i in 0 until a.length()) add(a.optString(i)) }
}
fun savePlaylist(c: Context, current: List<String>, name: String): List<String> {
    val result = current + name; val a = JSONArray()
    result.forEach { a.put(it) }; prefs(c).edit().putString("playlists", a.toString()).apply()
    return result
}