package eu.aempathy.empushy.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Window
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.adapters.HomeAdapter
import eu.aempathy.empushy.adapters.SwipeToDeleteCallback
import eu.aempathy.empushy.data.AppSummaryItem
import eu.aempathy.empushy.data.EmpushyNotification
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.services.EmpushyNotificationService
import eu.aempathy.empushy.utils.Constants
import eu.aempathy.empushy.utils.DataUtils
import kotlinx.android.synthetic.main.activity_detail.*
import java.util.*

/**
 * DetailActivity
 * - activity opened when the user clicks on the EmPushy notification
 * - displays the active notifications in summary format
 * - displays a graph of the previous 24 hours of notification engagement
 */
class DetailActivity : AppCompatActivity() {
    val TAG = "MyDetailActivity"

    var ref: DatabaseReference ?=null
    var selectedId: String ?= ""

    var notificationsRef: DatabaseReference ?= null
    var mListener: ValueEventListener?= null
    var notifications = mutableListOf<EmpushyNotification>()

    var cachedNotificationsRef: DatabaseReference ?= null

    var archivedNotificationsRef: DatabaseReference ?= null
    var archivedListener: ValueEventListener ?= null
    var archNotifications = mutableListOf<EmpushyNotification>()

    var appSummaryItems = ArrayList<AppSummaryItem>()

    var lcHome: LineChart ?= null

    companion object {
        const val NOTIFICATIONS: String = "notifications"
        const val ARCHIVE: String = "archive"
        const val CACHED: String = "cached"
        const val MOBILE: String = "mobile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_detail)

        try {

            val firebaseApp = Empushy.initialise(applicationContext)
            val authInstance = FirebaseAuth.getInstance(firebaseApp)
            ref = FirebaseDatabase.getInstance(firebaseApp).reference
            selectedId = authInstance.currentUser?.uid

        } catch(e:Exception){finish()}

        lcHome = lc_home
        setUpNotifications()
    }

    fun selector(n: EmpushyNotification): Long = n.time?:0

    private fun setUpNotifications(){

        /*val gridAdapter = AppSummaryAdapter(this, appSummaryItems, { a : AppSummaryItem-> longClickAppSummary(a) })
        val gridView = findViewById<GridView>(R.id.gv_home)
        gridView?.adapter = gridAdapter;*/
        gv_home.layoutManager = GridLayoutManager(this, 2)
        val adapter = HomeAdapter(appSummaryItems, { a : AppSummaryItem-> longClickAppSummary(a) })
        gv_home.adapter = adapter

        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                val appItem = appSummaryItems[viewHolder!!.adapterPosition]
                removeAppSummaryItem(appItem)
                val myService = Intent(applicationContext, EmpushyNotificationService::class.java)
                myService.putExtra("notification", appItem)
                myService.setAction(Constants.ACTION.REMOVAL_ACTION);
                startService(myService)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(gv_home)

        ib_home_refresh.setOnClickListener({refresh()})
        ib_home_settings.setOnClickListener({settings()})

        getNotifications(selectedId)
    }

    private fun removeAppSummaryItem(a: AppSummaryItem){
        for(n in a.active?:ArrayList()){
            ref?.child("notifications")?.child(selectedId?:"none")?.child(MOBILE)?.child(n.id?:"none")?.removeValue()
        }
        for(n in a.hidden?:ArrayList()){
            ref?.child("cached/notifications")?.child(selectedId?:"none")?.child(MOBILE)?.child(n.id?:"none")?.removeValue()
        }
        getNotifications(selectedId)
    }

