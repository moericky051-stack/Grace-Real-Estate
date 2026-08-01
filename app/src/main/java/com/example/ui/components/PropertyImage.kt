package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import java.io.File

@Composable
fun PropertyImage(
    imageResName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    val imageModel: Any = remember(imageResName) {
        // 1. ကော်မာပါရင် ပထမဆုံး လမ်းကြောင်းကို ယူမည်
        val primaryPath = if (imageResName.contains(",")) {
            imageResName.split(",").firstOrNull { it.isNotBlank() }?.trim() ?: imageResName
        } else {
            imageResName
        }

        // 2. Type အလိုက် Model ခွဲခြားမည်
        when {
            primaryPath.startsWith("/") -> File(primaryPath)
            primaryPath.startsWith("file://") ||
            primaryPath.startsWith("content://") ||
            primaryPath.startsWith("http") -> primaryPath
            else -> {
                // Extension များ ပါလာပါက ဖျက်ထုတ်မည် (ဥပမာ .jpg, .png)
                val cleanName = primaryPath.substringBeforeLast(".")
                val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
                if (resId != 0) resId else R.drawable.img_hero_banner
            }
        }
    }

    AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        error = painterResource(id = R.drawable.img_hero_banner),
        placeholder = painterResource(id = R.drawable.img_hero_banner)
    )
}
