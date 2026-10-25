package net.ganjoorak.app

import kotlinx.coroutines.test.runTest
import net.ganjoorak.app.data.InterestSource
import net.ganjoorak.app.data.sanitizeInterestKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class InterestsTest {
    private val love = """{"label":"عاشقانه","poemIds":[1,2,3,4],"poetSlugs":["hafez","hafez","saadi","saadi"]}"""

    @Test
    fun servesEveryPoemOnceBeforeRepeating() = runTest {
        val source = InterestSource { love }
        val round = List(4) { source.nextPoemId("love") }
        assertEquals(setOf(1, 2, 3, 4), round.toSet())
        assertEquals(4, round.size)
    }

    @Test
    fun avoidsTheSamePoetTwiceInARowWhenItCan() = runTest {
        val poetOf = mapOf(1 to "hafez", 2 to "hafez", 3 to "saadi", 4 to "saadi")
        val source = InterestSource { love }
        val ids = List(4) { source.nextPoemId("love")!! }
        for (i in 1 until 3) assertNotEquals(poetOf[ids[i - 1]], poetOf[ids[i]])
    }

    @Test
    fun unknownInterestGivesNothing() = runTest {
        assertNull(InterestSource { love }.nextPoemId("nope"))
    }

    @Test
    fun sanitizeKeepsKnownKeysInCanonicalOrder() {
        assertEquals(listOf("love", "wine"), sanitizeInterestKeys(listOf("wine", "bogus", "love", "wine")))
    }
}
