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

fun loadPlaylistSongs(c: Context): Map<String, Set<Long>> {
    val obj = org.json.JSONObject(prefs(c).getString("playlist_songs", "{}") ?: "{}")
    return buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val name = keys.next()
            val arr = obj.optJSONArray(name) ?: JSONArray()
            put(name, buildSet { for (i in 0 until arr.length()) add(arr.optLong(i)) })
        }
    }
}
fun togglePlaylistSong(c: Context, current: Map<String, Set<Long>>, playlist: String, songId: Long): Map<String, Set<Long>> {
    val updated = current.toMutableMap()
    val ids = (current[playlist] ?: emptySet()).toMutableSet()
    if (!ids.add(songId)) ids.remove(songId)
    updated[playlist] = ids
    val obj = org.json.JSONObject()
    updated.forEach { (name, values) ->
        val arr = JSONArray()
        values.forEach { arr.put(it) }
        obj.put(name, arr)
    }
    prefs(c).edit().putString("playlist_songs", obj.toString()).apply()
    return updated
}
