package com.axelliant.hris.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.model.attendance.AttendanceDetail
import com.axelliant.hris.model.dashboard.FilterModel
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard

val FluentBlue = Color(0xFF106EBE)
val PresentGreen = Color(0xFF1D8A4E)
val PresentBg = Color(0xFFDDF3E4)
val WeekendPurple = Color(0xFF6A3FA0)
val WeekendBg = Color(0xFFE9E1F7)
val AbsentRed = Color(0xFFC0392B)
val AbsentBg = Color(0xFFFBE2E2)
val LeaveAmber = Color(0xFFB8860B)
val LeaveBg = Color(0xFFFFF3D6)
val CardWhite = Color.White
val BorderGrey = Color(0xFFE5E5EA)
val TextGrey = Color(0xFF6E6E73)
val ScreenBg = Color(0xFFF5F5F7)

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

@Composable
fun StatCard(value: String, valueColor: Color, label: String, modifier: Modifier = Modifier) {
    BasicCard(
        modifier = modifier,
        cardType = CardType.Elevated
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(SdpR.dimen._14sdp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = dimensionResource(SspR.dimen._20ssp).value.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
            Text(
                label,
                fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                color = TextGrey
            )
        }
    }
}

fun statusStyle(rawStatus: String): Triple<Color, Color, String> = when {
    rawStatus.contains("present", true) -> Triple(PresentBg, PresentGreen, rawStatus)
    rawStatus.contains("week", true) -> Triple(WeekendBg, WeekendPurple, rawStatus)
    rawStatus.contains("absent", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    rawStatus.contains("leave", true) -> Triple(LeaveBg, LeaveAmber, rawStatus)
    rawStatus.contains("home", true) -> Triple(PresentBg, PresentGreen, rawStatus)
    else -> Triple(BorderGrey, TextGrey, rawStatus.ifBlank { "—" })
}

@Composable
fun StatusChip(rawStatus: String) {
    val (bg, fg, label) = statusStyle(rawStatus)
    Row(
        Modifier
            .background(bg, RoundedCornerShape(dimensionResource(SdpR.dimen._14sdp)))
            .padding(
                horizontal = dimensionResource(SdpR.dimen._8sdp),
                vertical = dimensionResource(SdpR.dimen._4sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(dimensionResource(SdpR.dimen._5sdp))
                .background(fg, RoundedCornerShape(dimensionResource(SdpR.dimen._3sdp)))
        )
        Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
        Text(
            label,
            color = fg,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AttendanceRow(item: AttendanceDetail, onClick: () -> Unit) {
    val label = item.display_status.ifBlank { item.status }
    Row(
        Modifier
            .fillMaxWidth()
            .noRippleClickable(onClick)
            .padding(
                vertical = dimensionResource(SdpR.dimen._10sdp),
                horizontal = dimensionResource(SdpR.dimen._4sdp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1.1f)) {
            Text(
                item.date,
                fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (item.attendance_location.isNotBlank()) {
                Text(
                    item.attendance_location,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    color = TextGrey
                )
            }
        }
        Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.Center) {
            if (item.working_hours > 0) {
                Text(
                    "%.2f".format(item.working_hours),
                    fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._2sdp)))
                Text("hrs", fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
            } else {
                Text("—", fontSize = dimensionResource(SspR.dimen._13ssp).value.sp, color = TextGrey)
            }
        }
        Row(Modifier.weight(1.1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            StatusChip(label)
            Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
            Text(">", color = TextGrey)
        }
    }
}

@Composable
fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .noRippleClickable(onClick)
            .padding(vertical = dimensionResource(SdpR.dimen._8sdp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) FluentBlue else Color.Black
        )
        if (selected) {
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
            Box(
                Modifier
                    .height(dimensionResource(SdpR.dimen._2sdp))
                    .width(dimensionResource(SdpR.dimen._40sdp))
                    .background(FluentBlue)
            )
        }
    }
}

@Composable
fun AttendanceStatusChipItem(filter: FilterModel, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FluentBlue else CardWhite
    val fg = if (selected) Color.White else Color.Black
    BasicCard(
        cardType = if (selected) CardType.Elevated else CardType.Outlined,
        modifier = Modifier.noRippleClickable(onClick)
    ) {
        Row(
            Modifier
                .background(bg)
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._10sdp),
                    vertical = dimensionResource(SdpR.dimen._7sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
            } else if (filter.title?.contains("home", true) == true) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
            } else if (filter.title?.contains("office", true) == true) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(dimensionResource(SdpR.dimen._12sdp))
                )
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
            }
            Text(
                filter.title ?: "",
                color = fg,
                fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                fontWeight = FontWeight.Medium
            )
            if (!filter.count.isNullOrBlank()) {
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
                Box(
                    Modifier
                        .background(
                            if (selected) Color.White.copy(alpha = 0.25f) else ScreenBg,
                            RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
                        )
                        .padding(
                            horizontal = dimensionResource(SdpR.dimen._5sdp),
                            vertical = dimensionResource(SdpR.dimen._2sdp)
                        )
                ) {
                    Text(
                        filter.count ?: "0",
                        color = fg,
                        fontSize = dimensionResource(SspR.dimen._10ssp).value.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceListCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BasicCard(modifier = modifier, cardType = CardType.Elevated) {
        Column(Modifier.padding(horizontal = dimensionResource(SdpR.dimen._6sdp))) { content() }
    }
}
