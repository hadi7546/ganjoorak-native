package net.ganjoorak.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.ui.components.AppActions
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.feed.FeedScreen
import net.ganjoorak.app.ui.feed.InterestPickerSheet
import net.ganjoorak.app.ui.menu.AppMenu
import net.ganjoorak.app.ui.menu.FeedPoetsSheet
import net.ganjoorak.app.ui.menu.MenuTarget
import net.ganjoorak.app.ui.player.MiniPlayer
import net.ganjoorak.app.ui.poem.PoemScreen
import net.ganjoorak.app.ui.poets.CategoryScreen
import net.ganjoorak.app.ui.poets.FollowingScreen
import net.ganjoorak.app.ui.poets.PoetScreen
import net.ganjoorak.app.ui.poets.PoetsScreen
import net.ganjoorak.app.ui.profile.ProfileScreen
import net.ganjoorak.app.ui.saved.SavedScreen
import net.ganjoorak.app.ui.search.SearchScreen
import net.ganjoorak.app.ui.settings.SettingsScreen
import net.ganjoorak.app.ui.theme.GanjoorakTheme

@Serializable data object FeedRoute
@Serializable data object PoetsRoute
@Serializable data object SearchRoute
@Serializable data object SavedRoute
@Serializable data object FollowingRoute
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute
@Serializable data class PoemRoute(val id: Int, val source: PoetSource = PoetSource.Ganjoor, val poet: String = "") {
    val ref: PoemRef get() = PoemRef(source, id, poet)
}
@Serializable data class PoetRoute(val key: String)
@Serializable data class CategoryRoute(val id: Int)

private fun PoemRef.route() = PoemRoute(id, source, poetSlug)

private fun NavDestination.menuTarget(): MenuTarget? = when {
    hasRoute<FeedRoute>() -> MenuTarget.Home
    hasRoute<PoetsRoute>() -> MenuTarget.Poets
    hasRoute<SavedRoute>() -> MenuTarget.Saved
    hasRoute<FollowingRoute>() -> MenuTarget.Following
    hasRoute<ProfileRoute>() -> MenuTarget.Profile
    hasRoute<SettingsRoute>() -> MenuTarget.Settings
    else -> null
}

/** Root composable shared by every platform. */
@Composable
fun App() {
    val graph = AppGraph.instance
    val settings by graph.settings.settings.collectAsState()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { AppGraph.instance.imageHttp })) }
            .crossfade(true)
            .build()
    }
    GanjoorakTheme(settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppShell()
        }
    }
}

/**
 * Like the site: no tab bar. Every screen reaches the right-side drawer menu and search from its
 * own controls; the drawer lists the destinations.
 */
@Composable
private fun AppShell() {
    val graph = AppGraph.instance
    val settings by graph.settings.settings.collectAsState()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var pickingInterests by rememberSaveable { mutableStateOf(false) }
    var pickingPoets by rememberSaveable { mutableStateOf(false) }
    // The site opens the interest picker on first run; here once per launch until a choice is saved.
    var offeredInterests by rememberSaveable { mutableStateOf(false) }
    if (!settings.hasChosenInterests && !offeredInterests) {
        offeredInterests = true
        pickingInterests = true
    }

    val goTo: (Any) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo<FeedRoute>()
            launchSingleTop = true
        }
    }
    val actions = remember(nav, drawer) {
        AppActions(
            openMenu = { scope.launch { drawer.open() } },
            openSearch = { nav.navigate(SearchRoute) { launchSingleTop = true } },
        )
    }

    CompositionLocalProvider(LocalAppActions provides actions) {
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                AppMenu(
                    current = entry?.destination?.menuTarget(),
                    onSelect = { target ->
                        scope.launch { drawer.close() }
                        when (target) {
                            MenuTarget.Home -> goTo(FeedRoute)
                            MenuTarget.Poets -> goTo(PoetsRoute)
                            MenuTarget.Saved -> goTo(SavedRoute)
                            MenuTarget.Following -> goTo(FollowingRoute)
                            MenuTarget.Interests -> pickingInterests = true
                            MenuTarget.Profile -> goTo(ProfileRoute)
                            MenuTarget.Settings -> goTo(SettingsRoute)
                        }
                    },
                    onClose = { scope.launch { drawer.close() } },
                )
            },
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AppNavHost(
                        nav,
                        onEditInterests = { pickingInterests = true },
                        onEditFollowing = { pickingPoets = true },
                    )
                }
                MiniPlayer(
                    graph.player,
                    onOpenPoem = { nav.navigate(it.route()) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }
    }

    if (pickingInterests) InterestPickerSheet(onDismiss = { pickingInterests = false })
    if (pickingPoets) FeedPoetsSheet(onDismiss = { pickingPoets = false })
}

@Composable
private fun AppNavHost(nav: NavHostController, onEditInterests: () -> Unit, onEditFollowing: () -> Unit) {
    val graph = AppGraph.instance
    val openPoem: (PoemRef) -> Unit = { nav.navigate(it.route()) }
    val openPoet: (String) -> Unit = { nav.navigate(PoetRoute(it)) }
    val openCategory: (Int) -> Unit = { nav.navigate(CategoryRoute(it)) }
    val back: () -> Unit = {
        if (!nav.popBackStack()) nav.navigate(FeedRoute)
    }
    val home: () -> Unit = { nav.navigate(FeedRoute) { popUpTo<FeedRoute> { inclusive = true } } }

    NavHost(navController = nav, startDestination = FeedRoute) {
        composable<FeedRoute> {
            FeedScreen(
                onOpenPoem = openPoem,
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
                onOpenPoets = { nav.navigate(PoetsRoute) },
                onEditInterests = onEditInterests,
            )
        }
        composable<PoetsRoute> { PoetsScreen(onBack = back, onOpenPoet = openPoet, onOpenFollowing = { nav.navigate(FollowingRoute) }) }
        composable<SearchRoute> { SearchScreen(onBack = back, onOpenPoem = openPoem) }
        composable<SavedRoute> { SavedScreen(onBack = back, onOpenPoem = openPoem) }
        composable<FollowingRoute> {
            FollowingScreen(
                onBack = back,
                onOpenPoet = openPoet,
                onOpenPoets = { nav.navigate(PoetsRoute) },
                onEditFollowing = onEditFollowing,
                onReadFollowing = {
                    graph.settings.setFeedMode(FeedMode.Following)
                    home()
                },
            )
        }
        composable<ProfileRoute> {
            ProfileScreen(
                onBack = back,
                onOpenPoem = openPoem,
                onOpenSaved = { nav.navigate(SavedRoute) },
                onOpenFollowing = { nav.navigate(FollowingRoute) },
                onEditInterests = onEditInterests,
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<SettingsRoute> { SettingsScreen(onBack = back) }
        composable<PoemRoute> { entry ->
            PoemScreen(
                ref = entry.toRoute<PoemRoute>().ref,
                onBack = back,
                // Stepping through next/previous replaces the current poem instead of growing the stack.
                onOpenPoem = { ref -> nav.navigate(ref.route()) { popUpTo<PoemRoute> { inclusive = true } } },
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
            )
        }
        composable<PoetRoute> { entry ->
            PoetScreen(
                poetKey = entry.toRoute<PoetRoute>().key,
                onBack = back,
                onOpenPoem = openPoem,
                onOpenCategory = openCategory,
            )
        }
        composable<CategoryRoute> { entry ->
            CategoryScreen(
                categoryId = entry.toRoute<CategoryRoute>().id,
                onBack = back,
                onOpenPoem = openPoem,
                onOpenPoet = openPoet,
                onOpenCategory = openCategory,
            )
        }
    }
}
