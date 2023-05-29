package com.eazy.daikoupos.utils.payment

import java.text.SimpleDateFormat
import java.util.*

object DateHelper {

    @JvmStatic
    fun getDateFormatString(millis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val date = Date(millis)
        val locale = Locale.getDefault()
        val dateFormat = SimpleDateFormat(pattern, locale)
        return dateFormat.format(date)
    }

}