    private fun longClickAppSummary(a : AppSummaryItem): Boolean {

        val builder = AlertDialog.Builder(this);
        builder.setTitle("Remove app notifications?");

        builder.setPositiveButton("Remove active"){dialog, which ->
            for(n in a.active?:ArrayList()){
                ref?.child("notifications")?.child(selectedId?:"none")?.child(MOBILE)?.child(n.id?:"none")?.removeValue()
            }
            getNotifications(selectedId)
            Toast.makeText(applicationContext, "Removed active notifications from "+a.appName, Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("Remove hidden"){dialog, which ->
            for(n in a.hidden?:ArrayList()){
                ref?.child("cached/notifications")?.child(selectedId?:"none")?.child(MOBILE)?.child(n.id?:"none")?.removeValue()
            }
            getNotifications(selectedId)
            Toast.makeText(applicationContext, "Removed hidden notifications from "+a.appName, Toast.LENGTH_LONG).show()
        }
        builder.setNeutralButton("Cancel"){dialog,which ->
            dialog.cancel()
        }
        builder.show();

        return true
    }

    private fun getNotifications(id: String?){
        if(id!=null && !id.isEmpty()){
            appSummaryItems.clear();
            notifications.clear();
            archNotifications.clear();

            // active notifications
            notificationsRef = ref?.child(NOTIFICATIONS)?.child(id)?.child(MOBILE)
            notificationsRef?.orderByKey()?.addListenerForSingleValueEvent(readListener)

            // archived notifications
            archivedNotificationsRef = ref?.child(ARCHIVE)?.child(NOTIFICATIONS)?.child(id)?.child(MOBILE)
            archivedListener = archivedNotificationsRef?.orderByKey()?.limitToLast(100)?.addValueEventListener(readArchiveListener)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            archivedNotificationsRef?.removeEventListener(archivedListener!!)
            notificationsRef?.removeEventListener(mListener!!)
        } catch(e:Exception){}
    }

    // Listener for active notifications
    var readListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {
            appSummaryItems.clear();
            notifications.clear();
            val newNotifications = ArrayList<EmpushyNotification>();
            for(child in snapshot.children){
                val notification = child?.getValue(EmpushyNotification::class.java)
                if(notification!=null) {
                    newNotifications.add(notification)
                }
            }
            notifications.addAll(newNotifications);
            notifications.sortByDescending { selector(it) }

            tv_home_need_attention.text = notifications.filter{n -> n.hidden==false}.size.toString()
            tv_home_for_later.text = notifications.filter{n -> n.hidden==true}.size.toString()
            gv_home.adapter.notifyDataSetChanged()


            // cached notifications - selectedId may have changed at this point?
            /*cachedNotificationsRef = ref?.child(CACHED)?.child(NOTIFICATIONS)?.child(selectedId?:"none")?.child(MOBILE)
            cachedNotificationsRef!!.orderByKey().addListenerForSingleValueEvent(readCacheListener)*/

            notificationAnalysis()
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    // Listener for cached notifications called after active notifications
    /*var readCacheListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {
            tv_home_for_later.text = snapshot.childrenCount.toString()
            val newNotifications = ArrayList<EmpushyNotification>();
            for(child in snapshot.children){
                val notification = child?.getValue(EmpushyNotification::class.java)
                notification?.hidden = true
                if(notification!=null) {
                    newNotifications.add(notification)
                }
            }
            notifications.addAll(newNotifications);
            notifications.sortByDescending { selector(it) }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.d(TAG, "error")}
    }*/

    // Listener for archived notifications
    var readArchiveListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {
            archNotifications.clear();

            val newNotifications = ArrayList<EmpushyNotification>();
            for(child in snapshot.children){
                val notification = child?.getValue(EmpushyNotification::class.java)
                if(notification!=null) {
                    newNotifications.add(notification)
                }
            }
            archNotifications.addAll(newNotifications);
            archNotifications.sortByDescending { selector(it) }
            setHomeData()
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }


    /**
     * Analyse notifications list and split as necessary
     * e.g. split into separate app category.
     */
    private fun notificationAnalysis(){

        Log.d(TAG, "Notification analysis")
        // separate into app
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

            Log.d(TAG, "Notification size: "+appSummaryItems.size)

        }
        gv_home.adapter.notifyDataSetChanged()
    }


    private fun setHomeData(){

        val calendar = Calendar.getInstance()
        val currentTime = calendar.get(Calendar.HOUR_OF_DAY)
        calendar.add(Calendar.HOUR_OF_DAY, -23)

        val totalValues = ArrayList<Entry>()
        val acceptedValues = ArrayList<Entry>()
        val dismissedValues = ArrayList<Entry>()

        for (i in 0 until 24) {

            val data = DataUtils.filterNotificationsByHour(archNotifications, calendar)

            totalValues.add(Entry(i.toFloat(),
                    data[0]))
            acceptedValues.add(Entry(i.toFloat(),
                    data[1]))
            dismissedValues.add(Entry(i.toFloat(),
                    data[2]))
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }

        var max = 0
        for(entry in totalValues)
            if(entry.y >= max)
                max = entry.y.toInt()
        Log.d(TAG, "Max: "+max)

        val set2: LineDataSet
        val set3: LineDataSet

        if (lcHome?.getData() != null && lcHome?.getData()!!.dataSetCount > 0) {
            set2 = lcHome?.getData()?.getDataSetByIndex(0) as LineDataSet
            set2.values = dismissedValues

            set3 = lcHome?.getData()?.getDataSetByIndex(1) as LineDataSet
            set3.values = acceptedValues

            lcHome?.getData()?.notifyDataChanged()
            lcHome?.notifyDataSetChanged()
            lcHome?.axisLeft?.axisMaximum = (max * 1.5).toFloat()
            lcHome?.fitScreen()
            lcHome?.invalidate()

        } else {

            // create a dataset and give it a type
            set2 = LineDataSet(dismissedValues, "# dismissed during hour")
            set2.mode = LineDataSet.Mode.CUBIC_BEZIER

            set2.setDrawIcons(false)
            set2.setDrawValues(false);

            set2.color = Color.RED
            set2.setDrawCircles(false)
            set2.lineWidth = 1f
            set2.setDrawCircleHole(false)
            set2.valueTextSize = 9f
            set2.setDrawFilled(true)
            set2.fillColor = Color.RED


            // create a dataset and give it a type
            set3 = LineDataSet(acceptedValues, "# opened during hour")
            set3.mode = LineDataSet.Mode.CUBIC_BEZIER

            set3.setDrawIcons(false)
            set3.setDrawValues(false);

            set3.color = Color.GREEN
            set3.setDrawCircles(false)
            set3.lineWidth = 1f
            set3.setDrawCircleHole(false)
            set3.valueTextSize = 9f
            set3.setDrawFilled(true)
            set3.fillColor = Color.GREEN

            val dataSets = ArrayList<ILineDataSet>()
            //dataSets.add(set1) // add the datasets
            dataSets.add(set2)
            dataSets.add(set3)


            // create a data object with the datasets
            val data = LineData(dataSets)

            // set data
            lcHome?.setData(data)
            lcHome?.axisLeft?.axisMaximum = (max * 1.1).toFloat()

            lcHome?.xAxis?.axisLineColor = Color.WHITE
            lcHome?.axisLeft?.setDrawAxisLine(true)
            lcHome?.axisLeft?.axisLineColor = Color.WHITE
            lcHome?.axisLeft?.textColor = Color.WHITE
            lcHome?.xAxis?.axisLineColor = Color.WHITE
            lcHome?.xAxis?.textColor = Color.WHITE
            lcHome?.xAxis?.position = XAxis.XAxisPosition.BOTTOM
            lcHome?.axisLeft?.setDrawAxisLine(false)
            lcHome?.xAxis?.setDrawAxisLine(false)

            lcHome?.axisLeft?.granularity = 4f

            // UI changes
            lcHome?.xAxis?.setDrawGridLines(false);
            lcHome?.axisLeft?.setDrawGridLines(false);
            lcHome?.axisRight?.setDrawGridLines(false)
            lcHome?.axisRight?.isEnabled = false;
            lcHome?.description?.text = "";
            lcHome?.legend?.isEnabled = true;
            lcHome?.setExtraOffsets(5f, 5f, 5f,5f)
            lcHome?.setTouchEnabled(false)
            lcHome?.legend?.textColor = Color.WHITE


            val ll1 = LimitLine(currentTime.toFloat(), "Now");
            ll1.setLineWidth(4f);
            ll1.textColor = Color.WHITE
            ll1.enableDashedLine(10f, 10f, 0f);
            ll1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll1.setTextSize(10f);
            lcHome?.xAxis?.addLimitLine(ll1);

            lcHome?.fitScreen()
            lcHome?.invalidate()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    fun refresh() {
        getNotifications(selectedId)
    }

    fun settings(){
        val settingsIntent = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }
}
