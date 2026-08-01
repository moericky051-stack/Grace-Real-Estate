package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aistudio.realestate.shwehouse.edeaff.R
import java.io.File

@Composable
fun PropertyImage(
    imageResName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    // 1. Path/Name ခွဲထုတ်ခြင်း (getIdentifier အကြိမ်ကြိမ် မခေါ်အောင် remember သုံးထားသည်)
    val imageModel: Any = remember(imageResName) {
        val primaryPath = if (imageResName.contains(",")) {
            imageResName.split(",").firstOrNull { it.isNotBlank() }?.trim() ?: imageResName
        } else {
            imageResName
        }

        when {
            primaryPath.startsWith("/") -> File(primaryPath)
            primaryPath.startsWith("file://") ||
            primaryPath.startsWith("content://") ||
            primaryPath.startsWith("http") -> primaryPath
            else -> {
                val cleanName = primaryPath.substringBeforeLast(".")
                val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
                if (resId != 0) resId else R.drawable.img_hero_banner
            }
        }
    }

    // 2. Coil Request တွင် Cache ဖွင့်လှစ်ခြင်း
    val imageRequest = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    // 3. SubcomposeAsyncImage ဖြင့် Loading မလှုပ်ဘဲ Smooth ဖြစ်အောင် ပြသပေးခြင်း
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    )
}
