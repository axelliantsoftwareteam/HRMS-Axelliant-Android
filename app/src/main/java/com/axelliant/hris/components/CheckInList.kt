package com.axelliant.hris.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axelliant.hris.model.checkin.CheckInDetail
import com.axelliant.hris.model.dashboard.FilterModel
import com.intuit.sdp.R as SdpR
import com.intuit.ssp.R as SspR
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon

data class DaySummary(
    val dateLabel: String,
    val sortKey: String,
    val location: String?,
    val hours: Double,
    val isWeekend: Boolean,
    val rawEntry: CheckInDetail?,
    val hasEntry: Boolean,
    val entries: List<CheckInDetail>
)

fun groupByDay(
    list: List<CheckInDetail>,
    startDate: String,
    endDate: String
): List<DaySummary> {
    val outFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val entriesByDate: Map<String, List<CheckInDetail>> =
        list.groupBy { it.time.trim().split(Regex("\\s+")).firstOrNull().orEmpty() }

    val start = runCatching { keyFmt.parse(startDate) }.getOrNull() ?: return emptyList()
    val end = runCatching { keyFmt.parse(endDate) }.getOrNull() ?: return emptyList()

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    val result = mutableListOf<DaySummary>()
    val cal = Calendar.getInstance().apply { time = start }

    while (!cal.time.after(end) && !cal.time.after(today)) {
        val dateKey = keyFmt.format(cal.time)
        val entries = entriesByDate[dateKey]
        val isWeekendDay = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

        result.add(
            DaySummary(
                dateLabel = outFmt.format(cal.time),
                sortKey = dateKey,
                location = entries?.firstOrNull()?.location,
                hours = entries?.maxOfOrNull { it.working_hours } ?: 0.0,
                isWeekend = isWeekendDay && entries.isNullOrEmpty(),
                rawEntry = entries?.firstOrNull(),
                hasEntry = !entries.isNullOrEmpty(),
                entries = entries ?: emptyList()
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return result.sortedByDescending { it.sortKey }
}

@Composable
fun CheckInListComponent(
    checkInList: List<CheckInDetail>,
    subFilters: List<FilterModel>,
    startDate: String,
    endDate: String,
    selectedChipId: String,
    noRecord: Boolean,
    onChipSelected: (FilterModel) -> Unit,
    onRowClick: (CheckInDetail) -> Unit
) {
    val days = groupByDay(checkInList, startDate, endDate)
    val presentCount = days.count { it.hasEntry }
    val weekendCount = days.count { it.isWeekend }
    val totalHrs = days.sumOf { it.hours }

    Column(
        Modifier
            .fillMaxSize()
            .padding(
                top = dimensionResource(SdpR.dimen._6sdp),
                bottom = dimensionResource(SdpR.dimen._12sdp)
            )
            .verticalScroll(rememberScrollState())
    ) {

        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
            ) {
                DateLabelBox("From", startDate, Modifier.weight(1f))
                DateLabelBox("To", endDate, Modifier.weight(1f))
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._8sdp))
        ) {
            StatCard(presentCount.toString(), PresentGreen, "Present", Modifier.weight(1f))
            StatCard(weekendCount.toString(), WeekendPurple, "Weekend", Modifier.weight(1f))
            StatCard("%.1f".format(totalHrs), FluentBlue, "Total hrs", Modifier.weight(1f))
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))

        if (subFilters.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(SdpR.dimen._6sdp))) {
                items(subFilters) { chip ->
                    AttendanceStatusChipItem(
                        filter = chip,
                        selected = (chip.id ?: "") == selectedChipId,
                        onClick = { onChipSelected(chip) }
                    )
                }
            }
            Spacer(Modifier.height(dimensionResource(SdpR.dimen._12sdp)))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(SdpR.dimen._4sdp))
        ) {
            Text(
                "DATE",
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrey,
                modifier = Modifier.weight(1.1f)
            )
            Text(
                "HRS",
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrey,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                "STATUS",
                fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                fontWeight = FontWeight.Bold,
                color = TextGrey,
                modifier = Modifier.weight(1.1f)
            )
        }

        Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))

        if (noRecord || days.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(SdpR.dimen._30sdp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No record found", color = TextGrey)
            }
        } else {
            AttendanceListCard(Modifier.fillMaxWidth()) {
                days.forEach { day ->
                    DayRow(day, onEditClick = onRowClick)
                }
            }
        }
    }
}

@Composable
private fun DateLabelBox(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
        Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))
        Box(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp)))
                .border(
                    width = 1.dp,
                    color = BorderGrey,
                    shape = RoundedCornerShape(dimensionResource(SdpR.dimen._8sdp))
                )
                .padding(dimensionResource(SdpR.dimen._10sdp))
        ) {
            Text(value, fontSize = dimensionResource(SspR.dimen._12ssp).value.sp)
        }
    }
}

@Composable
private fun DayRow(day: DaySummary, onEditClick: (CheckInDetail) -> Unit) {
    var expanded by remember(day.sortKey) { mutableStateOf(false) }
    val status = when {
        day.hasEntry -> "Present"
        day.isWeekend -> "Weekend"
        else -> "Absent"
    }

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
                    day.dateLabel,
                    fontSize = dimensionResource(SspR.dimen._12ssp).value.sp,
                    fontWeight = FontWeight.Normal
                )
                day.location?.let {
                    Text(it, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
                }
            }
            Row(Modifier.weight(0.8f), horizontalArrangement = Arrangement.Center) {
                if (day.hours > 0) {
                    Text(
                        "%.2f".format(day.hours),
                        fontSize = dimensionResource(SspR.dimen._12ssp).value.sp
                    )
                    Spacer(Modifier.width(dimensionResource(SdpR.dimen._2sdp)))
                    Text("hrs", fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, color = TextGrey)
                } else {
                    Text("—", fontSize = dimensionResource(SspR.dimen._13ssp).value.sp, color = TextGrey)
                }
            }
            Row(Modifier.weight(1.1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                StatusChip(status)
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

                    DetailLine("Date", day.dateLabel)
                    DetailLine(
                        "Working Hours",
                        if (day.hours > 0) "%.2f hrs".format(day.hours) else "—"
                    )
                    DetailLine("Location", day.location ?: "—")
                    DetailLine(
                        "Reason",
                        day.entries.firstOrNull { it.reason.isNotBlank() }?.reason ?: "—"
                    )

                    if (day.entries.isNotEmpty()) {
                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._8sdp)))

                        Text(
                            "Punches",
                            fontSize = dimensionResource(SspR.dimen._10ssp).value.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGrey
                        )

                        Spacer(Modifier.height(dimensionResource(SdpR.dimen._4sdp)))

                        day.entries
                            .sortedBy { it.time }
                            .forEach { entry ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .noRippleClickable { onEditClick(entry) }
                                        .padding(vertical = dimensionResource(SdpR.dimen._4sdp)),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        entry.log_type,
                                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        entry.time.trim().split(Regex("\\s+")).getOrNull(1)
                                            ?: entry.time,
                                        fontSize = dimensionResource(SspR.dimen._11ssp).value.sp,
                                        color = TextGrey
                                    )
                                }
                            }
                    }
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
        Text(value, fontSize = dimensionResource(SspR.dimen._11ssp).value.sp, fontWeight = FontWeight.Medium)
    }
}