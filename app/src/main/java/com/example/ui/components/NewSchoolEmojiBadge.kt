package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KablammoFontFamily
import com.example.ui.theme.NewSchoolCyan
import com.example.ui.theme.NewSchoolLime
import com.example.ui.theme.NewSchoolOrange
import com.example.ui.theme.NewSchoolPink
import com.example.ui.theme.NewSchoolPurple
import com.example.ui.theme.NewSchoolYellow
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite

val NewSchoolBorderColors = listOf(
    NewSchoolPink,
    NewSchoolCyan,
    NewSchoolLime,
    NewSchoolYellow,
    NewSchoolOrange,
    NewSchoolPurple
)

fun getNewSchoolBorderColor(index: Int): Color {
    return NewSchoolBorderColors[index.coerceAtLeast(0) % NewSchoolBorderColors.size]
}

@Composable
fun NewSchoolEmojiBadge(
    emoji: String,
    label: String? = null,
    borderColor: Color = NewSchoolPink,
    isDarkBg: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 12.dp
    val shape = RoundedCornerShape(cornerRadius)
    val bgColor = if (isDarkBg) PureBlack else PureWhite
    val textColor = if (isDarkBg) PureWhite else PureBlack

    Box(modifier = modifier) {
        // Comic hard shadow layer
        Box(
            modifier = Modifier
                .offset(x = 2.5.dp, y = 2.5.dp)
                .matchParentSize()
                .clip(shape)
                .background(if (isDarkBg) PureWhite.copy(alpha = 0.2f) else PureBlack.copy(alpha = 0.3f))
        )

        // Main Sticker Body with New School colored border
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .border(2.5.dp, borderColor, shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = emoji,
                    fontSize = 18.sp
                )
                if (!label.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        fontFamily = KablammoFontFamily,
                        fontSize = 12.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}
