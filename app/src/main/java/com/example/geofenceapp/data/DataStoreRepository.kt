package com.example.geofenceapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.geofenceapp.util.Constants.PREFERENCE_DEFAULT_RADIUS
import com.example.geofenceapp.util.Constants.PREFERENCE_DEFAULT_RADIUS_DEFAULT
import com.example.geofenceapp.util.Constants.PREFERENCE_FIRST_LAUNCH
import com.example.geofenceapp.util.Constants.PREFERENCE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

@ViewModelScoped
class DataStoreRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object PreferenceKey {
        val firstLaunch = booleanPreferencesKey(PREFERENCE_FIRST_LAUNCH)
        val defaultRadius = floatPreferencesKey(PREFERENCE_DEFAULT_RADIUS)
    }
    val readFirstLaunch: Flow<Boolean> = context.dataStore.data
        .catch { exception->
            if (this is IOException){
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // No type safety.
           val firstLaunch =  preferences[PreferenceKey.firstLaunch] ?: true
            firstLaunch
        }
     suspend fun saveFirstLaunch (firstLaunch: Boolean) {
        context.dataStore.edit { settings ->
            settings[PreferenceKey.firstLaunch] = firstLaunch
        }
    }

    val readDefaultRadius: Flow<Float> = context.dataStore.data
        .catch { exception->
            if (this is IOException){
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // No type safety.
            val defaultRadius =  preferences[PreferenceKey.defaultRadius] ?: PREFERENCE_DEFAULT_RADIUS_DEFAULT
            defaultRadius
        }
    suspend fun saveDefaultRadius (defaultRadius: Float) {
        context.dataStore.edit { settings ->
            settings[PreferenceKey.defaultRadius] = defaultRadius
        }
    }
}