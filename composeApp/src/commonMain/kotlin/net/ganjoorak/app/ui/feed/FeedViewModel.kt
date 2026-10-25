package net.ganjoorak.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRepository
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.data.local.SettingsStore

/** Everything that decides which poems the feed shows. */
data class FeedSource(
    val mode: FeedMode = FeedMode.All,
    val followedKeys: List<String> = emptyList(),
)

data class FeedState(
    val poems: List<Poem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val source: FeedSource = FeedSource(),
    /** Bumped whenever the feed restarts so the pager can jump back to the top. */
    val generation: Int = 0,
) {
    /** «دنبال‌شده‌ها» is selected but no poet is followed yet. */
    val isFollowingEmpty: Boolean get() = source.mode == FeedMode.Following && source.followedKeys.isEmpty()
}

/**
 * Endless feed of poems, loaded a few pages ahead, like the website's home: «همه» draws random
 * poems from all poets, «دنبال‌شده‌ها» only from followed poets.
 */
class FeedViewModel(
    private val repository: PoemRepository,
    settings: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedState())
    val state: StateFlow<FeedState> = _state.asStateFlow()
    private var job: Job? = null
    private var visibleIndex = 0

    init {
        viewModelScope.launch {
            settings.settings
                .map { FeedSource(it.feedMode, it.followedPoets.map { poet -> poet.key }) }
                .distinctUntilChanged()
                .collect { restart(it) }
        }
    }

    fun onPageVisible(index: Int) {
        visibleIndex = index
        ensureLoaded()
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        ensureLoaded()
    }

    fun refresh() = restart(_state.value.source, keepVisible = false)

    /**
     * Drops the queued poems of the old source. The poem on screen stays (as on the site), so the
     * next swipe already lands on a poem from the new source.
     */
    private fun restart(source: FeedSource, keepVisible: Boolean = true) {
        job?.cancel()
        val current = _state.value
        val visible = current.poems.getOrNull(visibleIndex).takeIf { keepVisible }
        visibleIndex = 0
        _state.value = FeedState(poems = listOfNotNull(visible), source = source, generation = current.generation + 1)
        ensureLoaded()
    }

    private fun ensureLoaded() {
        val current = _state.value
        if (job?.isActive == true || current.error != null || current.isFollowingEmpty) return
        if (current.poems.size > visibleIndex + PREFETCH) return
        job = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            var duplicates = 0
            try {
                while (_state.value.poems.size <= visibleIndex + PREFETCH && duplicates < MAX_DUPLICATES) {
                    val poem = nextPoem(_state.value.source)
                    if (_state.value.poems.any { it.ref == poem.ref }) {
                        duplicates++
                    } else {
                        _state.update { it.copy(poems = it.poems + poem) }
                    }
                }
                _state.update { it.copy(loading = false) }
            } catch (e: GanjoorException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private suspend fun nextPoem(source: FeedSource): Poem = when (source.mode) {
        FeedMode.Following -> repository.randomPoem(source.followedKeys.randomOrNull())
        FeedMode.All -> repository.randomPoem(null)
    }

    private companion object {
        const val PREFETCH = 2
        const val MAX_DUPLICATES = 6
    }
}
