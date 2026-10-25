package net.ganjoorak.app.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.data.remote.GanjoorJson

enum class ThemeMode { System, Light, Dark, Paper }

enum class PoemFont { Vazirmatn, Samim, Tanha, Shabnam, Gandom, Parastoo, Sahel, Nahid }

enum class CoupletLayout { Auto, Stacked, SideBySide }

/** Where the home feed draws its poems from. */
enum class FeedMode { All, Following }

/** A poet whose poems appear in the home feed; [key] is a [net.ganjoorak.app.data.Poet.key]. */
@Serializable
data class FollowedPoet(val key: String, val name: String)

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.Dark,
    val poemFont: PoemFont = PoemFont.Vazirmatn,
    val fontScale: Float = 1f,
    val coupletLayout: CoupletLayout = CoupletLayout.Auto,
    val showVerseNumbers: Boolean = false,
    val autoScrollWithAudio: Boolean = true,
    /** Empty means random poems from all of Ganjoor, like the website. */
    val followedPoets: List<FollowedPoet> = emptyList(),
    val feedMode: FeedMode = FeedMode.All,
    /** Topics offered as chips above the feed; see [net.ganjoorak.app.data.PoemInterests]. */
    val interestKeys: List<String> = emptyList(),
    /** The chips currently switched on; none means no narrowing. */
    val activeInterestKeys: List<String> = emptyList(),
    val hasChosenInterests: Boolean = false,
)

/** A JSON value persisted in [Settings] and mirrored as a [StateFlow]. */
private class PersistedValue<T>(
    private val settings: Settings,
    private val key: String,
    private val serializer: KSerializer<T>,
    default: T,
) {
    private val state = MutableStateFlow(
        settings.getStringOrNull(key)
            ?.let { runCatching { GanjoorJson.decodeFromString(serializer, it) }.getOrNull() }
            ?: default,
    )
    val flow: StateFlow<T> = state.asStateFlow()

    fun update(transform: (T) -> T) {
        state.update(transform)
        settings.putString(key, GanjoorJson.encodeToString(serializer, state.value))
    }
}

class SettingsStore(settings: Settings) {
    private val value = PersistedValue(settings, "app_settings", AppSettings.serializer(), AppSettings())
    val settings: StateFlow<AppSettings> = value.flow

    fun update(transform: (AppSettings) -> AppSettings) = value.update(transform)

    fun follow(key: String, name: String) = update {
        if (it.followedPoets.any { poet -> poet.key == key }) it else it.copy(followedPoets = it.followedPoets + FollowedPoet(key, name))
    }

    fun unfollow(key: String) = update { it.copy(followedPoets = it.followedPoets.filterNot { poet -> poet.key == key }) }

    fun toggleFollow(key: String, name: String) {
        if (settings.value.followedPoets.any { it.key == key }) unfollow(key) else follow(key, name)
    }

    fun setInterests(keys: List<String>) = update { settings ->
        val chosen = net.ganjoorak.app.data.sanitizeInterestKeys(keys)
        settings.copy(
            interestKeys = chosen,
            activeInterestKeys = settings.activeInterestKeys.filter { it in chosen },
            hasChosenInterests = true,
        )
    }

    fun toggleActiveInterest(key: String) = update {
        it.copy(
            activeInterestKeys = if (key in it.activeInterestKeys) it.activeInterestKeys - key else it.activeInterestKeys + key,
        )
    }

    fun clearActiveInterests() = update { it.copy(activeInterestKeys = emptyList()) }

    fun setFeedMode(mode: FeedMode) = update { it.copy(feedMode = mode) }
}

class LibraryStore(settings: Settings) {
    private val saved = PersistedValue(settings, "saved_poems", ListSerializer(SavedPoem.serializer()), emptyList())
    private val recent = PersistedValue(settings, "recent_searches", ListSerializer(String.serializer()), emptyList())

    val savedPoems: StateFlow<List<SavedPoem>> = saved.flow
    val recentSearches: StateFlow<List<String>> = recent.flow

    fun toggleSaved(poem: Poem) {
        saved.update { list ->
            if (list.any { it.ref == poem.ref }) {
                list.filterNot { it.ref == poem.ref }
            } else {
                listOf(SavedPoem(poem.id, poem.title, poem.poetName, poem.excerpt, poem.source, poem.ref.poetSlug)) + list
            }
        }
    }

    fun remove(ref: PoemRef) = saved.update { list -> list.filterNot { it.ref == ref } }

    fun addRecentSearch(term: String) {
        val clean = term.trim()
        if (clean.length < 2) return
        recent.update { (listOf(clean) + it.filterNot { old -> old == clean }).take(MAX_RECENT) }
    }

    fun clearRecentSearches() = recent.update { emptyList() }

    private companion object {
        const val MAX_RECENT = 12
    }
}
