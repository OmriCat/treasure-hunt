package com.omricat.treasurehunt

import org.junit.Test

class AppStateTest {

    @Test
    fun initialRoundAllCluesNotSolved() {
        val state = HuntState.init() as HuntState.Round

        assert(state.clues.all { clue -> clue.solved.not() })
    }
}
