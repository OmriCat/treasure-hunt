package com.omricat.treasurehunt

import org.junit.Test

class AppStateTest {

    @Test
    fun initialRoundAllCluesNotSolved() {
        val state = HuntState.init() as HuntState.Round

        assert(state.clues.all { clue -> clue.solved.not() })
    }

    @Test
    fun solvingLastClueOfRoundReturnsNextRound() {
        val state =
            HuntState.Round(roundId = RoundId.ONE, clues = setOf(Clue(ClueId.WASHING_MACHINE)))

        val newState = state.trySolveClue(ClueId.WASHING_MACHINE)

        assert(newState is HuntState.SolveResult.Success)
        assert(
            ((newState as HuntState.SolveResult.Success).nextHuntState as HuntState.Round)
                .roundId == RoundId.TWO
        )
    }
}
