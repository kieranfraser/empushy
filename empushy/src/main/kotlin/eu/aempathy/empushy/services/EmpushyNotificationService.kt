package eu.aempathy.empushy.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.activities.DetailActivity
import eu.aempathy.empushy.data.*
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import eu.aempathy.empushy.utils.*
import java.lang.ref.WeakReference
import java.util.*


/**
 * Notification Listener Service
 * - implemented as a foreground service with paired EmPushy notification in the status bar (which shows
 * the current number of notifications which need attention and the current number of notifications cached for
 * later consumption).
 * - listens to the status bar for incoming notifications
 * - logs incoming notifications, updates EmPushy foreground notification and removes notification from status bar.
 */
class EmpushyNotificationService : NotificationListenerService() {

    private val TAG = EMPUSHY_TAG + EmpushyNotificationService::class.java.simpleName

    val ANDROID_CHANNEL_ID = "EmPushy"
    val EMPUSHY_NOTIFICATION_ID = 21192;

    private var runningService = false

    private var firebaseApp: FirebaseApp? = null
    private var authInstance: FirebaseAuth? = null
    private var ref: DatabaseReference? = null


    var featureRef: DatabaseReference ?= null
    var featureManager: FeatureManager?= null
    var featureListener: ValueEventListener ?= null

    private var runningRef: DatabaseReference ?= null
    private var runningListener: ChildEventListener ?= null

    private var removalActiveRef: DatabaseReference ?= null
    private var removalActiveListener: ChildEventListener ?= null

    private var removalCacheRef: DatabaseReference ?= null
    private var removalCacheListener: ChildEventListener ?= null

    private var activeList: ArrayList<EmpushyNotification>? = null
    private var cachedList: ArrayList<EmpushyNotification>? = null

