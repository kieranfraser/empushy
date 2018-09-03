package eu.aempathy.empushy.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Kieran on 09/04/2018.
 */
class DateUtils {

    companion object {

        var daysOfWeekNames = arrayOf("SUN", "MON", "TUES",
                "WED", "THUR", "FRI", "SAT")

        fun getDate(milliSeconds: Long, dateFormat: String?): String {
            // Create a DateFormatter object for displaying date in specified format.
            val formatter: SimpleDateFormat
            if (dateFormat != null) {
                formatter = SimpleDateFormat(dateFormat)
            } else {
                formatter = SimpleDateFormat()
            }


            // Create a calendar object that will convert the date and time value in milliseconds to date.
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            return formatter.format(calendar.time)
        }

        fun newDay(lastTime: Long): Boolean {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = lastTime
            val lastDay = calendar.get(Calendar.DAY_OF_YEAR)
            val lastYear = calendar.get(Calendar.YEAR)
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            if (currentDay > lastDay)
                return true
            else if (currentYear > lastYear)
                return true

            return false
        }

        fun getEpochMinus4Seconds(milliSeconds: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            calendar.add(Calendar.SECOND, -4)
            return calendar.timeInMillis
        }

        fun getEpochMinus2Hours(milliSeconds: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            calendar.add(Calendar.HOUR, -2)
            return calendar.timeInMillis
        }

        fun getEpochMinus3Hours(milliSeconds: Long): Long {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = milliSeconds
            calendar.add(Calendar.HOUR, -3)
            return calendar.timeInMillis
        }

        /**
         * Helper function to calcuate the time since epoch and convert to clean string
         * in the form X s/mins/hours/days ago.
         * @param epoch
         * @return
         */
        fun timeReceived(epoch: Long): String {
            var received = ""
            val differenceMillis = System.currentTimeMillis() - epoch

            if (differenceMillis / 1000 < 60) { // seconds
                received = (differenceMillis / 1000).toString() + " second(s) ago"
            } else if (differenceMillis / (1000 * 60) < 60) { // minutes
                received = (differenceMillis / (1000 * 60)).toString() + " min(s) ago"
            } else if (differenceMillis / (1000 * 60 * 60) < 24) { // hours
                received = (differenceMillis / (1000 * 60 * 60)).toString() + " hour(s) ago"
            } else { // days
                received = (differenceMillis / (1000 * 60 * 60 * 24)).toString() + "day(s) ago"
            }

            return received
        }

        fun isDayOld(epoch: Long): Boolean {
            val differenceMillis = System.currentTimeMillis() - epoch
            return (differenceMillis / (1000 * 60 * 60) >= 24)
        }

        fun getDayOfWeek(calendar: Calendar): String{
            val date_of_week = calendar.get(Calendar.DAY_OF_WEEK)
            return daysOfWeekNames[date_of_week - 1]
        }

        fun getPreviousWeekDays(): Array<String>{
            val calendar = Calendar.getInstance()
            val names = mutableListOf<String>()
            names.add(getDayOfWeek(calendar))
            for(i in 1..6){
                calendar.add(Calendar.DAY_OF_WEEK, -1)
                names.add(getDayOfWeek(calendar))
            }
            names.reverse()
            return names.toTypedArray()
        }
    }
}