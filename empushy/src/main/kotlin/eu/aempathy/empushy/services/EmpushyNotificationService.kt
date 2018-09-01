package eu.aempathy.empushy.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.activities.DetailActivity
import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.data.EmpushyNotification
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.utils.Constants
import eu.aempathy.empushy.utils.DataUtils
import eu.aempathy.empushy.utils.NotificationUtil
import eu.aempathy.empushy.utils.StateUtils
import java.util.*


class EmpushyNotificationService : NotificationListenerService() {

    private val TAG = EmpushyNotificationService::class.java.simpleName

    private var runningService = false

    private var firebaseApp: FirebaseApp? = null
    private var authInstance: FirebaseAuth? = null
    private var ref: DatabaseReference? = null
    private var runningRef: DatabaseReference ?= null
    private var runningListener: ChildEventListener ?= null

    private var activeList: ArrayList<EmpushyNotification>? = null
    private var cachedList: ArrayList<EmpushyNotification>? = null

    /*override fun onTaskRemoved(rootIntent: Intent) {
        if (authInstance != null && authInstance!!.currentUser != null && runningService) {
            val restartService = Intent(applicationContext,
                    this.javaClass)
            restartService.`package` = packageName
            val restartServicePI = PendingIntent.getService(
                    applicationContext, 1, restartService,
                    PendingIntent.FLAG_ONE_SHOT)
            val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 10000, restartServicePI)
        }
    }*/

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            if(authInstance!=null && authInstance?.currentUser != null) {
                val checkRunningRef = ref?.child("users")
                        ?.child(authInstance?.currentUser?.uid?:"none")
                        ?.child("running")
                        ?.child(NotificationUtil.simplePackageName(applicationContext, applicationContext.packageName))
                checkRunningRef?.keepSynced(true)
                checkRunningRef?.addListenerForSingleValueEvent(runningReadListenerSingle)
            }
        }
        else if (intent.getAction().equals( Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent")
            stopService()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    var runningReadListenerSingle: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.key == NotificationUtil.simplePackageName(applicationContext, applicationContext.packageName)) {
                startNotificationService(ArrayList(), false)
                runningService = true
            }
            else {
                stopService()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }


    private fun startNotificationService(items: ArrayList<AppSummaryItem>, update: Boolean){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            promote26(items, update)
        } else {
            val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
            val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)


            collapsedView.setTextViewText(R.id.tv_notification_collapsed_need_attention, activeList?.size.toString())
            collapsedView.setTextViewText(R.id.tv_notification_collapsed_for_later, cachedList?.size.toString())
            expandedView.setTextViewText(R.id.tv_notification_expanded_need_attention, activeList?.size.toString())
            expandedView.setTextViewText(R.id.tv_notification_collapsed_for_later, cachedList?.size.toString())

            for(item in items) {
                try {
                    val icon = applicationContext.packageManager.getApplicationIcon(item.app)
                    val bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
                    icon.draw(canvas)
                    if(items.indexOf(item) == 0) {
                        expandedView.setImageViewBitmap(R.id.iv_notification_icon_1, bitmap)
                        expandedView.setViewVisibility(R.id.iv_notification_icon_1, View.VISIBLE)
                    }
                    if(items.indexOf(item) == 1) {
                        expandedView.setImageViewBitmap(R.id.iv_notification_icon_2, bitmap)
                        expandedView.setViewVisibility(R.id.iv_notification_icon_2, View.VISIBLE)
                    }
                    if(items.indexOf(item) == 2) {
                        expandedView.setImageViewBitmap(R.id.iv_notification_icon_3, bitmap)
                        expandedView.setViewVisibility(R.id.iv_notification_icon_3, View.VISIBLE)
                        if(items.size>3)
                            expandedView.setViewVisibility(R.id.tv_notification_more, View.VISIBLE)
                        break
                    }
                } catch (e: Exception) {}
            }

            val builder = android.support.v4.app.NotificationCompat.Builder(this)
                    .setContentTitle("EmPushy")
                    .setContentText("EmPushy legacy running.")
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, DetailActivity::class.java), 0))
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setStyle(android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle())

            startForeground(1, builder.build())
        }
    }

    @TargetApi(26)
    private fun promote26(items: ArrayList<AppSummaryItem>, update: Boolean) {

        val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
        val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)


        collapsedView.setTextViewText(R.id.tv_notification_collapsed_need_attention, activeList?.size.toString())
        collapsedView.setTextViewText(R.id.tv_notification_collapsed_for_later, cachedList?.size.toString())
        expandedView.setTextViewText(R.id.tv_notification_expanded_need_attention, activeList?.size.toString())
        expandedView.setTextViewText(R.id.tv_notification_collapsed_for_later, cachedList?.size.toString())

        for(item in items) {

            Log.d(TAG, "App item: "+item.appName)
            try {
                val icon = applicationContext.packageManager.getApplicationIcon(item.app)
                val bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
                icon.draw(canvas)
                if(items.indexOf(item) == 0) {
                    expandedView.setImageViewBitmap(R.id.iv_notification_icon_1, bitmap)
                    expandedView.setViewVisibility(R.id.iv_notification_icon_1, View.VISIBLE)
                }
                if(items.indexOf(item) == 1) {
                    expandedView.setImageViewBitmap(R.id.iv_notification_icon_2, bitmap)
                    expandedView.setViewVisibility(R.id.iv_notification_icon_2, View.VISIBLE)
                }
                if(items.indexOf(item) == 2) {
                    expandedView.setImageViewBitmap(R.id.iv_notification_icon_3, bitmap)
                    expandedView.setViewVisibility(R.id.iv_notification_icon_3, View.VISIBLE)
                    if(items.size>3)
                        expandedView.setViewVisibility(R.id.tv_notification_more, View.VISIBLE)
                    break
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error setting icons.")}
        }


        val mNotifyManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(mNotifyManager)
        val builder = NotificationCompat.Builder(applicationContext, ANDROID_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setContentTitle("EmPushy Title")
                .setContentText("EmPushy descript")
                .setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, DetailActivity::class.java), 0))
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle())
        if(update)
            mNotifyManager.notify(21192, builder.build())
        else
            startForeground(21192, builder.build())
    }

    @TargetApi(26)
    private fun createChannel(notificationManager: NotificationManager) {
        val name = "EmPushy"
        val description = "EmPushy description here"
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val mChannel = NotificationChannel(name, name, importance)
        mChannel.description = description
        mChannel.enableLights(false)
        mChannel.lightColor = Color.BLUE
        notificationManager.createNotificationChannel(mChannel)
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        Empushy.initEmpushyApp(applicationContext)
        firebaseApp = FirebaseApp.getInstance("empushy")
        authInstance = FirebaseAuth.getInstance(firebaseApp!!)
        ref = FirebaseDatabase.getInstance(firebaseApp!!).reference
        activeList = ArrayList()
        cachedList = ArrayList()
        // get rules
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (authInstance != null && runningService) {
            val currentUser = authInstance?.currentUser
            if (currentUser != null && sbn.packageName != applicationContext.packageName) {
                Log.i(TAG, "Notification Posted! from " + sbn.packageName)
                val notification = EmpushyNotification()
                NotificationUtil.extractNotificationPostedValue(notification, sbn, applicationContext)
                val activeNotification = NotificationUtil.isInList(activeList, sbn.id, sbn.packageName)
                if (activeNotification != null) {
                    activeList!!.remove(activeNotification)
                    ref!!.child("notifications").child(currentUser.uid).child("mobile").child(activeNotification.id!!).removeValue()
                }
                if (activeList != null)
                    activeList!!.add(notification)
                ref!!.child("notifications").child(currentUser.uid).child("mobile").child(sbn.postTime.toString()).setValue(notification)
                // update notification
                updateEmpushyNotification()
            }
        }
        // update notification
    }

    override fun onDestroy() {
        try {
            runningRef?.removeEventListener(runningListener!!)
        }catch(e:Exception){}
        Log.d(TAG, "Stopping "+applicationContext.packageName+" notification service.")
    }


    override fun onNotificationRemoved(sbn: StatusBarNotification) {

        Log.i(TAG, "Notification Removed. App: " + sbn.packageName)
        if (authInstance != null && runningService) {
            val currentUser = authInstance!!.currentUser
            if (currentUser != null && sbn.packageName != applicationContext.packageName) {
                Log.i(TAG, "Notification Removed")

                val activeNotification = NotificationUtil.isInList(activeList, sbn.id, sbn.packageName)
                if (activeNotification != null) {
                    NotificationUtil.extractNotificationRemovedValue(activeNotification, sbn, applicationContext)
                    /*if(placeCats.isNotEmpty())
                    activeNotification.placeCategories = placeCats.toList() as ArrayList<Int>*/
                    ref!!.child("archive/notifications").child(currentUser.uid).child("mobile").child(activeNotification.id!!).setValue(activeNotification)
                    if (activeList != null)
                        activeList!!.remove(activeNotification)
                } else {
                    val cachedNotification = NotificationUtil.isInList(cachedList, sbn.id, sbn.packageName)
                    if (cachedNotification != null) {
                        NotificationUtil.extractNotificationRemovedValue(cachedNotification, sbn, applicationContext)
                        /*if(placeCats.isNotEmpty())
                        cachedNotification.placeCategories = placeCats.toList() as ArrayList<Int>*/
                        ref!!.child("cached/notifications").child(currentUser.uid).child("mobile").child(cachedNotification.id!!)
                                .setValue(cachedNotification)
                        if (cachedList != null)
                            cachedList!!.remove(cachedNotification)
                        return
                    }
                    val notification = EmpushyNotification()
                    NotificationUtil.extractNotificationPostedValue(notification, sbn, applicationContext)
                    NotificationUtil.extractNotificationRemovedValue(notification, sbn, applicationContext)
                    ref!!.child("archive/notifications").child(currentUser.uid).child("mobile").child(notification.id!!).setValue(notification)
                }

                ref!!.child("notifications").child(currentUser.uid).child("mobile").child(sbn.postTime.toString()).removeValue()
                consolidateActiveList(currentUser.uid)
                // update notification
                updateEmpushyNotification()
            }
        }
    }

    private fun updateEmpushyNotification(){
        Log.d(TAG, "Updating notification")
        val appItems = DataUtils.notificationAnalysis(activeList?: ArrayList())

        Log.d(TAG, "Apps: "+appItems.size)
        startNotificationService(appItems, true)
    }

    private fun consolidateActiveList(userId: String){
        // get notifications from server
        val notificationsRef = ref?.child("notifications")?.child(userId)?.child("mobile")
        notificationsRef?.orderByKey()?.addListenerForSingleValueEvent(consolidateReadListener)
    }

    var consolidateReadListener: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(authInstance!=null) {
                val currentUser = authInstance?.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "Consolidating active list.")
                    val newNotifications = ArrayList<EmpushyNotification>();
                    for (child in snapshot.children) {
                        val notification = child?.getValue(EmpushyNotification::class.java)
                        if (notification != null) {
                            newNotifications.add(notification)
                        }
                    }
                    for (notification: EmpushyNotification in newNotifications) {
                        val foundList = activeList?.filter { n -> n.notifyId == notification.notifyId && n.app == notification.app }
                        if (foundList == null || foundList.isEmpty())
                            ref?.child("notifications")?.child(currentUser.uid)?.child("mobile")?.child(notification.id
                                    ?: "none")?.removeValue()
                    }
                    val calendar = Calendar.getInstance()
                    val today = calendar.get(Calendar.DAY_OF_YEAR)
                    val toRemove = mutableListOf<EmpushyNotification>()
                    for (notification in activeList ?: ArrayList()) {
                        calendar.timeInMillis = notification.time ?: 0
                        if (calendar.get(Calendar.DAY_OF_YEAR) != today) {
                            toRemove.add(notification)
                            ref?.child("notifications")?.child(currentUser.uid)?.child("mobile")?.child(notification.id
                                    ?: "none")?.removeValue()
                        }
                    }
                    for (notification in toRemove)
                        activeList?.remove(notification)
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }


    private fun stopService(){
        stopForeground(true)
        stopSelf()
    }

    companion object {
        val ANDROID_CHANNEL_ID = "EmPushy"
    }
}