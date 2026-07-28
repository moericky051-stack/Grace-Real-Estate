package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RealEstateGold
import com.example.ui.theme.RealEstateNavy
import com.example.ui.viewmodel.RealEstateViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                RealEstateApp()
            }
        }
    }
}

@Composable
fun RealEstateApp(
    viewModel: RealEstateViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredProperties by viewModel.filteredProperties.collectAsStateWithLifecycle()
    val favoriteProperties by viewModel.favoriteProperties.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedListingTypeTab.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedPropType by viewModel.selectedPropertyType.collectAsStateWithLifecycle()
    val maxPriceLakhs by viewModel.maxPriceLakhs.collectAsStateWithLifecycle()
    val minBedrooms by viewModel.minBedrooms.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("home", "ရှာဖွေမည်", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("favorites", "သိမ်းဆည်းထားသော", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, badgeCount = favoriteProperties.size),
        BottomNavItem("calculator", "တွက်ချက်စက်", Icons.Filled.Calculate, Icons.Outlined.Calculate),
        BottomNavItem("agents", "အကျိုးဆောင်များ", Icons.Filled.SupportAgent, Icons.Outlined.SupportAgent)
    )

    val showBottomBar = currentRoute in listOf("home", "favorites", "calculator", "agents")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = RealEstateNavy,
                    contentColor = RealEstateGold,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (item.badgeCount > 0) {
                                            Badge(containerColor = RealEstateGold) {
                                                Text("${item.badgeCount}", color = RealEstateNavy, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) RealEstateGold else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RealEstateGold else Color.White.copy(alpha = 0.7f)
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Home Feed
            composable("home") {
                HomeScreen(
                    properties = filteredProperties,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.searchQuery.value = it },
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectedListingTypeTab.value = it },
                    selectedCity = selectedCity,
                    onCitySelected = { viewModel.selectedCity.value = it },
                    selectedPropType = selectedPropType,
                    onPropTypeSelected = { viewModel.selectedPropertyType.value = it },
                    maxPriceLakhs = maxPriceLakhs,
                    onPriceChanged = { viewModel.maxPriceLakhs.value = it },
                    minBedrooms = minBedrooms,
                    onBedroomsSelected = { viewModel.minBedrooms.value = it },
                    onFavoriteToggle = { property ->
                        viewModel.toggleFavorite(property)
                        val statusText = if (property.isFavorite) "အကြိုက်ဆုံးမှ ဖယ်ရှားလိုက်ပါပြီ" else "အကြိုက်ဆုံးစာရင်းသို့ ထည့်သွင်းလိုက်ပါပြီ"
                        scope.launch { snackbarHostState.showSnackbar(statusText) }
                    },
                    onPropertyClick = { propertyId ->
                        navController.navigate("detail/$propertyId")
                    },
                    onPostNewClick = {
                        navController.navigate("post")
                    },
                    onResetFilters = {
                        viewModel.resetFilters()
                    },
                    isSyncing = isSyncing,
                    onRefreshSync = {
                        viewModel.refreshCloudData { success ->
                            scope.launch {
                                val msg = if (success) "အချက်အလက်များကို Cloud database နှင့် ချိတ်ဆက် update လုပ်ပြီးပါပြီ" else "Cloud ချိတ်ဆက်မှု မအောင်မြင်ပါ။ နောက်မှ ပြန်လည် ကြိုးစားပါ။"
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    }
                )
            }

            // 2. Property Detail Screen
            composable(
                route = "detail/{propertyId}",
                arguments = listOf(navArgument("propertyId") { type = NavType.LongType })
            ) { backStackEntry ->
                val propId = backStackEntry.arguments?.getLong("propertyId") ?: 0L
                val property by viewModel.getPropertyById(propId).collectAsStateWithLifecycle()

                PropertyDetailScreen(
                    property = property,
                    onBackClick = { navController.popBackStack() },
                    onFavoriteToggle = { prop ->
                        viewModel.toggleFavorite(prop)
                        val statusText = if (prop.isFavorite) "အကြိုက်ဆုံးမှ ဖယ်ရှားလိုက်ပါပြီ" else "အကြိုက်ဆုံးစာရင်းသို့ ထည့်သွင်းလိုက်ပါပြီ"
                        scope.launch { snackbarHostState.showSnackbar(statusText) }
                    },
                    onCalculateLoanClick = { priceLakhs ->
                        navController.navigate("calculator?price=$priceLakhs")
                    }
                )
            }

            // 3. Post Listing Screen
            composable("post") {
                PostPropertyScreen(
                    onBackClick = { navController.popBackStack() },
                    onSubmitProperty = { title, listingType, propType, priceLakhs, pricePeriod, city, township, address, area, beds, baths, floor, furn, deed, desc, name, phone, imgRes ->
                        viewModel.postNewProperty(
                            title, listingType, propType, priceLakhs, pricePeriod, city, township, address, area, beds, baths, floor, furn, deed, desc, name, phone, imgRes
                        ) {
                            scope.launch {
                                snackbarHostState.showSnackbar("ကြော်ငြာ အောင်မြင်စွာ တင်ပြီးပါပြီ။")
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }

            // 4. Saved / Favorites Screen
            composable("favorites") {
                FavoritesScreen(
                    favoriteProperties = favoriteProperties,
                    onPropertyClick = { propertyId ->
                        navController.navigate("detail/$propertyId")
                    },
                    onFavoriteToggle = { property ->
                        viewModel.toggleFavorite(property)
                        scope.launch { snackbarHostState.showSnackbar("အကြိုက်ဆုံးမှ ဖယ်ရှားလိုက်ပါပြီ") }
                    }
                )
            }

            // 5. Calculator Screen
            composable(
                route = "calculator?price={price}",
                arguments = listOf(navArgument("price") {
                    type = NavType.FloatType
                    defaultValue = 3500f
                })
            ) { backStackEntry ->
                val priceVal = backStackEntry.arguments?.getFloat("price") ?: 3500f
                CalculatorScreen(initialPriceLakhs = priceVal.toDouble())
            }

            // 6. Agents Directory
            composable("agents") {
                AgentDirectoryScreen()
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int = 0
)
