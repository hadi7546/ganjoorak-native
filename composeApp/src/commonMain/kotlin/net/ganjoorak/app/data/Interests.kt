package net.ganjoorak.app.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.remote.GanjoorJson

/** A topic of poems the home feed can be narrowed to, e.g. «عاشقانه». */
data class PoemInterest(val key: String, val label: String)

val PoemInterests = listOf(
    PoemInterest("love", "عاشقانه"),
    PoemInterest("wine", "باده و خرابات"),
    PoemInterest("spring", "بهار و طبیعت"),
    PoemInterest("seeking", "طلب و سلوک"),
    PoemInterest("annihilation", "فنا و ترک خود"),
    PoemInterest("mysticabstract", "عرفان انتزاعی"),
    PoemInterest("unity", "توحید و فقر"),
    PoemInterest("prayer", "مناجات با خدا"),
    PoemInterest("contentment", "قناعت و بی‌نیازی"),
    PoemInterest("time", "گذر عمر و روزگار"),
    PoemInterest("praise", "مدح و ستایش"),
    PoemInterest("epic", "حماسه و روایت"),
    PoemInterest("satire", "هجو و رسوایی"),
)

/** What the picker pre-selects the first time it opens. */
val DefaultSuggestedInterestKeys = listOf("love", "wine", "spring", "seeking", "time", "prayer", "epic", "satire")

fun interestByKey(key: String): PoemInterest? = PoemInterests.firstOrNull { it.key == key }

/** Keeps only known keys, in the canonical order, without duplicates. */
fun sanitizeInterestKeys(keys: Collection<String>): List<String> =
    PoemInterests.map { it.key }.filter { it in keys }

@Serializable
private class InterestFile(val poemIds: List<Int> = emptyList(), val poetSlugs: List<String> = emptyList())

/**
 * Hands out poem ids per interest from the bundled lists (built from Ganjoor's embeddings): each
 * list is shuffled and used up before it is shuffled again, and consecutive poems avoid repeating
 * the same poet.
 */
class InterestSource(private val read: suspend (key: String) -> String) {
    private class Entry(val poemId: Int, val poetSlug: String)

    private class Pool(val all: List<Entry>) {
        val pending = ArrayDeque<Entry>()
        var lastPoet: String? = null
    }

    private val mutex = Mutex()
    private val pools = HashMap<String, Pool>()

    private suspend fun pool(key: String): Pool = pools[key] ?: run {
        val file = GanjoorJson.decodeFromString<InterestFile>(read(key))
        val entries = file.poemIds.mapIndexedNotNull { index, id ->
            if (id > 0) Entry(id, file.poetSlugs.getOrNull(index).orEmpty()) else null
        }
        Pool(entries).also { pools[key] = it }
    }

    /** The id of the next poem of [key], or null when that interest is unknown or empty. */
    suspend fun nextPoemId(key: String): Int? = mutex.withLock {
        if (interestByKey(key) == null) return@withLock null
        val pool = pool(key)
        if (pool.all.isEmpty()) return@withLock null
        if (pool.pending.isEmpty()) pool.pending.addAll(pool.all.shuffled())
        val index = pool.pending.indexOfFirst { it.poetSlug != pool.lastPoet }.takeIf { it >= 0 } ?: 0
        val entry = pool.pending.removeAt(index)
        pool.lastPoet = entry.poetSlug.ifEmpty { null }
        entry.poemId
    }
}
