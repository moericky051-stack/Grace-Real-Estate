package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
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

    val imageModel = remember(imageResName) {
        val path = imageResName
            .split(",")
            .firstOrNull()
            ?.trim()
            ?: ""

        when {
            path.startsWith("/") -> File(path)

            path.startsWith("http://") ||
            path.startsWith("https://") ||
            path.startsWith("content://") ||
            path.startsWith("file://") -> path

            else -> {
                val resId = context.resources.getIdentifier(
                    path.substringBeforeLast("."),
                    "drawable",
                    context.packageName
                )

                if (resId != 0) resId
                else R.drawable.img_hero_banner
            }
        }
    }

    val request = remember(imageModel) {
        ImageRequest.Builder(context)
            .data(imageModel)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        error = androidx.compose.ui.res.painterResource(R.drawable.img_hero_banner),
        placeholder = androidx.compose.ui.res.painterResource(R.drawable.img_hero_banner)
    )
}