package com.omricat.treasurehunt

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omricat.treasurehunt.ui.theme.TreasureHuntTheme
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.android.rememberAndroidScreenAwareNavigator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.json.Json
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Parcelize
data object MainScreen : Screen {
    data class State(
        val huntState: HuntState,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data object ClickScanQr : Event

        data class ClickNextRound(val beforeRound: HuntState.BeforeRound) : Event
    }
}

class MainScreenPresenter(
    private val dataStore: DataStore<Preferences>,
    private val mainActivity: MainActivity,
    private val navigator: Navigator,
) : Presenter<MainScreen.State> {

    private val DATA_PREFERENCES_KEY = stringPreferencesKey("data")

    @Composable
    override fun present(): MainScreen.State {

        val scope = rememberCoroutineScope()

        val huntFlow = remember {
            dataStore.data
                .map { preferences ->
                    val prefValue = preferences[DATA_PREFERENCES_KEY]
                    val huntState =
                        if (prefValue.isNullOrBlank()) {
                            HuntState.init()
                        } else {
                            Json.decodeFromString<HuntState>(prefValue)
                        }
                    huntState
                }
                .distinctUntilChanged()
        }

        val huntState by huntFlow.collectAsState(initial = HuntState.init())

        var errorMessage by remember { mutableStateOf<String?>(null) }

        val starter =
            QrScannerAndroidScreenStarter(mainActivity, scope) { barcode ->
                val scannedClueId =
                    try {
                        ClueId.valueOf(barcode?.rawValue ?: "")
                    } catch (_: IllegalArgumentException) {
                        errorMessage = "I don't recognise that QR code!"
                        return@QrScannerAndroidScreenStarter
                    }
                val solveResult = (huntState as HuntState.Round).trySolveClue(scannedClueId)
                when (solveResult.negativeResult) {
                    HuntState.SolveResult.NegativeResult.CLUE_NOT_IN_ROUND -> {
                        errorMessage = "That isn't correct! Try somewhere else!"
                    }

                    HuntState.SolveResult.NegativeResult.ALREADY_SOLVED -> {
                        errorMessage = "You've already found that one!"
                    }

                    null -> {
                        scope.launch {
                            dataStore.edit { preferences ->
                                preferences[DATA_PREFERENCES_KEY] =
                                    Json.encodeToString(solveResult.nextHuntStateState)
                            }
                        }
                    }
                }
            }
        val resultNavigator = rememberAndroidScreenAwareNavigator(navigator, starter)

        return MainScreen.State(huntState = huntState, errorMessage) { event ->
            when (event) {
                is MainScreen.Event.ClickScanQr -> resultNavigator.goTo(QrScannerAndroidScreen)
                is MainScreen.Event.ClickNextRound ->
                    scope.launch {
                        dataStore.edit { preferences ->
                            preferences[DATA_PREFERENCES_KEY] =
                                Json.encodeToString(event.beforeRound.round)
                        }
                    }
            }
        }
    }

    class Factory(
        private val dataStore: DataStore<Preferences>,
        private val mainActivity: MainActivity,
    ) : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext,
        ): Presenter<*>? =
            when (screen) {
                is MainScreen -> MainScreenPresenter(dataStore, mainActivity, navigator)
                else -> null
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(state: MainScreen.State, modifier: Modifier = Modifier) {

    if (state.errorMessage.isNullOrBlank().not()) {
        val context = LocalContext.current
        LaunchedEffect(state.errorMessage) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    when (state.huntState) {
        is HuntState.BeforeRound ->
            BeforeRound(state.huntState, modifier) {
                state.eventSink(MainScreen.Event.ClickNextRound(state.huntState))
            }

        is HuntState.Round ->
            TreasureHuntRound(state.huntState, modifier) {
                state.eventSink(MainScreen.Event.ClickScanQr)
            }

        is HuntState.Complete -> Complete()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeforeRound(
    beforeRound: HuntState.BeforeRound,
    modifier: Modifier = Modifier,
    onClickNextRound: () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                title = { Text(stringResource(R.string.app_name), maxLines = 1) },
            )
        },
    ) { innerPadding ->
        val round1Description =
            """
            |Welcome to your Treasure Hunt!
            |
            |You have to find and scan all the clues.
            |
            |When you've scanned all the clues for one round, you'll move onto the next.
            |
            |Have fun and good luck!"""
                .trimMargin()

        val round2Description =
            """
            |Well done!
            |
            |You've completed the first round.
            |
            |Keep going!
            |
            |I wonder how many rounds there are..."""
                .trimMargin()

        val round3Description =
            """
            |Another round complete!
            |
            |Surely you must be near the end 
            |of your quest now!"""
                .trimMargin()

        val round4Description =
            """
            |Amazing!
            |
            |You're nearly finished. 
            |There's only clue left to find.
            |
            |I hope there's some 
            |amazing treasure at the end!"""
                .trimMargin()

        val (description, buttonText) =
            when (beforeRound.roundId) {
                RoundId.ONE -> round1Description to "Let's Go!"
                RoundId.TWO -> round2Description to "Next Round"
                RoundId.THREE -> round3Description to "On To Round 3"
                RoundId.FOUR -> round4Description to "Last One!"
            }

        Column(
            modifier = modifier.padding(innerPadding).fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = description,
                modifier = modifier.fillMaxHeight(0.8f),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onClickNextRound,
                modifier = modifier.defaultMinSize(minWidth = 160.dp),
            ) {
                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Complete(modifier: Modifier = Modifier) {
    val party: Party = remember {
        Party(emitter = Emitter(duration = 30, TimeUnit.SECONDS).perSecond(30))
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        KonfettiView(modifier = modifier.fillMaxSize(), parties = listOf(party))
        Text(
            "Woohoo!\nYou've done it!",
            modifier = Modifier.wrapContentSize(),
            textAlign = TextAlign.Center,
            style =
                TextStyle(
                    fontSize = 52.sp,
                    brush = Brush.linearGradient(colors = listOf(Color.Red, Color.Yellow)),
                ),
            maxLines = 2,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureHuntRound(
    round: HuntState.Round,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                title = { Text(stringResource(R.string.app_name), maxLines = 1) },
                actions = { IconButton(onClick = onActionClick) { ScanIcon() } },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onActionClick, shape = CircleShape) { ScanIcon() }
        },
    ) { innerPadding ->
        Column(
            modifier =
                modifier.padding(innerPadding).verticalScroll(rememberScrollState()).fillMaxWidth()
        ) {
            round.clues.forEach { clue -> Clue(clue) }
        }
    }
}

@Composable
private fun ScanIcon() {
    Icon(painterResource(R.drawable.barcode_scanner_24px), "Scan QR code button")
}

@Composable
fun Clue(clue: Clue, modifier: Modifier = Modifier) {
    val clueImage = painterResource(clue.clueId.getDrawable())

    Box(modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        if (clue.solved) {
            val contrast = 0.5f
            val filter =
                ColorFilter.colorMatrix(
                    ColorMatrix(
                            floatArrayOf(
                                contrast,
                                0f,
                                0f,
                                0f,
                                0f,
                                0f,
                                contrast,
                                0f,
                                0f,
                                0f,
                                0f,
                                0f,
                                contrast,
                                0f,
                                0f,
                                0f,
                                0f,
                                0f,
                                1f,
                                0f,
                            )
                        )
                        .apply {
                            val saturationMatrix = ColorMatrix()
                            saturationMatrix.setToSaturation(0f)
                            timesAssign(saturationMatrix)
                        }
                )

            Image(
                painter = clueImage,
                contentDescription = clue.clueId.name,
                modifier =
                    modifier.blur(radius = 6.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle),
                colorFilter = filter,
            )
            Text(
                "Found✓",
                fontSize = 48.sp,
                color = Color(0xA0A0A0A0),
                fontWeight = FontWeight.Bold,
            )
        } else {
            Image(painter = clueImage, contentDescription = clue.clueId.name)
        }
    }
}

fun ClueId.getDrawable(): Int =
    when (this) {
        ClueId.SUN_ROOM_CUPBOARD -> R.drawable.sun_room_cupboard
        ClueId.BIKE_SHED -> R.drawable.bike_shed
        ClueId.WASHING_MACHINE -> R.drawable.washing_machine
        ClueId.PIANO_STOOL -> R.drawable.piano_stool
        ClueId.ENSUITE_CUPBOARD -> R.drawable.ensuite_cupboard
        ClueId.SPARE_ROOM_WARDROBE -> R.drawable.spare_room_wardrobe
        ClueId.GARDEN_TOOL_SHED -> R.drawable.garden_tool_shed
        ClueId.PORCH -> R.drawable.porch
        ClueId.KITCHEN_CUPBOARD -> R.drawable.kitchen_cupboard
        ClueId.LANDING_CUPBOARD -> R.drawable.landing_cupboard
    }

// @Preview(showBackground = true, device = "id:pixel_8")
// @Composable
// fun TreasureHuntPreview() {
//    val round =
//        (round1.trySolveClue(ClueId.WASHING_MACHINE).nextHuntStateState as HuntState.Round)
//            .trySolveClue(ClueId.BIKE_SHED)
//            .nextHuntStateState as HuntState.Round
//    TreasureHuntTheme { TreasureHuntRound(round, onActionClick = {}) }
// }
//
// @Preview(showBackground = true, device = "id:pixel_8")
// @Composable
// fun CompletePreview() {
//    TreasureHuntTheme { Complete() }
// }

@Preview(showBackground = true, device = "id:pixel_8")
@Composable
fun BeforeRoundPreview() {
    TreasureHuntTheme { BeforeRound(HuntState.BeforeRound(round4.roundId), onClickNextRound = {}) }
}
