package com.omricat.treasurehunt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omricat.treasurehunt.ui.theme.TreasureHuntTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataStore = TreasureHuntApp.fromContext(this).dataStore

        val circuit =
            Circuit.Builder()
                .addPresenterFactory(MainScreenPresenter.Factory(dataStore, this))
                .addUi<MainScreen, MainScreen.State> { state, modifier -> Main(state, modifier) }
                .build()

        setContent {
            val backStack = rememberSaveableBackStack(root = MainScreen)
            val navigator = rememberCircuitNavigator(backStack)
            TreasureHuntTheme {
                CircuitCompositionLocals(circuit) { NavigableCircuitContent(navigator, backStack) }
            }
        }
    }
}
