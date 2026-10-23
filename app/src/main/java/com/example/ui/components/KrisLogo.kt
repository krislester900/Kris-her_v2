package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

/**
 * Logo de l'application kris&her :
 * Lettre 'k' en noir en police Kablamo et le reste en blanc.
 */
@Composable
fun KrisLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    showShadow: Boolean = false,
    borderColor: Color = PureBlack
) {
    Box(modifier = modifier.size(size)) {
        if (showShadow) {
            Box(
                modifier = Modifier
                    .offset(x = 2.5.dp, y = 2.5.dp)
                    .matchParentSize()
                    .clip(shape)
                    .background(PureWhite.copy(alpha = 0.35f))
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(PureWhite)
                .border(2.dp, borderColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "k",
                fontFamily = KablammoFontFamily,
                color = PureBlack,
                fontSize = (size.value * 0.65f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
