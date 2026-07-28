package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Property
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.PropertyCard
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
    onRefreshSync: () -> Unit = {}
) {
    var isFilterSheetOpen by remember { mutableStateOf(false) }

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
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = null,
                                        tint = RealEstateNavy,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Grace အိမ် ခြံ မြေ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "ဝယ် · ရောင်း · ငှား စုံလင်စွာ ရှာဖွေပါ",
                                    fontSize = 11.sp,
                                    color = RealEstateGold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Sync Refresh Button
                            IconButton(
                                onClick = onRefreshSync,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    .size(40.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = RealEstateGold,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudSync,
                                        contentDescription = "Sync Cloud",
                                        tint = RealEstateGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Filter Button with active indicator
                            BadgedBox(
                                badge = {
                                    if (selectedCity != "ALL" || selectedPropType != "ALL" || maxPriceLakhs < 20000f || minBedrooms > 0) {
                                        Badge(containerColor = RealEstateGold) {
                                            Text("!", color = RealEstateNavy, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { isFilterSheetOpen = true },
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FilterList,
                                        contentDescription = "Filter",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("မြို့နယ်၊ ခေါင်းစဉ် သို့မဟုတ် လိပ်စာ ရှာပါ...", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = RealEstateGold) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.White)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.12f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedBorderColor = RealEstateGold,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                            text = { Text("အားလုံး (All)", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == "BUY",
                            onClick = { onTabSelected("BUY") },
                            text = { Text("ဝယ်ရန်/ရောင်းရန်", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == "RENT",
                            onClick = { onTabSelected("RENT") },
                            text = { Text("ငှားရန် (Rent)", fontWeight = FontWeight.Bold) }
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cities) { (key, label) ->
                        val isSelected = selectedCity == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCitySelected(key) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (key != "ALL") {
                                { Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RealEstateNavy,
                                selectedLabelColor = RealEstateGold,
                                selectedLeadingIconColor = RealEstateGold
                            )
                        )
                    }
                }
            }

            // Main Content Area (Featured Banner & Property Feed)
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Hero Banner
                item {
                    FeaturedHeroBanner(
                        onExploreClick = { onTabSelected("BUY") }
                    )
                }

                // Header title & count
                item {
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
                    item {
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
}

@Composable
private fun FeaturedHeroBanner(
    onExploreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                RealEstateNavy.copy(alpha = 0.92f),
                                RealEstateNavy.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RealEstateGold,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "အထူး အစီအစဉ်",
                        color = RealEstateNavy,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "စိတ်ကြိုက် အိမ်ခြံမြေများကို\nတိုက်ရိုက် ဆက်သွယ်ပါ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExploreClick,
                    colors = ButtonDefaults.buttonColors(containerColor = RealEstateGold),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ကြည့်ရှုမည်", color = RealEstateNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
