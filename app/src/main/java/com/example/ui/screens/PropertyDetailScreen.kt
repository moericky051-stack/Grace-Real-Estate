package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Property
import com.example.ui.components.PropertyImage
import com.example.ui.components.MapPreviewCard
import com.example.ui.theme.RealEstateBlue
import com.example.ui.theme.RealEstateGold
import com.example.ui.theme.RealEstateGreen
import com.example.ui.theme.RealEstateNavy
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    property: Property?,
    onBackClick: () -> Unit,
    onFavoriteToggle: (Property) -> Unit,
    onCalculateLoanClick: (Double) -> Unit
) {
    if (property == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RealEstateNavy)
        }
        return
    }

    val context = LocalContext.current
    val imageResId = context.resources.getIdentifier(
        property.imageResName, "drawable", context.packageName
    ).let { id -> if (id != 0) id else R.drawable.img_hero_banner }

    var showContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            // Bottom Action Bar with Call & Chat Buttons
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "စျေးနှုန်း", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = if (property.priceLakhs >= 1000) {
                                String.format("%.1f", property.priceLakhs / 1000.0) + " သောင်းကျပ်"
                            } else {
                                "${property.priceLakhs.toInt()} သိန်းကျပ်"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RealEstateNavy
                        )
                    }

                    // Direct Phone Call Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${property.agentPhone}")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RealEstateNavy),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Call", tint = RealEstateGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ဖုန်းခေါ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // SMS / Viber Chat Button
                    OutlinedButton(
                        onClick = { showContactDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = "Message", tint = RealEstateBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("မေးမြန်းမည်", color = RealEstateBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                PropertyImage(
                    imageResName = property.imageResName,
                    contentDescription = property.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                // Top Nav Buttons (Back, Favorite, Share)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${property.title}\nစျေးနှုန်း: ${property.priceLakhs} သိန်း\nတည်နေရာ: ${property.township}, ${property.city}\nဆက်သွယ်ရန်: ${property.agentPhone}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "အိမ်ခြံမြေ မျှဝေမည်"))
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share", tint = Color.White)
                        }

                        IconButton(
                            onClick = { onFavoriteToggle(property) },
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (property.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (property.isFavorite) Color.Red else Color.Gray
                            )
                        }
                    }
                }

                // Listing Tag Badge
                val isRent = property.listingType == "RENT"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isRent) RealEstateBlue else RealEstateGreen,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = if (isRent) "ငှားရန် (Rent)" else "ဝယ်ရန်/ရောင်းရန် (Sale)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Property Body Content
            Column(modifier = Modifier.padding(16.dp)) {
                // Title and Location
                Text(
                    text = property.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = RealEstateNavy
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = RealEstateGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${property.township}၊ ${property.city}၊ မြန်မာ။",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Property Specs Grid (Area, Bedrooms, Bathrooms, Floor, Furnishing, Deed)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "အဓိက အချက်အလက်များ (Property Highlights)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RealEstateNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecDetailItem(icon = Icons.Filled.SquareFoot, label = "အကျယ်အဝန်း", value = "${property.areaSqft} sqft")
                            SpecDetailItem(icon = Icons.Filled.Bed, label = "အိပ်ခန်း", value = "${property.bedrooms} ခန်း")
                            SpecDetailItem(icon = Icons.Filled.Bathtub, label = "ရေချိုးခန်း", value = "${property.bathrooms} ခန်း")
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SpecDetailItem(icon = Icons.Filled.Layers, label = "အလွှာ / အထပ်", value = property.floorLevel)
                            SpecDetailItem(icon = Icons.Filled.Chair, label = "ပရိဘောဂ", value = property.furnishing)
                            SpecDetailItem(icon = Icons.Filled.Assignment, label = "စာရွက်စာတမ်း", value = property.deedType)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                Text(
                    text = "အသေးစိတ် ဖော်ပြချက် (Description)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RealEstateNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = property.description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Map & Neighborhood Card
                MapPreviewCard(
                    township = property.township,
                    city = property.city,
                    address = property.address
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Loan Estimator Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RealEstateNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("အိမ်ခြံမြေ ချေးငွေ တွက်ချက်စက်", color = RealEstateGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("လစဉ် ဆပ်ရမည့် ခန့်မှန်းပမာဏ တွက်ချက်ပါ", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                            IconButton(onClick = { onCalculateLoanClick(property.priceLakhs) }) {
                                Icon(Icons.Filled.Calculate, contentDescription = "Calculate", tint = RealEstateGold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick estimate formula (e.g. 15-year term, 8% interest, 30% downpayment)
                        val principalLakhs = property.priceLakhs * 0.7
                        val monthlyRate = 0.08 / 12.0
                        val totalMonths = 15 * 12
                        val monthlyPaymentLakhs = (principalLakhs * (monthlyRate * (1 + monthlyRate).pow(totalMonths.toDouble()))) /
                                ((1 + monthlyRate).pow(totalMonths.toDouble()) - 1)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ခန့်မှန်း လစဉ်ပေးသွင်းငွေ (၁၅ နှစ်):", color = Color.White, fontSize = 12.sp)
                            Text(
                                text = "ခန့်မှန်း ~${String.format("%.2f", monthlyPaymentLakhs)} သိန်း / လ",
                                color = RealEstateGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { onCalculateLoanClick(property.priceLakhs) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("အသေးစိတ် တွက်ချက်ရန် ကြည့်မည် →", color = RealEstateGold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Agent Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = RealEstateNavy,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = RealEstateGold, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = property.agentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = RealEstateBlue, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${property.agentType} · ${property.agentPhone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Contact Dialog
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("အကျိုးဆောင် ဆက်သွယ်ရန်") },
            text = {
                Column {
                    Text("အမည်: ${property.agentName}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("ဖုန်း: ${property.agentPhone}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Viber, SMS သို့မဟုတ် ဖုန်းဖြင့် တိုက်ရိုက် မေးမြန်းနိုင်ပါသည်။")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:${property.agentPhone}")
                            putExtra("sms_body", "မင်္ဂလာပါ၊ ${property.title} နှင့် ပတ်သက်ပြီး မေးမြန်းလိုပါသည်။")
                        }
                        context.startActivity(intent)
                        showContactDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RealEstateNavy)
                ) {
                    Text("SMS/Viber ပို့မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("ပိတ်မည်")
                }
            }
        )
    }
}

@Composable
private fun SpecDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = RealEstateNavy, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RealEstateNavy)
    }
}
