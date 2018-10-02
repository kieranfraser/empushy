package eu.aempathy.empushy.activities

import android.os.AsyncTask
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.CheckBox
import android.widget.ExpandableListView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.adapters.FeatureExpandableListAdapter
import eu.aempathy.empushy.adapters.NotificationSummaryAdapter
import eu.aempathy.empushy.data.*
import eu.aempathy.empushy.data.FeatureManager.Companion.COMMON_FEATURE_PATH
import eu.aempathy.empushy.data.FeatureManager.Companion.FEATURE_PATH
import eu.aempathy.empushy.data.FeatureManager.Companion.USER_PATH
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import eu.aempathy.empushy.utils.LearnUtils
import kotlinx.android.synthetic.main.activity_settings.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.HashMap

/**
 * SettingsActivity
 * -
 */
class SettingsActivity : AppCompatActivity() {

    val TAG = EMPUSHY_TAG+SettingsActivity::class.java.simpleName
    val ARCHIVE_NOTIFICATION_PATH = "archive/notifications"
    val ARCHIVE_MOBILE_PATH = "mobile"

    var ref: DatabaseReference ?=null
    var featureManager: FeatureManager ?= null
    var authInstance: FirebaseAuth ?= null
    var featureRef: DatabaseReference ?= null

    var expandableListView: ExpandableListView ?= null
    var adapter: FeatureExpandableListAdapter ?= null

    var featureData: HashMap<String, List<Feature>> ?= null
    var titleList: List<String> ?= null
    var expandedCats: ArrayList<Int> ?=null

