package com.costura.pro.utils

import java.text.SimpleDateFormat
import java.util.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


object DateUtils {

    fun getCurrentDateFormatted(): String {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun formatDateForDisplay(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    // NUEVOS MÉTODOS para nueva estructura
    fun getCurrentDateString(): String {
        return DateTime().toString(Constants.DATE_FORMAT)
    }

    fun getCurrentYearMonth(): String {
        return DateTime().toString(Constants.YEAR_MONTH_FORMAT)
    }

    fun getCurrentTimeString(): String {
        return DateTime().toString(Constants.TIME_FORMAT)
    }

    fun parseDateString(dateString: String): DateTime {
        return DateTime.parse(dateString, DateTimeFormat.forPattern(Constants.DATE_FORMAT))
    }

    fun formatToYearMonth(dateString: String): String {
        val date = parseDateString(dateString)
        return date.toString(Constants.YEAR_MONTH_FORMAT)
    }
}