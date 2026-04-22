package com.gamevault.android.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamevault_prefs")

object Prefs {
    val SERVER_URL     = stringPreferencesKey("server_url")
    val LOCAL_URL      = stringPreferencesKey("local_url")
    val ROMS_ROOT_URI  = stringPreferencesKey("roms_root_uri")
    val USERNAME       = stringPreferencesKey("username")
    val ROLE           = stringPreferencesKey("role")
    val SAVED_PASSWORD = stringPreferencesKey("saved_password")
    val REMEMBER_ME    = booleanPreferencesKey("remember_me")

    fun serverUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[SERVER_URL] ?: "" }

    fun localUrl(context: Context): Flow<String> =
        context.dataStore.data.map { it[LOCAL_URL] ?: "" }

    fun romsRootUri(context: Context): Flow<String> =
        context.dataStore.data.map { it[ROMS_ROOT_URI] ?: "" }

    suspend fun setServerUrl(context: Context, url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setLocalUrl(context: Context, url: String) {
        context.dataStore.edit { it[LOCAL_URL] = url }
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

    suspend fun saveCredentials(context: Context, password: String, rememberMe: Boolean) {
        context.dataStore.edit {
            it[REMEMBER_ME] = rememberMe
            if (rememberMe) it[SAVED_PASSWORD] = password else it.remove(SAVED_PASSWORD)
        }
    }

    suspend fun clearUser(context: Context) {
        context.dataStore.edit {
            it.remove(USERNAME)
            it.remove(ROLE)
            it.remove(SAVED_PASSWORD)
            it.remove(REMEMBER_ME)
        }
    }
}
