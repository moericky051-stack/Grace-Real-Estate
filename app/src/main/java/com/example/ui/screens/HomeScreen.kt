package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Property
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.PropertyCard
import com.example.ui.theme.AppThemeOption
import com.example.ui.theme.RealEstateBlue
import com.example.ui.theme.RealEstateGold
import com.example.ui.theme.RealEstateGreen
import com.example.ui.theme.RealEstateNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    properties: List<Property>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    selectedPropType: String,
    onPropTypeSelected: (String) -> Unit,
    maxPriceLakhs: Float,
    onPriceChanged: (Float) -> Unit,
    minBedrooms: Int,
    onBedroomsSelected: (Int) -> Unit,
    onFavoriteToggle: (Property) -> Unit,
    onPropertyClick: (Long) -> Unit,
    onPostNewClick: () -> Unit,
    onResetFilters: () -> Unit,
    isSyncing: Boolean = false,
    onRefreshSync: () -> Unit = {},
    selectedTheme: AppThemeOption = AppThemeOption.NAVY_GOLD,
    onThemeSelected: (AppThemeOption) -> Unit = {}
) {
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    var isThemeDialogVisible by remember { mutableStateOf(false) }

    val cities = listOf(
        "ALL" to "မြို့အားလုံး",
        "Yangon" to "ရန်ကုန်",
        "Mandalay" to "မန္တလေး",
        "Naypyidaw" to "နေပြည်တော်",
        "Pyin Oo Lwin" to "ပြင်ဦးလွင်",
        "Taunggyi" to "တောင်ကြီး"
    )

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPostNewClick,
                containerColor = RealEstateNavy,
                contentColor = RealEstateGold,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Filled.AddHome, contentDescription = "Post Property")
                Spacer(modifier = Modifier.width(8.dp))
                Text("အိမ်ခြံမြေ တင်မည်", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with App Branding & Top Bar
            Surface(
                color = RealEstateNavy,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RealEstateGold,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = null,
                                        tint = RealEstateNavy,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Grace အိမ် ခြံ မြေ",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "ဝယ် · ရောင်း · ငှား ရှာဖွေပါ",
                                    fontSize = 10.sp,
                                    color = RealEstateGold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Theme Change Palette Button
                            IconButton(
                                onClick = { isThemeDialogVisible = true },
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = "Change Theme",
                                    tint = RealEstateGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Sync Refresh Button
                            IconButton(
                                onClick = onRefreshSync,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .size(32.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = RealEstateGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudSync,
                                        contentDescription = "Sync Cloud",
                                        tint = RealEstateGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Filter Button with active indicator
                            BadgedBox(
                                badge = {
                                    if (selectedCity != "ALL" || selectedPropType != "ALL" || maxPriceLakhs < 20000f || minBedrooms > 0) {
                                        Badge(containerColor = RealEstateGold) {
                                            Text("!", color = RealEstateNavy, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { isFilterSheetOpen = true },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Filter",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Listing Type Selector Tabs (အားလုံး | ဝယ်ရန် | ငှားရန်)
                    TabRow(
                        selectedTabIndex = when (selectedTab) {
                            "BUY" -> 1
                            "RENT" -> 2
                            else -> 0
                        },
                        containerColor = Color.Transparent,
                        contentColor = RealEstateGold,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == "ALL",
                            onClick = { onTabSelected("ALL") },
                            text = { Text("အားလုံး", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(34.dp)
                        )
                        Tab(
                            selected = selectedTab == "BUY",
                            onClick = { onTabSelected("BUY") },
                            text = { Text("ဝယ်/ရောင်း", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(34.dp)
                        )
                        Tab(
                            selected = selectedTab == "RENT",
                            onClick = { onTabSelected("RENT") },
                            text = { Text("ငှားရန်", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(34.dp)
                        )
                    }
                }
            }

            // Quick City Chips Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(cities) { (key, label) ->
                        val isSelected = selectedCity == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCitySelected(key) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (key != "ALL") {
                                { Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RealEstateNavy,
                                selectedLabelColor = RealEstateGold,
                                selectedLeadingIconColor = RealEstateGold
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Main Content Area (Property Feed in 2 Columns)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header title & count (Spans both 2 columns)
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "အိမ်ခြံမြေ စာရင်းများ (${properties.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RealEstateNavy
                        )
                        if (selectedCity != "ALL" || selectedPropType != "ALL" || maxPriceLakhs < 20000f) {
                            TextButton(onClick = onResetFilters) {
                                Text("စစ်ထုတ်မှု ဖျက်မည်", fontSize = 12.sp, color = RealEstateBlue)
                            }
                        }
                    }
                }

                // Empty state if no properties found
                if (properties.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ရှာဖွေမှုဖြင့် ကိုက်ညီသော အိမ်ခြံမြေ မရှိပါ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ကျေးဇူးပြု၍ စစ်ထုတ်မှု သို့မဟုတ် ရှာဖွေစကားလုံး ပြောင်းလဲကြည့်ပါ",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = onResetFilters) {
                                    Text("စစ်ထုတ်မှုများ ပြန်လည်စတင်မည်")
                                }
                            }
                        }
                    }
                } else {
                    items(properties, key = { it.id }) { property ->
                        PropertyCard(
                            property = property,
                            onCardClick = { onPropertyClick(property.id) },
                            onFavoriteClick = { onFavoriteToggle(property) }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet Dialog
    if (isFilterSheetOpen) {
        FilterBottomSheet(
            selectedCity = selectedCity,
            onCitySelected = onCitySelected,
            selectedPropType = selectedPropType,
            onPropTypeSelected = onPropTypeSelected,
            maxPriceLakhs = maxPriceLakhs,
            onPriceChanged = onPriceChanged,
            minBedrooms = minBedrooms,
            onBedroomsSelected = onBedroomsSelected,
            onReset = onResetFilters,
            onDismiss = { isFilterSheetOpen = false }
        )
    }

    // Theme Switcher Dialog
    if (isThemeDialogVisible) {
        ThemeSelectionDialog(
            currentTheme = selectedTheme,
            onThemeSelect = { theme ->
                onThemeSelected(theme)
                isThemeDialogVisible = false
            },
            onDismiss = { isThemeDialogVisible = false }
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: AppThemeOption,
    onThemeSelect: (AppThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Palette,
                contentDescription = "Theme",
                tint = RealEstateGold,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Theme အရောင် ရွေးချယ်ပါ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                AppThemeOption.entries.forEach { option ->
                    val isSelected = option == currentTheme
                    Surface(
                        onClick = { onThemeSelect(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) option.headerColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) option.accentColor else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Color sample badge
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(option.headerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(option.accentColor)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = option.titleMm,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = option.titleEn,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = option.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည် (Close)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun FeaturedHeroBanner(
    onExploreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Featured Real Estate",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                RealEstateNavy.copy(alpha = 0.95f),
                                RealEstateNavy.copy(alpha = 0.75f),
                                RealEstateNavy.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RealEstateGold
                    ) {
                        Text(
                            text = "အထူးအစီအစဉ်",
                            color = RealEstateNavy,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "စိတ်ကြိုက် အိမ်ခြံမြေများကို တိုက်ရိုက် ဆက်သွယ်ပါ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onExploreClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RealEstateGold),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("ကြည့်ရှုမည်", color = RealEstateNavy, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
