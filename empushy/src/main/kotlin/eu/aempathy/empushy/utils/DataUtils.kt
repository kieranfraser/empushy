package eu.aempathy.empushy.utils

import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.data.EmpushyNotification
import java.util.*
import kotlin.collections.HashMap

/**
 * Various data manipulation/analysis functions.
 */
class DataUtils {

    companion object {

        private val TAG = DataUtils::class.java.simpleName

        fun filterNotificationsByDate(notifications: MutableList<EmpushyNotification>, calendar: Calendar): FloatArray{
            val totalFound = mutableListOf<EmpushyNotification>()
            val dayFor = calendar.get(Calendar.DAY_OF_YEAR)
            val yearFor = calendar.get(Calendar.YEAR)

            val c = Calendar.getInstance()
            for(n in notifications){

                // sets calendar to notification time
                c.timeInMillis = n.time?:0

                if(c.get(Calendar.DAY_OF_YEAR) == dayFor &&
                        c.get(Calendar.YEAR) == yearFor)
                    totalFound.add(n)
            }

            var totalAccepted = 0
            for(n in totalFound){
                if(n.clicked!!)
                    totalAccepted++
            }

            return floatArrayOf(totalFound.size.toFloat(), totalAccepted.toFloat(),
                    (totalFound.size - totalAccepted).toFloat())
        }

        fun filterNotificationsByHour(notifications: MutableList<EmpushyNotification>, calendar: Calendar): FloatArray{
            val totalFound = mutableListOf<EmpushyNotification>()
            val dayFor = calendar.get(Calendar.DAY_OF_YEAR)
            val yearFor = calendar.get(Calendar.YEAR)
            val hourFor = calendar.get(Calendar.HOUR_OF_DAY)

            val c = Calendar.getInstance()
            for(n in notifications){

                // sets calendar to notification time
                c.timeInMillis = n.time?:0

                if(c.get(Calendar.DAY_OF_YEAR) == dayFor &&
                        c.get(Calendar.YEAR) == yearFor && c.get(Calendar.HOUR_OF_DAY) == hourFor)
                    totalFound.add(n)
            }

            var totalAccepted = 0
            for(n in totalFound){
                if(n.clicked!!)
                    totalAccepted++
            }

            return floatArrayOf(totalFound.size.toFloat(), totalAccepted.toFloat(),
                    (totalFound.size - totalAccepted).toFloat())
        }

        fun notificationAnalysis(notifications: MutableList<EmpushyNotification>): ArrayList<AppSummaryItem>{
            // separate into app
            val activeAppItems = ArrayList<AppSummaryItem>()
            for(notification in notifications){
                // check if app is in summary list
                // to get a a string
                if(notification.hidden == false) {
                    try {
                        val selectedItem: AppSummaryItem = activeAppItems.filter { s -> s.app == notification.app }.single()
                        selectedItem.active?.add(notification)
                    } catch (e: Exception) {
                        val activeAppItem = AppSummaryItem()
                        activeAppItem.app = notification.app
                        activeAppItem.appName = notification.appName
                        activeAppItem.active = arrayListOf()
                        activeAppItem.active?.add(notification)
                        activeAppItems.add(activeAppItem)
                    }
                }
            }
            return activeAppItems
        }
    }
}