package com.jiil.secretnoti.ui.components

import android.content.Context
import com.jiil.secretnoti.NotiApp
import com.jiil.secretnoti.R
import java.time.format.FormatStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun clockFormat(): DateTimeFormatter {
    val locale = Locale.getDefault()
    return if (locale.language == Locale.KOREAN.language) DateTimeFormatter.ofPattern("a h:mm", locale)
    else DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
}

private fun dateFormat(): DateTimeFormatter {
    val locale = Locale.getDefault()
    val pattern = if (locale.language == Locale.KOREAN.language) "M월 d일" else "MMM d"
    return DateTimeFormatter.ofPattern(pattern, locale)
}

fun messageDate(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

fun formatClockTime(at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(at).atZone(zone).format(clockFormat())

fun formatDateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    formatDateLabel(NotiApp.instance, date, today)

internal fun formatDateLabel(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> context.getString(R.string.time_today)
    today.minusDays(1) -> context.getString(R.string.time_yesterday)
    else -> date.format(dateFormat())
}

fun formatRelativeTime(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String =
    formatRelativeTime(NotiApp.instance, at, now, zone)

internal fun formatRelativeTime(context: Context, at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    val date = messageDate(at, zone)
    val today = messageDate(now, zone)
    return if (date == today) formatClockTime(at, zone) else formatDateLabel(context, date, today)
}
