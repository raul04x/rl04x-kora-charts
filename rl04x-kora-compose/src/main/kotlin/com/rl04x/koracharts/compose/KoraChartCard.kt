package com.rl04x.koracharts.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rl04x.koracharts.core.model.KoraChartStyle

/**
 * Container card for any Kora Chart with customizable height, header, subtitle, and badges.
 *
 * @param title Card title text
 * @param modifier Outer card modifier
 * @param subtitle Optional subtitle text
 * @param badgeText Optional pill badge text
 * @param chartHeight Optional height for the chart container box
 * @param style KoraChartStyle color theme
 * @param content Chart content composable
 */
@Composable
public fun KoraChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    chartHeight: Dp? = null,
    style: KoraChartStyle = KoraChartStyle.dark(),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(style.cardBackgroundColor),
        ),
        border = BorderStroke(1.dp, Color(style.cardBorderColor)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color(style.titleTextColor),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            color = Color(style.subtitleTextColor),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(style.badgeBackgroundColor),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = badgeText,
                            color = Color(style.badgeTextColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val contentModifier = if (chartHeight != null) {
                Modifier.fillMaxWidth().height(chartHeight)
            } else {
                Modifier.fillMaxWidth()
            }

            Box(modifier = contentModifier) {
                content()
            }
        }
    }
}
