package com.axelliant.hris.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.R
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import com.microsoft.fluentui.theme.token.controlTokens.ButtonSize
import com.microsoft.fluentui.theme.token.controlTokens.ButtonStyle
import com.microsoft.fluentui.theme.token.controlTokens.CardType
import com.microsoft.fluentui.tokenized.controls.BasicCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.unit.dp


fun expenseStatusStyle(rawStatus: String): Triple<Color, Color, String> = when {
    rawStatus.contains("draft", true) ->
        Triple(LeaveBg, LeaveAmber, "Pending")
    rawStatus.contains("approved", true) -> Triple(PresentBg, PresentGreen, rawStatus)
    rawStatus.contains("pending", true) -> Triple(LeaveBg, LeaveAmber, rawStatus)
    rawStatus.contains("reject", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    rawStatus.contains("cancel", true) -> Triple(AbsentBg, AbsentRed, rawStatus)
    else -> Triple(BorderGrey, TextGrey, rawStatus.ifBlank { "—" })
}

@Composable
fun ExpenseStatusChip(rawStatus: String) {
    val (bg, fg, label) = expenseStatusStyle(rawStatus)
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
fun ExpenseRow(item: Expense, onClick: () -> Unit) {
    var expanded by remember(item.name) { mutableStateOf(false) }
    val label = item.approval_status?.ifBlank { item.status.orEmpty() } ?: item.status.orEmpty()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .noRippleClickable { expanded = !expanded }
                .padding(
                    vertical = dimensionResource(SdpR.dimen._10sdp),
                    horizontal = dimensionResource(SdpR.dimen._4sdp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1.1f)) {
                Text(
                    item.posting_date.orEmpty(),
                    fontSize = dimensionResource(SspR.dimen._13ssp).value.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!item.name.isNullOrBlank()) {
                    Text(
                        item.name.orEmpty(),
                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                        color = TextGrey
                    )
                }
            }
            Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.Center) {
                Text(
                    item.grand_total?.let { "%d".format(it) } ?: "—",
                    fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
                )
            }
            Row(
                Modifier.weight(1.1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpenseStatusChip(label)
                Spacer(Modifier.width(dimensionResource(SdpR.dimen._4sdp)))
                Icon(
                    imageVector = if (expanded)
                        Icons.Filled.KeyboardArrowUp
                    else
                        Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextGrey
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            ScreenBg,
                            RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
                        )
                        .padding(dimensionResource(SdpR.dimen._10sdp))
                ) {

                    item.expenses_detail?.forEachIndexed { index, detail ->

                        if (index > 0) {
                            Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))
                        }

                        DetailLine("Type", detail.expense_type ?: "—")
                        DetailLine("Date", detail.expense_date ?: "—")
                        DetailLine("Amount", detail.amount?.toString() ?: "—")
                        DetailLine("Description", detail.description.ifBlank { "—" })
                    }

                    Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

                    AppButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = onClick,
                        text = "Edit",
                        style = ButtonStyle.Button,
                        size = ButtonSize.Small,
                        buttonTokens = CheckInButtonTokens(
                            fontSizeDimenName = "_11ssp",
                            bgRest = 0xFFFF0000,
                            bgPressed = 0xFFCC0000,
                            bgSelected = 0xFFCC0000
                        )
                    )
                }

                Spacer(Modifier.height(dimensionResource(SdpR.dimen._6sdp)))
            }
        }    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._2sdp)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
        Text(
            value,
            fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ExpenseFilterChipItem(filter: FilterModel, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FluentBlue else CardWhite
    val fg = if (selected) Color.White else Color.Black
    BasicCard(
        cardType = if (selected) CardType.Elevated else CardType.Outlined,
        modifier = Modifier.noRippleClickable(onClick)
    ) {
        Row(
            Modifier
                .background(bg, RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
                .padding(
                    horizontal = dimensionResource(SdpR.dimen._10sdp),
                    vertical = dimensionResource(SdpR.dimen._7sdp)
                ),
        ) {
            if (selected) {
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
fun ExpenseListCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BasicCard(modifier = modifier, cardType = CardType.Elevated) {
        Column(Modifier.padding(horizontal = dimensionResource(SdpR.dimen._6sdp))) { content() }
    }
}

@Composable
fun DateRangeBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(modifier) {
        Text(label, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
        Box(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm)))
                .border(
                    width = 1.dp,
                    color = BorderGrey,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.ds_radius_sm))
                )
                .noRippleClickable(onClick)
                .padding(dimensionResource(SdpR.dimen._10sdp))
        ) {
            Text(value, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp)
        }
    }
}
@Composable
fun ExpenseEmptyState(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(SdpR.dimen._40sdp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = TextGrey,
            fontSize = dimensionResource(SspR.dimen._13ssp).value.sp
        )
    }
}
