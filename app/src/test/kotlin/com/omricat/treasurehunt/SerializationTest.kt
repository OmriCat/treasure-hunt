package com.omricat.treasurehunt

import kotlinx.serialization.json.Json
import org.junit.Test

class SerializationTest {

    @Test
    fun canSerializeRound() {
        val round = HuntState.init()
        val serialized = Json.encodeToString(round)
    }
}
