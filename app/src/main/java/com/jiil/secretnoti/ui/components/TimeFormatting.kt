package com.jiil.secretnoti.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val clockFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val dateFormat = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)

fun messageDate(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

fun formatClockTime(at: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(at).atZone(zone).format(clockFormat)

fun formatDateLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "오늘"
    today.minusDays(1) -> "어제"
    else -> date.format(dateFormat)
}

fun formatRelativeTime(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
    val date = messageDate(at, zone)
    val today = messageDate(now, zone)
    return if (date == today) formatClockTime(at, zone) else formatDateLabel(date, today)
}
