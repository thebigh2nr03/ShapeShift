package com.usc.shapeshift

import java.time.LocalDate

data class User(val Name : String? = null, val events : Map<LocalDate, List<CalendarEntry>>?){}
