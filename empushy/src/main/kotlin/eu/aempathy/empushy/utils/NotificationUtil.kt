package eu.aempathy.empushy.utils

import android.app.Notification
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification
import eu.aempathy.empushy.data.EmpushyNotification
import java.util.*

/**
 * Created by Kieran on 23/08/2018.
 */

object NotificationUtil {

    private val TAG = NotificationUtil::class.java.simpleName

    fun simplePackageName(context: Context, packageName: String): String {
        var appName = "(unknown)"

        val pm = context.packageManager
        var ai: ApplicationInfo? = null
        try {
            ai = pm.getApplicationInfo(packageName, 0)
        } catch (e: Exception) {
        }

        if (ai != null)
            appName = pm.getApplicationLabel(ai).toString()

        return appName
    }

    fun extractNotificationPostedValue(notification: EmpushyNotification, sbn: StatusBarNotification, context: Context) {
        notification.notifyId = sbn.id
        notification.app = sbn.packageName
        notification.appName = simplePackageName(context, sbn.packageName.trim { it <= ' ' })
        notification.time = sbn.postTime
        notification.id = sbn.postTime.toString()

        val n = sbn.notification
        if (n.tickerText != null)
            notification.ticker = n.tickerText.toString()
        // extras
        if (n.extras.containsKey(Notification.EXTRA_BIG_TEXT)) {
            notification.extraBigText = if (n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT) != null)
                n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_INFO_TEXT)) {
            notification.infoText = if (n.extras.getCharSequence(Notification.EXTRA_INFO_TEXT) != null)
                n.extras.getCharSequence(Notification.EXTRA_INFO_TEXT)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_SUB_TEXT)) {
            notification.subText = if (n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT) != null)
                n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_SUMMARY_TEXT)) {
            notification.summaryText = if (n.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) != null)
                n.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_TEXT)) {
            notification.extraText = if (n.extras.getCharSequence(Notification.EXTRA_TEXT) != null)
                n.extras.getCharSequence(Notification.EXTRA_TEXT)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_TEXT_LINES)) {
            notification.extraTextLines = if (n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES) != null)
                convertCharSequenceArrayToString(n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES))
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_TITLE)) {
            notification.extraTitle = if (n.extras.getCharSequence(Notification.EXTRA_TITLE) != null)
                n.extras.getCharSequence(Notification.EXTRA_TITLE)!!.toString()
            else
                ""
        }
        if (n.extras.containsKey(Notification.EXTRA_TITLE_BIG)) {
            notification.extraTitleBig = if (n.extras.getCharSequence(Notification.EXTRA_TITLE_BIG) != null)
                n.extras.getCharSequence(Notification.EXTRA_TITLE_BIG)!!.toString()
            else
                ""
        }
        if (n.category != null) {
            notification.category = n.category
        }
    }

    fun isInList(activeNotifications: ArrayList<EmpushyNotification>?,
                 notifyId: Int, appPackage: String): EmpushyNotification? {
        if (activeNotifications != null) {
            for (n in activeNotifications) {
                if (n.notifyId == notifyId && n.app == appPackage) {
                    return n
                }
            }
        }
        return null
    }

    private fun convertCharSequenceArrayToString(charSeq: Array<CharSequence>?): String {
        var concatenatedString = ""
        for (i in charSeq!!.indices) {
            concatenatedString = concatenatedString + " " + charSeq[i]
        }
        return concatenatedString
    }

    fun extractNotificationRemovedValue(notification: EmpushyNotification, sbn: StatusBarNotification, context: Context) {
        notification.removedTime = System.currentTimeMillis()
        notification.clicked = appOpenedSincePosting(context, notification.app, notification.time!!)
    }

    private fun appOpenedSincePosting(context: Context?, packageName: String?, postTime: Long): Boolean {
        if (context != null && packageName != null) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            if (usageStatsManager != null) {
                val queryEvents = usageStatsManager.queryEvents(postTime, end)

                if (queryEvents != null) {
                    var event: UsageEvents.Event

                    while (queryEvents.hasNextEvent()) {
                        val eventAux = UsageEvents.Event()
                        queryEvents.getNextEvent(eventAux)

                        if (eventAux.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            event = eventAux
                            if (event.packageName.trim { it <= ' ' }.contains(packageName.trim { it <= ' ' }))
                                return true
                        }
                    }
                }
            }
        }

        return false
    }
}
