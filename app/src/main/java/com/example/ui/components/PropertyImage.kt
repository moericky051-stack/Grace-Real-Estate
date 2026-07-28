package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R

@Composable
fun PropertyImage(
    imageResName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val imageModel: Any = remember(imageResName) {
        when {
            imageResName.startsWith("/") -> java.io.File(imageResName)
            imageResName.startsWith("file://") ||
            imageResName.startsWith("content://") ||
            imageResName.startsWith("http") -> imageResName
            else -> {
                val resId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
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
