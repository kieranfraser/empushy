package eu.aempathy.empushy.utils

import android.app.Notification
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aempathy.NLPAndroid.NLP
import com.aempathy.NLPAndroid.models.Entity
import eu.aempathy.empushy.data.EmpushyNotification
import eu.aempathy.empushy.data.Feature
import eu.aempathy.empushy.services.EmpushyNotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


/**
 * Created by Kieran on 23/08/2018.
 */

object NotificationUtil {

    private val TAG = NotificationUtil::class.java.simpleName
    private val TOPIC_DEFAULT = "unknown"
    private val SENTIMENT_DEFAULT = -1.0
    private val ENTITIES_DEFAULT = mutableListOf<Entity>()

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

    fun extractNotificationPostedValue(notification: EmpushyNotification, sbn: StatusBarNotification, context: Context,
                                       features: List<Feature>, nlp: NLP?) {

        val n = sbn.notification

        notification.id = sbn.postTime.toString()
        notification.notifyId = sbn.id

        var feature = features.filter { f -> f.name == "app" }
        if(feature.isNotEmpty() && feature[0].enabled!!) {
            notification.app = sbn.packageName
            notification.appName = simplePackageName(context, sbn.packageName.trim { it <= ' ' })
        }

        feature = features.filter { f -> f.name == "time" }
        if(feature.isNotEmpty() && feature[0].enabled!!)
            notification.time = sbn.postTime

        feature = features.filter { f -> f.name == "text" }
        if(feature.isNotEmpty() && feature[0].enabled!!) {
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

            val usefulText = extractUsefulText(notification)

            var topic = TOPIC_DEFAULT
            runBlocking {
                launch (Dispatchers.Default) {
                    val gResult = async { nlp?.classifyTopic(usefulText) }
                    topic = gResult.await()?:TOPIC_DEFAULT
                }
            }
            notification.subject = topic

            var sentiment = SENTIMENT_DEFAULT
            runBlocking {
                launch (Dispatchers.Default) {
                    val gResult = async { nlp?.classifySentiment(usefulText) }
                    sentiment = gResult.await()?: SENTIMENT_DEFAULT
                }
            }
            notification.sentiment = sentiment

            var entities = ENTITIES_DEFAULT
            runBlocking {
                launch (Dispatchers.Default) {
                    val gResult = async { nlp?.namedEntityRecognition(usefulText) }
                    entities = gResult.await()?: ENTITIES_DEFAULT
                }
            }
            notification.entities = ArrayList(entities)
        }

        feature = features.filter { f -> f.name == "category" }
        if(feature.isNotEmpty() && feature[0].enabled!!)
            if (n.category != null) {
                notification.category = n.category
            }
    }

    fun isInList(activeNotifications: MutableList<EmpushyNotification>?,
                 notifyId: Int, appPackage: String, ticker: String): EmpushyNotification? {
        if (activeNotifications != null) {
            for (n in activeNotifications) {
                if (n.app == appPackage) {
                    return n
                }
            }
        }
        return null
    }

    fun updateEmPushyNotifyId(activeNotifications: MutableList<EmpushyNotification>?, notification: EmpushyNotification?): MutableList<EmpushyNotification>? {
        if (activeNotifications != null && notification != null) {
            for (n in activeNotifications) {
                if (n.app == notification.app) {
                    Log.d(TAG, "Updating notify id "+notification.empushyNotifyId)
                    n.empushyNotifyId = notification.empushyNotifyId
                }
            }
        }
        return activeNotifications
    }

    fun removeNotificationById(activeNotifications: MutableList<EmpushyNotification>?, id: String): MutableList<EmpushyNotification>? {
        if (activeNotifications != null) {
            val found = activeNotifications.filter { n -> n.id == id }.singleOrNull()
            if(found!=null)
                activeNotifications.remove(found)
        }
        return activeNotifications
    }

    private fun convertCharSequenceArrayToString(charSeq: Array<CharSequence>?): String {
        var concatenatedString = ""
        for (i in charSeq!!.indices) {
            concatenatedString = concatenatedString + " " + charSeq[i]
        }
        return concatenatedString
    }

    fun extractNotificationRemovedValue(notification: EmpushyNotification, context: Context, features: List<Feature>) {
        var feature = features.filter { f -> f.name == "time" }
        if(feature.isNotEmpty() && feature[0].enabled!!)
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

    fun notificationServiceRunning(context: Context): Boolean{
        val notificationActive = false

        return notificationActive
    }

    fun extractUsefulText(n: EmpushyNotification): String {
        var text = ""

        if(!n.ticker.isNullOrEmpty() && !text.contains(n.ticker?:""))
            text += "..."+n.ticker
        if(!n.subText.isNullOrEmpty() && !text.contains(n.subText?:""))
            text += "..."+n.subText
        if(!n.summaryText.isNullOrEmpty() && !text.contains(n.summaryText?:""))
            text += "..."+n.summaryText
        if(!n.infoText.isNullOrEmpty() && !text.contains(n.infoText?:""))
            text += "..."+n.infoText
        if(!n.extraBigText.isNullOrEmpty() && !text.contains(n.extraBigText?:""))
            text += "..."+n.extraBigText
        if(!n.extraText.isNullOrEmpty() && !text.contains(n.extraText?:""))
            text += "..."+n.extraText
        if(!n.extraTextLines.isNullOrEmpty() && !text.contains(n.extraTextLines?:""))
            text += "..."+n.extraTextLines

        return text
    }

    fun createOpenAppIntent(context: Context, packageName: String): Intent {

        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            // We found the activity now start the activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        } else {
            // Bring user to the market or let them choose an app?
            intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.data = Uri.parse("market://details?id=$packageName")
            return intent
        }
    }

    fun createNotificationOpenIntent(context: Context, nId: Int, id: String, appId: String): PendingIntent{
        val myService = Intent(context, EmpushyNotificationService::class.java)
        myService.putExtra("notification", id)
        myService.putExtra("package", appId)
        myService.setAction(Constants.ACTION.OPEN_ACTION);
        return PendingIntent.getService(context, nId,
                myService, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun createNotificationRemoveIntent(context: Context, nId: Int, id: String): PendingIntent{
        val myService = Intent(context, EmpushyNotificationService::class.java)
        myService.putExtra("notification", id)
        myService.setAction(Constants.ACTION.REMOVAL_ACTION);
        return PendingIntent.getService(context, nId,
                myService, PendingIntent.FLAG_UPDATE_CURRENT)
    }

}
