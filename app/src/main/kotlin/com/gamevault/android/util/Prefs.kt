package com.gamevault.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamevault_prefs")

object Prefs {
    val SERVER_URL = stringPreferencesKey("server_url")
    val ROMS_ROOT_URI = stringPreferencesKey("roms_root_uri")
    val USERNAME = stringPreferencesKey("username")
    val ROLE = stringPreferencesKey("role")

    fun serverUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[SERVER_URL] ?: "" }

    fun romsRootUri(context: Context): Flow<String> =
        context.dataStore.data.map { it[ROMS_ROOT_URI] ?: "" }

    suspend fun setServerUrl(context: Context, url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setRomsRootUri(context: Context, uri: String) {
        context.dataStore.edit { it[ROMS_ROOT_URI] = uri }
    }

    suspend fun setUser(context: Context, username: String, role: String) {
        context.dataStore.edit {
            it[USERNAME] = username
            it[ROLE] = role
        }
    }

    suspend fun clearUser(context: Context) {
        context.dataStore.edit {
            it.remove(USERNAME)
            it.remove(ROLE)
        }
    }
}
