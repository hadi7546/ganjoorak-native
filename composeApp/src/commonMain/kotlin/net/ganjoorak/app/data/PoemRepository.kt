package net.ganjoorak.app.data

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.ganjoorak.app.data.remote.GanjoorApi
import net.ganjoorak.app.data.remote.PoemDto

/** An error with a message that is safe to show to the user. */
class GanjoorException(message: String, cause: Throwable? = null) : Exception(message, cause)

class PoemRepository(
    private val api: GanjoorApi,
    private val echolalia: EcholaliaSource,
    private val local: LocalSource,
) {
    private val mutex = Mutex()
    private var poetGroups: List<PoetGroup>? = null
    private val poems = LruCache<String, Poem>(64)
    private val categories = LruCache<Int, Category>(32)
    private val timings = LruCache<Int, List<VerseTiming>>(16)

    /** Ganjoor's classical poets, grouped by century. */
    suspend fun poetGroups(): List<PoetGroup> = mutex.withLock {
        poetGroups ?: request("دریافت فهرست شاعران ممکن نشد") {
            api.centuries()
                .filter { it.poets.isNotEmpty() }
                .sortedBy { it.halfCenturyOrder }
                .map { century ->
                    PoetGroup(
                        title = century.name.orEmpty(),
                        poets = century.poets.filter { it.published }.map { it.toPoet() },
                    )
                }
                .ifEmpty {
                    listOf(PoetGroup("شاعران", api.poets().filter { it.published }.map { it.toPoet() }))
                }
        }.also { poetGroups = it }
    }

    /**
     * The bundled contemporary poets plus Echolalia's poets grouped like the website. Echolalia
     * being unreachable only drops its groups.
     */
    suspend fun contemporaryGroups(): List<PoetGroup> {
        val bundled = PoetGroup(LOCAL_POETS_GROUP, request("دریافت شاعران معاصر ممکن نشد") { local.poets() })
        val echolaliaPoets = try {
            request("دریافت شاعران اکولالیا ممکن نشد") { echolalia.poets() }
        } catch (e: GanjoorException) {
            emptyList()
        }
        val echolaliaGroups = echolaliaPoets
            .groupBy { it.group ?: "اکولالیا" }
            .map { (title, poets) -> PoetGroup(title, poets) }
        return listOf(bundled) + echolaliaGroups
    }

    suspend fun poet(key: String): PoetDetail {
        val parsed = PoetKey.parse(key) ?: throw GanjoorException("شاعر پیدا نشد")
        return request("دریافت اطلاعات شاعر ممکن نشد") {
            when (parsed.source) {
                PoetSource.Ganjoor -> {
                    val dto = api.poet(parsed.ref.toIntOrNull() ?: throw GanjoorException("شاعر پیدا نشد"))
                    val poet = dto.poet?.toPoet() ?: throw GanjoorException("شاعر پیدا نشد")
                    PoetDetail(poet = poet, root = dto.toCategory())
                }
                PoetSource.Echolalia -> {
                    val poet = echolalia.poet(parsed.ref)
                    PoetDetail(poet, poet.flatCategory(echolalia.poemList(poet)))
                }
                PoetSource.Local -> {
                    val poet = local.poet(parsed.ref)
                    PoetDetail(poet, poet.flatCategory(local.poemList(parsed.ref)))
                }
            }
        }
    }

    private fun Poet.flatCategory(poems: List<PoemSummary>) = Category(
        id = 0,
        title = name,
        description = description,
        poet = this,
        ancestors = emptyList(),
        children = emptyList(),
        poems = poems,
    )

    suspend fun category(id: Int): Category = categories[id] ?: request("دریافت مجموعه ممکن نشد") {
        api.category(id).toCategory()
    }.also { categories[id] = it }

    suspend fun poem(ref: PoemRef): Poem = poems[ref.key] ?: request("دریافت شعر ممکن نشد") {
        when (ref.source) {
            PoetSource.Ganjoor -> api.poem(ref.id).toPoem()
            PoetSource.Echolalia -> echolalia.poem(ref.id)
            PoetSource.Local -> local.poem(ref.poetSlug, ref.id)
        }
    }.also { poems[ref.key] = it }

    /** A random poem by the poet with [poetKey], or from all of Ganjoor when it's null. */
    suspend fun randomPoem(poetKey: String? = null): Poem {
        val parsed = poetKey?.let { PoetKey.parse(it) }
        return request("دریافت شعر تصادفی ممکن نشد") {
            when (parsed?.source) {
                null, PoetSource.Ganjoor -> {
                    val dto: PoemDto = api.randomPoem(parsed?.ref?.toIntOrNull())
                    if (dto.isIncomplete() && dto.id > 0) api.poem(dto.id).toPoem() else dto.toPoem()
                }
                PoetSource.Echolalia -> echolalia.randomPoem(parsed!!.ref)
                PoetSource.Local -> local.randomPoem(parsed!!.ref)
            }
        }.also { poems[it.ref.key] = it }
    }

    /**
     * Full-text search. Ganjoor is paged; on the first page the bundled poets and Echolalia are
     * searched too and their matches lead the results.
     */
    suspend fun search(term: String, page: Int, pageSize: Int = 20): SearchPage = coroutineScope {
        val extra = if (page == 1) {
            async {
                val bundled = runCatching { local.search(term) }.getOrDefault(emptyList())
                val world = try {
                    request("") { echolalia.search(term) }
                } catch (e: GanjoorException) {
                    emptyList()
                }
                bundled + world
            }
        } else {
            null
        }
        val ganjoor = request("جستجو ممکن نشد") {
            val response = api.search(term, page, pageSize)
            val hits = response.poems.filter { it.id > 0 }.map { it.toSearchHit(term) }
            val paging = response.paging
            SearchPage(
                hits = hits,
                page = page,
                hasNext = paging?.hasNextPage ?: (paging?.let { page < it.totalPages } ?: (hits.size >= pageSize)),
                total = paging?.totalCount,
            )
        }
        val extraHits = extra?.await().orEmpty()
        ganjoor.copy(hits = extraHits + ganjoor.hits, total = ganjoor.total?.plus(extraHits.size))
    }

    suspend fun semanticSearch(query: String, topK: Int = 20): SearchPage = request("جستجوی معنایی در دسترس نیست") {
        val hits = api.semanticSearch(query, topK).results.map { it.toSearchHit() }.filter { it.ref.id > 0 }
        SearchPage(hits = hits, page = 1, hasNext = false, total = hits.size)
    }

    suspend fun timings(recitationId: Int): List<VerseTiming> = timings[recitationId] ?: request("همگام‌سازی خوانش ممکن نشد") {
        api.recitationTimings(recitationId)
            .map { VerseTiming(verseOrder = it.verseOrder, startMs = it.audioStartMilliseconds) }
            .sortedBy { it.startMs }
    }.also { timings[recitationId] = it }

    private suspend fun <T> request(failure: String, block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        // Ktor can report a failed call as a CancellationException. Only a cancellation of the
        // caller itself should pass through silently; anything else is a failed request.
        if (!currentCoroutineContext().isActive) throw e
        println("Ganjoorak: $failure: $e, cause: ${e.cause}")
        throw GanjoorException("$failure. اتصال اینترنت را بررسی کنید", e)
    } catch (e: GanjoorException) {
        throw e
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) throw GanjoorException("مورد درخواستی پیدا نشد", e)
        throw GanjoorException(failure, e)
    } catch (e: ServerResponseException) {
        println("Ganjoorak: $failure: $e")
        throw GanjoorException("$failure. سرور پاسخ نمی‌دهد", e)
    } catch (e: Exception) {
        // The user only sees a generic message; keep the real cause in the log.
        println("Ganjoorak: $failure: $e")
        throw GanjoorException("$failure. اتصال اینترنت را بررسی کنید", e)
    }
}

internal class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>()

    operator fun get(key: K): V? = map.remove(key)?.also { map[key] = it }

    operator fun set(key: K, value: V) {
        map.remove(key)
        map[key] = value
        while (map.size > maxSize) map.remove(map.keys.first())
    }
}
