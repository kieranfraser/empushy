package eu.aempathy.empushy.utils

import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.data.EmpushyNotification
import java.util.*


/**
 * Created by Kieran on 30/03/2018.
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

        fun notificationAnalysis(notifications: ArrayList<EmpushyNotification>): ArrayList<AppSummaryItem>{
            // separate into app
            val appSummaryItems = ArrayList<AppSummaryItem>()
            for(notification in notifications){
                // check if app is in summary list
                // to get a a string
                try {
                    val selectedItem: AppSummaryItem = appSummaryItems.filter { s -> s.app == notification.app }.single()
                    if(notification.hidden == true)
                        selectedItem.hidden?.add(notification)
                    else
                        selectedItem.active?.add(notification)
                } catch(e: Exception){

                    val list = ArrayList<EmpushyNotification>()
                    list.add(notification)
                    var newItem: AppSummaryItem;
                    if(notification.hidden?:false)
                        newItem = AppSummaryItem(notification.app,
                                notification.appName, ArrayList(), list)
                    else
                        newItem = AppSummaryItem(notification.app,
                                notification.appName, list, ArrayList())
                    appSummaryItems.add(newItem)
                }

            }
            return appSummaryItems
        }
    }
}