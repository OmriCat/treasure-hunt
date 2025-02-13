package com.omricat.treasurehunt

import com.omricat.treasurehunt.ClueId.BIKE_SHED
import com.omricat.treasurehunt.ClueId.ENSUITE_CUPBOARD
import com.omricat.treasurehunt.ClueId.GARDEN_TOOL_SHED
import com.omricat.treasurehunt.ClueId.KITCHEN_CUPBOARD
import com.omricat.treasurehunt.ClueId.LANDING_CUPBOARD
import com.omricat.treasurehunt.ClueId.PIANO_STOOL
import com.omricat.treasurehunt.ClueId.PORCH
import com.omricat.treasurehunt.ClueId.SPARE_ROOM_WARDROBE
import com.omricat.treasurehunt.ClueId.SUN_ROOM_CUPBOARD
import com.omricat.treasurehunt.ClueId.WASHING_MACHINE
import kotlinx.serialization.Serializable

@Serializable
sealed class HuntState {

    @Serializable
    class Round(val roundId: RoundId, val clues: Set<Clue>) : HuntState() {

        fun trySolveClue(id: ClueId): SolveResult {
            val clueToSolve = (clues.find { clue -> clue.clueId == id })
            if (clueToSolve == null)
                return SolveResult(this, SolveResult.NegativeResult.CLUE_NOT_IN_ROUND)
            else {
                val newClues = clues - clueToSolve + clueToSolve.copy(solved = true)
                return SolveResult(
                    if (!newClues.all { it.solved }) {
                        Round(roundId = this.roundId, newClues)
                    } else {
                        nextRound(roundId)
                    }
                )
            }
        }
    }

    data class SolveResult(
        val nextHuntStateState: HuntState,
        val negativeResult: NegativeResult? = null,
    ) {
        enum class NegativeResult {
            CLUE_NOT_IN_ROUND,
            ALREADY_SOLVED,
        }
    }

    @Serializable object Complete : HuntState()

    companion object {
        fun init(): HuntState = round1
    }
}

@Serializable data class Clue(val clueId: ClueId, val solved: Boolean = false)

internal val round1 =
    HuntState.Round(
        RoundId.ONE,
        setOf(Clue(SUN_ROOM_CUPBOARD), Clue(BIKE_SHED), Clue(WASHING_MACHINE)),
    )

internal val round2 =
    HuntState.Round(
        RoundId.TWO,
        setOf(Clue(PIANO_STOOL), Clue(SPARE_ROOM_WARDROBE), Clue(ENSUITE_CUPBOARD)),
    )

internal val round3 =
    HuntState.Round(
        RoundId.THREE,
        setOf(Clue(PORCH), Clue(GARDEN_TOOL_SHED), Clue(LANDING_CUPBOARD)),
    )
internal val round4 = HuntState.Round(RoundId.FOUR, setOf(Clue(KITCHEN_CUPBOARD)))

internal fun nextRound(roundId: RoundId): HuntState =
    when (roundId) {
        RoundId.ONE -> round2
        RoundId.TWO -> round3
        RoundId.THREE -> round4
        RoundId.FOUR -> HuntState.Complete
    }

@Serializable
enum class RoundId {
    ONE,
    TWO,
    THREE,
    FOUR,
}

@Serializable
enum class ClueId {
    SUN_ROOM_CUPBOARD,
    BIKE_SHED,
    WASHING_MACHINE,
    PIANO_STOOL,
    ENSUITE_CUPBOARD,
    SPARE_ROOM_WARDROBE,
    GARDEN_TOOL_SHED,
    PORCH,
    KITCHEN_CUPBOARD,
    LANDING_CUPBOARD,
}
