package com.axelliant.hris.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.R
import com.axelliant.hris.components.StatCardTokens
import com.microsoft.fluentui.theme.token.controlTokens.ButtonTokens
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard

// ---------- Reusable accent stat card (Total/Pending/Approved/Rejected/Remaining) ----------
@Composable
fun LeaveStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
) {
    BasicCard(
        modifier = modifier.fillMaxWidth(),
        cardType = CardType.Elevated,
        basicCardTokens = LeaveCardTokens(radius = R.dimen.ds_radius_md),

        ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(dimensionResource(SdpR.dimen._4sdp))
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(
                    horizontal = dimensionResource(SdpR.dimen._12sdp),
                    vertical = dimensionResource(SdpR.dimen._10sdp)
                )
            ) {
                Text(
                    text = value,
                    fontSize = dimensionResource(SspR.dimen._20ssp).value.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
                Text(
                    text = label,
                    fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                    color = Color(0xFF6E6E73)
                )
            }
        }
    }
}

// ---------- Self leave stats: Total (full width) + Pending/Approved/Rejected row + Remaining ----------
@Composable
fun SelfLeaveStatsGrid(
    total: String,
    pending: String,
    approved: String,
    rejected: String,
    remaining: String,
    appColor: Color,
    yellowColor: Color,
    greenColor: Color,
    redColor: Color
) {
    val spacing = dimensionResource(SdpR.dimen._10sdp)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        LeaveStatCard(Modifier.fillMaxWidth(), "Total Leaves", total, appColor)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            LeaveStatCard(Modifier.weight(1f), "Pending", pending, yellowColor)
            LeaveStatCard(Modifier.weight(1f), "Approved", approved, greenColor)
            LeaveStatCard(Modifier.weight(1f), "Rejected", rejected, redColor)
        }

        LeaveStatCard(Modifier.fillMaxWidth(), "Remaining", remaining, appColor)
    }
}

// ---------- Team stats grid (Total members / Present / Approved / Rejected / Pending) ----------
@Composable
fun TeamLeaveStatsGrid(
    totalMembers: String,
    present: String,
    approved: String,
    rejected: String,
    pending: String,
    blueColor: Color,
    appColor: Color,
    greenColor: Color,
    redColor: Color,
    yellowColor: Color
) {
    val spacing = dimensionResource(SdpR.dimen._10sdp)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
        LeaveStatCard(Modifier.fillMaxWidth(), "Total Members", totalMembers, blueColor)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            LeaveStatCard(Modifier.weight(1f), "Present", present, appColor)
            LeaveStatCard(Modifier.weight(1f), "Approved", approved, greenColor)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
            LeaveStatCard(Modifier.weight(1f), "Rejected", rejected, redColor)
            LeaveStatCard(Modifier.weight(1f), "Pending", pending, yellowColor)
        }
    }
}