    var archiveNotifications: ArrayList<EmpushyNotification> ?= arrayListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_settings)

        try {
            val firebaseApp = Empushy.initialise(applicationContext)
            authInstance = FirebaseAuth.getInstance(firebaseApp)
            ref = FirebaseDatabase.getInstance(firebaseApp).reference

            featureManager = FeatureManager(arrayListOf(), listOf(), ref!!, authInstance?.currentUser?.uid!!)
            expandableListView = findViewById(R.id.elv_features)
            expandedCats = arrayListOf()

            rv_settings_simulate.layoutManager = LinearLayoutManager(applicationContext)
            rv_settings_simulate.hasFixedSize()
            rv_settings_simulate.adapter = NotificationSummaryAdapter(mutableListOf(), {n->{}}, true)
            // Get features.. listener
            getFeatures()
            setUpUI()
            getArchiveNotifications(authInstance?.currentUser?.uid!!)

        } catch(e:Exception){finish()}

    }

    private fun setUpUI(){
        featureData = HashMap<String, List<Feature>>()

        titleList = featureManager?.categories
        for(cat in titleList?: listOf()) {
            featureData?.put(cat, featureManager?.features?.filter { feature -> feature.category == cat }?: listOf())
        }
        if (expandableListView != null) {
            adapter = FeatureExpandableListAdapter(this, titleList?: listOf(), featureData?: hashMapOf(),
                    {view: View -> onClickListener(view) })
            expandableListView!!.setAdapter(adapter)

            expandableListView!!.setOnGroupExpandListener {
                groupPosition ->
                expandedCats?.add(groupPosition)
            }


            expandableListView!!.setOnGroupCollapseListener {
                groupPosition ->
                val toRemove = expandedCats?.filter { i -> i == groupPosition }
                for(i in toRemove?: listOf()){
                    expandedCats?.remove(i)
                }
            }

            for(cat in expandedCats?.toMutableList()?: arrayListOf()){
                expandableListView?.expandGroup(cat)
            }
        }

    }

    private fun onClickListener(view: View){
        Log.d(TAG, "Clicked")
        val featureId = view.tag as String
        featureManager?.updateFeatureEnabled(applicationContext, featureId, (view as CheckBox).isChecked)
        adapter?.notifyDataSetChanged()
        getArchiveNotifications(authInstance?.currentUser?.uid?:"")


    }

    /**
     * Call to get user features
     *  - set FeatureManager features if not empty
     *  - if empty call getCommonFeatures() e.g. for new user
     *  - keepSynced
     */
    private fun getFeatures(){
        try {
            featureRef = ref?.child(USER_PATH)?.child(authInstance!!.currentUser!!.uid)?.child(FEATURE_PATH)
            featureRef?.addListenerForSingleValueEvent(readListener)
        } catch(e: Exception){Log.d(TAG, "Unable to get features.")}
    }

    var readListener: ValueEventListener = object : ValueEventListener  {

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
                setUpUI()
            }
            else{
                getCommonFeatures()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Call to get common features
     *  - calls featureManager update() to update user in FB
     *  - oneTimeCall
     */
    private fun getCommonFeatures(){
        try {
            ref?.child(COMMON_FEATURE_PATH)?.addListenerForSingleValueEvent(commonReadListener)
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
                setUpUI()

            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    private fun getArchiveNotifications(userId: String){
        archiveNotifications?.clear()
        try {
            ref?.child(ARCHIVE_NOTIFICATION_PATH)?.child(userId)?.child(ARCHIVE_MOBILE_PATH)
                    ?.orderByKey()
                    ?.limitToLast(10)
                    ?.addListenerForSingleValueEvent(archiveReadListener)
        } catch(e: Exception){Log.d(TAG, "Unable to get archived notifications.")}
    }

    var archiveReadListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(snapshot.hasChildren()) {
                for(child in snapshot.children) {
                    val notification = child?.getValue(EmpushyNotification::class.java)
                    if (notification != null) {
                        archiveNotifications?.add(notification)
                    }
                }
                // send to learn in background task
                simulate(archiveNotifications?: arrayListOf(), authInstance?.currentUser?.uid?:"")
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    fun simulate(notifications: ArrayList<EmpushyNotification>, userId: String){
        // strip unselected features from notifications
        // loop through feature values, if feature disabled,
        // get feature name - get corresponding notification property
        // change property to default
        for(n in notifications){
            var feature = featureManager?.features?.filter { f -> f.name == "app"}
            if(feature!=null && feature.isNotEmpty() && !feature[0].enabled!!){
                n.app = NOTIFICATION_STRING_DEFAULT
                n.appName = NOTIFICATION_STRING_DEFAULT
            }

            feature = featureManager?.features?.filter { f -> f.name == "text"}
            if(feature!=null && feature.isNotEmpty() && !feature[0].enabled!!){
                n.ticker = NOTIFICATION_STRING_DEFAULT
                n.subText = NOTIFICATION_STRING_DEFAULT
                n.infoText = NOTIFICATION_STRING_DEFAULT
                n.summaryText = NOTIFICATION_STRING_DEFAULT
                n.extra = NOTIFICATION_STRING_DEFAULT
                n.extraText = NOTIFICATION_STRING_DEFAULT
                n.extraTextLines = NOTIFICATION_STRING_DEFAULT
                n.extraBigText = NOTIFICATION_STRING_DEFAULT
                n.extraTitle = NOTIFICATION_STRING_DEFAULT
                n.extraTitleBig = NOTIFICATION_STRING_DEFAULT
            }

            feature = featureManager?.features?.filter { f -> f.name == "category"}
            if(feature!=null && feature.isNotEmpty() && !feature[0].enabled!!){
                n.category = NOTIFICATION_STRING_DEFAULT
            }

            feature = featureManager?.features?.filter { f -> f.name == "time"}
            if(feature!=null && feature.isNotEmpty() && !feature.get(0).enabled!!){
                n.time = NOTIFICATION_LONG_DEFAULT
                n.removedTime = NOTIFICATION_LONG_DEFAULT
            }
        }
        val taskPosted = SettingsSimulateTask(userId, rv_settings_simulate)
        for(n in notifications){
            Log.d(TAG, n.appName)
        }
        taskPosted.execute(notifications)
    }

    private class SettingsSimulateTask(userId: String,
                                       textView: RecyclerView) : AsyncTask<List<EmpushyNotification>, String, MutableList<EmpushyNotification>>() {

        private val TAG = EMPUSHY_TAG + SettingsSimulateTask::class.java.simpleName

        private val userIdRef: WeakReference<String>
        private val rvRef: WeakReference<RecyclerView>

        init {
            this.userIdRef = WeakReference(userId)
            this.rvRef = WeakReference(textView)
        }

        override fun onPreExecute() {
            rvRef.get()?.visibility = View.GONE
        }

        override fun doInBackground(vararg list: List<EmpushyNotification>): MutableList<EmpushyNotification>? {
            Log.d(TAG, "Settings simulate background task.")
            val notifications = list[0]

            // send notifications for simulation
            val results = LearnUtils.deliverNotificationNow(notifications, userIdRef.get()?: "")

            val newNotifications = notifications.toMutableList()

            var i = 0
            if(results.size == newNotifications.size)
                for(n in newNotifications){
                    n.hidden = !results[i]
                    i++
                }

            return newNotifications
        }

        override fun onPostExecute(notifications: MutableList<EmpushyNotification>) {
            val rv = rvRef.get()
            rv?.visibility = View.VISIBLE
            rv?.adapter = NotificationSummaryAdapter(notifications,
                    {n -> {}},
                    true)
        }

    }

}
