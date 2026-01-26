package com.example.expensetracker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore extension
val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    // 1. Read Name (Default to "User" if not found)
    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME] ?: "User"
        }

    // 2. Write Name
    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    // Read the flag (Default is false)
    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_SETUP_COMPLETE] ?: false
        }

    // Write the flag
    suspend fun setSetupComplete() {
        context.dataStore.edit { preferences ->
            preferences[IS_SETUP_COMPLETE] = true
        }
    }
}