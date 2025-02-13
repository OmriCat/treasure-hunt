package com.omricat.treasurehunt

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.slack.circuit.foundation.Circuit

class TreasureHuntApp : Application() {
    val dataStore: DataStore<Preferences> by preferencesDataStore("appdata")

    companion object {
        fun fromContext(context: Context): TreasureHuntApp =
            context.applicationContext as TreasureHuntApp
    }
}
