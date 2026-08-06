package com.axelliant.hris.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color,
    accentColor: Color? = null
) {
    BasicCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(SdpR.dimen._4sdp)),
        cardType = CardType.Elevated,
        basicCardTokens = StatCardTokens(radius = R.dimen.ds_radius_md)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            accentColor?.let {
                Box(
                    modifier = Modifier
                        .width(dimensionResource(SdpR.dimen._4sdp))
                        .fillMaxHeight()
                        .background(it)
                )
            }
            Column(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._16sdp),
                    vertical = dimensionResource(SdpR.dimen._14sdp)
                )
            ) {
                Text(
                    text = label,
                    fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                    color = Color(0xFF6E6E73)
                )
                Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
                Text(
                    text = value,
                    fontSize = dimensionResource(SspR.dimen._20ssp).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

// ---------- Grid of 6 stat cards (My Attendance) ----------
@Composable
fun AttendanceStatsGrid(
    absent: String,
    present: String,
    missedPunchOut: String,
    leaves: String,
    holiday: String,
    weeklyOffs: String,
    redColor: Color,
    blueColor: Color,
    neutralColor: Color
) {
    val cardSpacing = dimensionResource(SdpR.dimen._12sdp)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(cardSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
            StatCard(Modifier.weight(1f), "Absents", absent, redColor, redColor)
            StatCard(Modifier.weight(1f), "Present", present, blueColor, blueColor)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
            StatCard(Modifier.weight(1f), "Missed punch-outs", missedPunchOut, neutralColor)
            StatCard(Modifier.weight(1f), "Leaves", leaves, neutralColor)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cardSpacing)) {
            StatCard(Modifier.weight(1f), "Holiday", holiday, neutralColor)
            StatCard(Modifier.weight(1f), "Weekly offs", weeklyOffs, neutralColor)
        }
    }
}

@Composable
fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BasicCard(
        modifier = modifier.fillMaxWidth(),
        cardType = CardType.Elevated,
        basicCardTokens = StatCardTokens(radius = R.dimen.ds_radius_md)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(SdpR.dimen._16sdp)),
            content = content
        )
    }
}

@Composable
fun DetailLabelValue(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    dotColor: Color? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            color = Color(0xFF8A8A8E),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            dotColor?.let {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(SdpR.dimen._8sdp))
                        .background(it, CircleShape)
                )
                Spacer(modifier = Modifier.width(dimensionResource(SdpR.dimen._6sdp)))
            }
            Text(
                text = value,
                fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}