    override fun onTaskRemoved(rootIntent: Intent) {
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
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
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
        else if (intent.action.equals( Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent")
            runningService = false
            stopService()
        }
        else if(intent.action.equals( Constants.ACTION.REMOVAL_ACTION)){
            val notificationId = intent.getStringExtra("notification")
            notificationRemoval(notificationId, false)
        }
        else if(intent.action.equals( Constants.ACTION.OPEN_ACTION)){
            val notificationId = intent.getStringExtra("notification")
            val packageId = intent.getStringExtra("package")
            notificationRemoval(notificationId, true)
            startActivity(NotificationUtil.createOpenAppIntent(applicationContext, packageId))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun notificationRemoval(id: String?, opened: Boolean){
        if(StateUtils.isNetworkAvailable(applicationContext) && authInstance!=null) {
            val currentUser = authInstance?.currentUser
            if (currentUser != null) {
                Log.d(TAG, "Finding opened notification: ")
                Log.d(TAG, id)
                Log.d(TAG, activeList.toString())
                val foundNotifications: List<EmpushyNotification>? = activeList?.filter { n -> n.id == id }

                    if (foundNotifications != null && !foundNotifications.isEmpty()) {
                        val activeNotification = foundNotifications[0]
                        NotificationUtil.extractNotificationRemovedValue(activeNotification, applicationContext,
                                featureManager?.features?.filter { f -> f.category == FEATURE_CAT_NOTIFICATION }?: listOf())

                        if(opened)
                            activeNotification.clicked = true

                        ref?.child("notifications")?.child(currentUser.uid?:"none")?.child("mobile")?.child(activeNotification.id?:"none")?.removeValue()
                        ref!!.child("archive/notifications").child(currentUser.uid).child("mobile").child(activeNotification.id!!).setValue(activeNotification)


                        if (activeList != null)
                            activeList!!.remove(activeNotification)

                        updateEmpushyNotification(activeList!!, activeNotification, false)
                        /*val mNotifyManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        mNotifyManager.cancel(activeNotification.empushyNotifyId?:-1)*/
                    }
                    else{
                        Log.d(TAG, "Could not find notification")
                    }
                //consolidateActiveList(currentUser.uid)
            }
        }
    }

    var runningReadListenerSingle: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.key == NotificationUtil.simplePackageName(applicationContext, applicationContext.packageName)
                && snapshot.value != null) {

                startNotificationService(ArrayList(), 0, false)
                //subscribeToRemovals()
                runningService = true
                consolidateActiveList(authInstance?.currentUser?.uid?:"none")
            }
            else {
                stopService()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    /**
     * Creates the EmPush notification (necessary for background service running)
     */
    private fun startNotificationService(items: ArrayList<AppSummaryItem>, numHiddenItems: Int, update: Boolean){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            promote26(update)
        } else {
            val expandedView = RemoteViews(packageName, R.layout.notification_expanded)
            val collapsedView = RemoteViews(packageName, R.layout.notification_collapsed)
            val hiddenNum = numHiddenItems
            var activeNum = 0

            for(item in items) {

                activeNum+=item.active?.size?:0

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

            collapsedView.setTextViewText(R.id.tv_notification_collapsed_need_attention, activeNum.toString())
            expandedView.setTextViewText(R.id.tv_notification_expanded_need_attention, activeNum.toString())
            expandedView.setTextViewText(R.id.tv_notification_expanded_for_later, hiddenNum.toString())


            val notificationIntent = Intent(applicationContext, DetailActivity::class.java)
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val intent = PendingIntent.getActivity(applicationContext, 0,
                    notificationIntent, 0)

            val builder = android.support.v4.app.NotificationCompat.Builder(this)
                    .setContentTitle("EmPushy")
                    .setSmallIcon(R.mipmap.ic_empushy)
                    .setSubText("(EmPushy)")
                    .setContentIntent(intent)
                    .setContentText("EmPushy running.")
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setStyle(android.support.v4.app.NotificationCompat.DecoratedCustomViewStyle())

            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(1, builder.build())
        }
    }

    @TargetApi(26)
    private fun promote26( update: Boolean) {

        val GROUP_KEY_EMPUSHY = "eu.aempathy.EMPUSHY"

        val mNotifyManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(mNotifyManager)
        mNotifyManager.cancelAll()

        val notificationIntent = Intent(applicationContext, DetailActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val intent = PendingIntent.getActivity(applicationContext, 0,
                notificationIntent, 0)

        val notificationList = arrayListOf<Notification>()
        var activeNum = 0

        val pre24SummaryNotification = NotificationCompat.Builder(applicationContext, ANDROID_CHANNEL_ID)
                .setContentTitle("Nothing for now. "+(if(activeList?.size?.minus(activeNum) == 0) "Nothing" else "Some")+" saved for later.")
                //set content text to support devices running API level < 24
                .setContentText("Press to open EmPushy")
                .setSmallIcon(R.mipmap.ic_empushy)
                .setContentIntent(intent)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //build summary info into InboxStyle template
                .setStyle(NotificationCompat.InboxStyle()
                        .setSummaryText("running EmPushy"))
                //specify which group this notification belongs to
                .setGroup(GROUP_KEY_EMPUSHY)
                //set this notification as the summary for the group
                .setGroupSummary(true)
                .build()

        //startForeground(EMPUSHY_NOTIFICATION_ID, pre24SummaryNotification)
        NotificationManagerCompat.from(applicationContext).apply {
            var id = EMPUSHY_NOTIFICATION_ID+1
            for(notification in notificationList) {
                notify(id, notification)
                id++
            }
            //notify(0, pre24SummaryNotification)
        }

        startForeground(EMPUSHY_NOTIFICATION_ID, pre24SummaryNotification)

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
        Empushy.initialise(applicationContext)
        firebaseApp = FirebaseApp.getInstance("empushy")
        authInstance = FirebaseAuth.getInstance(firebaseApp!!)
        ref = FirebaseDatabase.getInstance(firebaseApp!!).reference
        activeList = ArrayList()
        cachedList = ArrayList()

        try {
            featureManager = FeatureManager(arrayListOf(), listOf(), ref!!, authInstance?.currentUser?.uid!!)
        }catch(e: Exception){
            Log.d(TAG, "Error creating feature manager.")
        }
        getFeatures()
        subscribeToRunning()
        // get rules
    }

    private fun getFeatures(){
        try {
            featureRef = ref?.child(FeatureManager.USER_PATH)?.child(authInstance!!.currentUser!!.uid)?.child(FeatureManager.FEATURE_PATH)
            featureListener = featureRef?.addValueEventListener(featureReadListner)
        } catch(e: Exception){Log.d(TAG, "Unable to get features.")}
    }

    var featureReadListner: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(snapshot.hasChildren()) {
                val featureList = ArrayList<Feature>()
                val categoryList = ArrayList<String>()
                for (child in snapshot.children) {
                    val feature = child?.getValue(Feature::class.java)
                    if (feature != null) {
                        featureList.add(feature)
                        categoryList.add(feature.category?:"none")
                    }
                }
                featureManager?.features = featureList
                featureManager?.categories = categoryList.distinct()
            }
            else{
                getCommonFeatures()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    /**
     * Call to get common features
     *  - calls featureManager update() to update user in FB
     *  - oneTimeCall
     */
    private fun getCommonFeatures(){
        try {
            ref?.child(FeatureManager.COMMON_FEATURE_PATH)?.addListenerForSingleValueEvent(commonReadListener)
        } catch(e: Exception){Log.d(TAG, "Unable to get common features.")}
    }

    var commonReadListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(snapshot.hasChildren()) {
                val featureList = ArrayList<Feature>()
                val categoryList = ArrayList<String>()
                for (child in snapshot.children) {
                    val feature = child?.getValue(Feature::class.java)
                    if (feature != null) {
                        featureList.add(feature)
                        categoryList.add(feature.category?:"none")
                    }
                }
                featureManager?.features = featureList
                featureManager?.categories = categoryList.distinct()
                featureManager?.updateFeatures(featureList)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.i(TAG, "Notification Posted! from " + sbn.packageName)
        if (authInstance != null && runningService) {
            val currentUser = authInstance?.currentUser
            if (currentUser != null && sbn.packageName != applicationContext.packageName) {
                try {
                    val taskPosted = NotificationPostedTask(applicationContext, activeList
                            ?: arrayListOf(),
                            featureManager!!, authInstance!!, ref!!, this)
                    taskPosted.execute(*arrayOf(sbn))
                    cancelNotification(sbn.key)
                }catch (e: Exception){Log.d(TAG, "Exception starting posted background task: "+e.toString())}
            }
        }
        // update notification
    }

    private class NotificationPostedTask(context: Context, activeList: ArrayList<EmpushyNotification>, featureManager: FeatureManager,
                                         authInstance: FirebaseAuth, ref: DatabaseReference,
                                         service: EmpushyNotificationService) : AsyncTask<StatusBarNotification, EmpushyNotification, String>() {

        private val TAG = EMPUSHY_TAG + NotificationPostedTask::class.java.simpleName
        private val contextRef: WeakReference<Context>
        private val activeRef: WeakReference<ArrayList<EmpushyNotification>>
        private val authRef: WeakReference<FirebaseAuth>
        private val refRef: WeakReference<DatabaseReference>
        private val serviceRef: WeakReference<EmpushyNotificationService>
        private val featureManagerRef: WeakReference<FeatureManager>

        init {
            this.contextRef = WeakReference(context)
            this.activeRef = WeakReference(activeList)
            this.featureManagerRef = WeakReference(featureManager)
            this.authRef = WeakReference(authInstance)
            this.refRef = WeakReference(ref)
            this.serviceRef = WeakReference(service)
        }

        override fun onPreExecute() {}

        override fun doInBackground(vararg sbns: StatusBarNotification): String? {
            Log.d(TAG, "Notification posted background task.")
            val authInstance = authRef.get()
            if (authInstance != null) {
                val currentUser = authInstance.currentUser
                val context = contextRef.get()
                var activeList = activeRef.get()
                val ref = refRef.get()
                val service = serviceRef.get()
                val featureManager = featureManagerRef.get()

                val sbn = sbns[0]

                val notification = EmpushyNotification()
                try {
                    NotificationUtil.extractNotificationPostedValue(notification, sbn, context!!,
                            featureManager?.features?.filter { f -> f.category == FEATURE_CAT_NOTIFICATION }?: listOf())

                    val activeNotification = NotificationUtil.isInList(activeList, sbn.id, sbn.packageName,
                            sbn.notification.category)

                    // Check if notification should be cached or not
                    val notificationList = arrayListOf<EmpushyNotification>()
                    notificationList.add(activeNotification ?: notification)
                    val result = LearnUtils.deliverNotificationNow(notificationList, currentUser!!.uid)
                    var deliver = false
                    if(result.size>0)       
                        deliver = result.get(0)

                    if (activeNotification != null) {
                        Log.d(TAG, "Found notification "+activeNotification.appName + "; "+activeNotification.id)
                        Log.d(TAG, activeList.toString())
                        service!!.updateEmpushyNotification(activeList?: arrayListOf(), activeNotification, false)
                        activeList = ArrayList(NotificationUtil.removeNotificationById(activeList, activeNotification.id?:""))
                        ref?.child("notifications")?.child(currentUser.uid)?.child("mobile")?.child(activeNotification.id
                                ?: "none")?.removeValue()
                        Log.d(TAG, activeList.toString())
                    }
                    if (activeList != null) {
                        notification.hidden = !deliver
                        activeList.add(notification)
                        Log.d(TAG,"Here adding the notification.")
                        Log.d(TAG, activeList.toString())
                    }
                    ref!!.child("notifications").child(currentUser.uid).child("mobile").child(sbn.postTime.toString()).setValue(notification)
                    // update notification
                    // publishProgress(n) - on progress update
                    service!!.updateEmpushyNotification(activeList?: arrayListOf(), notification, true)


                } catch (e: Exception) {
                    Log.d(TAG, "Exception in notification posted background task: " + e.toString())
                }
            }
            return ""
        }

        override fun onProgressUpdate(vararg values: EmpushyNotification) {
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(n: String) {
            Log.d(TAG,"Post execute")
            Log.d(TAG, activeRef.get().toString())
        }

    }


    override fun onNotificationRemoved(sbn: StatusBarNotification) {

        Log.i(TAG, "Notification Removed. App: " + sbn.packageName)
        /*if (authInstance != null && !runningService) {
            val currentUser = authInstance!!.currentUser
            if (currentUser != null && sbn.packageName != applicationContext.packageName) {
                Log.i(TAG, "Notification Removed")

                val activeNotification = NotificationUtil.isInList(activeList, sbn.id, sbn.packageName)
                if (activeNotification != null) {
                    NotificationUtil.extractNotificationRemovedValue(activeNotification, sbn, applicationContext)
                    *//*if(placeCats.isNotEmpty())
                    activeNotification.placeCategories = placeCats.toList() as ArrayList<Int>*//*
                    ref!!.child("archive/notifications").child(currentUser.uid).child("mobile").child(activeNotification.id!!).setValue(activeNotification)
                    if (activeList != null)
                        activeList!!.remove(activeNotification)
                } else {
                    val cachedNotification = NotificationUtil.isInList(cachedList, sbn.id, sbn.packageName)
                    if (cachedNotification != null) {
                        NotificationUtil.extractNotificationRemovedValue(cachedNotification, sbn, applicationContext)
                        *//*if(placeCats.isNotEmpty())
                        cachedNotification.placeCategories = placeCats.toList() as ArrayList<Int>*//*
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
        }*/
    }

    fun updateEmpushyNotification(newList: ArrayList<EmpushyNotification>, notification: EmpushyNotification, add: Boolean){
        Log.d(TAG, "Updating notification")
        val appItems = DataUtils.notificationAnalysis(newList)
        val numHiddenItems = newList.filter { n -> n.hidden == true }.size
        // update the notification bar e.g. remove or update notification using empushyNotifyId
        // instead removing everything and pushing active list.
        updateStatusBarNotifications(newList, notification, add)
        //startNotificationService(appItems, numHiddenItems, true)
    }

    /**
     *
     */
    fun updateStatusBarNotifications(newList: ArrayList<EmpushyNotification>, notification: EmpushyNotification, add: Boolean){

        val mNotifyManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val GROUP_KEY_EMPUSHY = "eu.aempathy.EMPUSHY"

        if(add){
            // add notification to status bar
            // new notification
            val maxIdObject = mNotifyManager.activeNotifications.maxBy { it.id }
            if(maxIdObject!=null)
                notification.empushyNotifyId = maxIdObject.id + 1
            else
                notification.empushyNotifyId = EMPUSHY_NOTIFICATION_ID + 1

            activeList = ArrayList(NotificationUtil.updateEmPushyNotifyId(newList, notification))


            val icon = applicationContext.packageManager.getApplicationIcon(notification.app)
            val bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
            icon.draw(canvas)

            val newNotification = NotificationCompat.Builder(applicationContext, ANDROID_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_empushy_green)
                    .setLargeIcon(bitmap)
                    .setContentTitle(notification.appName) // App name
                    .setContentText(notification.ticker) // Text of notification
                    .setGroup(GROUP_KEY_EMPUSHY)
                    .setContentIntent(NotificationUtil.createNotificationOpenIntent(applicationContext,
                            notification.empushyNotifyId!!,
                            notification.id ?: "", notification.app ?: packageName))
                    .setDeleteIntent(NotificationUtil.createNotificationRemoveIntent(applicationContext,
                            notification.empushyNotifyId!!,
                            notification.id ?: ""))
                    .setStyle(NotificationCompat.BigTextStyle()
                            .bigText(NotificationUtil.extractUsefulText(notification)))
                    .build()

            NotificationManagerCompat.from(applicationContext).apply {
                if (notification.empushyNotifyId != null) {

                    Log.d(TAG, "Adding the notification!! "+notification.id+" "+notification.empushyNotifyId)
                    notify(notification.empushyNotifyId!!, newNotification)
                }
            }

        }
        else {
            // removing current notification
            Log.d(TAG, mNotifyManager.activeNotifications.size.toString())
            Log.d(TAG, "Removing the notification!! "+notification.id+" "+notification.empushyNotifyId)
            mNotifyManager.cancel(notification.empushyNotifyId?:-1)
            Log.d(TAG, mNotifyManager.activeNotifications.size.toString())
        }

        val notificationIntent = Intent(applicationContext, DetailActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val intent = PendingIntent.getActivity(applicationContext, EMPUSHY_NOTIFICATION_ID,
                notificationIntent, 0)

        val pre24SummaryNotification = NotificationCompat.Builder(applicationContext, ANDROID_CHANNEL_ID)
                .setContentTitle("Nothing for now.")
                //set content text to support devices running API level < 24
                .setContentText("Press to open EmPushy")
                .setSmallIcon(if(mNotifyManager.activeNotifications.size>1) R.mipmap.ic_empushy_green
                                else R.mipmap.ic_empushy)
                .setContentIntent(intent)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //build summary info into InboxStyle template
                .setStyle(NotificationCompat.InboxStyle()
                        .setSummaryText("running EmPushy"))
                //specify which group this notification belongs to
                .setGroup(GROUP_KEY_EMPUSHY)
                //set this notification as the summary for the group
                .setGroupSummary(true)
                .build()

        mNotifyManager.notify(EMPUSHY_NOTIFICATION_ID, pre24SummaryNotification)

    }

    private fun consolidateActiveList(userId: String){
        // get notifications from server
        val notificationsRef = ref?.child("notifications")?.child(userId)?.child("mobile")
        notificationsRef?.keepSynced(true)
        notificationsRef?.orderByKey()?.addListenerForSingleValueEvent(consolidateReadListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            runningRef?.removeEventListener(runningListener!!)
            removalActiveRef?.removeEventListener(removalActiveListener!!)
            removalCacheRef?.removeEventListener(removalCacheListener!!)
            featureRef?.removeEventListener(featureListener!!)
        }catch(e:Exception){}
        Log.d(TAG, "Destroying listeners "+applicationContext.packageName)
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
                        //activeList?.remove(notification)
                        updateEmpushyNotification(activeList!!,notification, false )
                }
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private fun subscribeToRunning(){
        runningRef = ref?.child("users")?.child(authInstance?.currentUser?.uid?:"none")?.child("running")
        runningListener = runningRef?.addChildEventListener(runningReadListener)
    }

    var runningReadListener: ChildEventListener = object : ChildEventListener {

        override fun onChildRemoved(p0: DataSnapshot) {
            if(StateUtils.isNetworkAvailable(applicationContext) && authInstance!=null) {
                val currentUser = authInstance?.currentUser
                if (currentUser != null) {
                    Log.d(TAG, "Signing out!")
                    authInstance?.signOut()
                    stopService()
                }
            }
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {}

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private fun stopService(){
        runningService = false
        stopForeground(true)
        stopSelf()
    }
}