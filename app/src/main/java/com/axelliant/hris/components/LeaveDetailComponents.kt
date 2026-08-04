package com.axelliant.hris.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.R
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.leave.LeaveDetail
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.ButtonSize
import com.microsoft.fluentui.theme.token.controlTokens.ButtonStyle
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard

// ---------- Status chip (Open / Approved / Rejected / Cancelled) ----------

fun leaveStatusStyle(rawStatus: String): Triple<Color, Color, String> = when {
    rawStatus.contains("open", true) -> Triple(LeaveBg, LeaveAmber, rawStatus)
    rawStatus.contains("approved", true) -> Triple(PresentBg, PresentGreen, rawStatus)
    rawStatus.contains("reject", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    rawStatus.contains("cancel", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    else -> Triple(BorderGrey, TextGrey, rawStatus.ifBlank { "—" })
}

@Composable
fun LeaveStatusChip(rawStatus: String) {
    val (bg, fg, label) = leaveStatusStyle(rawStatus)
    Row(
        Modifier
            .background(bg, RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
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

// ---------- One leave row (From | To | Type | Status) ----------

@Composable
fun LeaveDetailRow(item: LeaveDetail, onEditClick: (LeaveDetail) -> Unit) {
    var expanded by remember(item.name) { mutableStateOf(false) }

    val status = item.status
    val leaveType = item.leave_type.ifBlank { "—" }
    val fromDate = item.from_date.ifBlank { "—" }
    val toDate = item.to_date.ifBlank { "—" }
    val reason = (item.leave_reason ?: item.description).orEmpty()
    val paidLabel = if (item.is_paid) "Paid" else "Unpaid"
    val paidColor = if (item.is_paid) PresentGreen else AbsentRed
    val leaveDaysLabel = if (item.total_leave_days == 1.0) {
        "1 day"
    } else {
        "%.1f days".format(item.total_leave_days)
    }

    BasicCard(
        cardType = CardType.Elevated,
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .noRippleClickable { expanded = !expanded }
                    .padding(
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        fromDate,
                        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        toDate,
                        fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    Text(
                        leaveType,
                        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                        color = TextGrey
                    )
                }
                Icon(
                    imageVector = if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGrey
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(SdpR.dimen._5sdp))
                ) {
                    LeaveStatusChip(status)

                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))

                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onEditClick(item) },
                        text = "Edit",
                        style = ButtonStyle.Button,
                        size = ButtonSize.Small,
                        buttonTokens = CheckInButtonTokens(
                            fontSizeDimenName = "_11ssp",
                            bgRest = 0xFF09589B,
                            bgPressed = 0xFF06406F,
                            bgSelected = 0xFF06406F
                        )
                    )

                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(dimensionResource(SdpR.dimen._1sdp))
                            .background(BorderGrey)
                    )
                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._15sdp)))

                    Row(Modifier.fillMaxWidth()) {
                        LeaveDetailField(
                            label = "LEAVE DAYS",
                            value = leaveDaysLabel,
                            modifier = Modifier.weight(1f)
                        )
                        LeaveDetailField(
                            label = "STATUS",
                            value = paidLabel,
                            valueColor = paidColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (reason.isNotBlank()) {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))
                        Text(
                            "REASON",
                            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                            color = TextGrey
                        )
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
                        Text(
                            reason,
                            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveDetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Black
) {
    Column(modifier.padding(dimensionResource(SdpR.dimen._5sdp))) {
        Text(
            label,
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            color = TextGrey
        )
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._5sdp)))
        Text(
            value,
            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- Full screen content ----------

@Composable
fun LeaveDetailContent(
    filters: List<FilterModel>,
    selectedFilterId: String,
    leaveList: List<LeaveDetail>,
    onFilterClick: (FilterModel) -> Unit,
    onEditClick: (LeaveDetail) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = dimensionResource(SdpR.dimen._10sdp))
    ) {
        AttendanceFilterRow(
            filters = filters,
            selectedId = selectedFilterId,
            onFilterClick = onFilterClick
        )

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._10sdp)))

        if (leaveList.isEmpty()) {
            ExpenseEmptyState("No record found")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
            ) {
                items(leaveList) { item ->
                    LeaveDetailRow(item = item, onEditClick = onEditClick)
                }
            }
        }
    }
}