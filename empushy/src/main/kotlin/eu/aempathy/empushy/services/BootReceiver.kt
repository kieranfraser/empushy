package eu.aempathy.empushy.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.utils.Constants
import eu.aempathy.empushy.utils.NotificationUtil

/**
 * Service called when device is booted up.
 * - Initialises the EmPushy library.
 * - Checks user EmPushy auth status.
 * - If logged-in and app is designated to run notification, starts EmpushyNotificationService
 */
class BootReceiver: BroadcastReceiver() {

    private val TAG = BootReceiver::class.java.simpleName
    private var context: Context ?= null

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "EmPushy Boot.")
        this.context = context
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Empushy.initialise(context)
            val firebaseApp = FirebaseApp.getInstance("empushy")
            val authInstance = FirebaseAuth.getInstance(firebaseApp!!)
            val ref = FirebaseDatabase.getInstance(firebaseApp).reference
            if (authInstance.currentUser != null) {
                val checkRunningRef = ref.child("users")
                        .child(authInstance?.currentUser?.uid?:"none")
                        .child("running")
                        .child(NotificationUtil.simplePackageName(context, context.packageName))
                checkRunningRef.keepSynced(true)
                checkRunningRef.addListenerForSingleValueEvent(runningReadListenerSingle)

            } else {
                Log.d(TAG, "User logged out - not starting services on boot.")
            }
        }
    }

    var runningReadListenerSingle: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG, "Checking EmPushy to run.")
            try {
                if (snapshot.key == NotificationUtil.simplePackageName(context!!, context!!.packageName)) {
                    Log.d(TAG, "Starting EmPushy via "+snapshot.key)
                    val myService = Intent(context, EmpushyNotificationService::class.java)
                    myService.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context?.startForegroundService(myService)
                    } else {
                        context?.startService(myService)
                    }
                }
            }catch(e:Exception){Log.d(TAG, "Exception checking EmPushy running in boot.")}
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    /*private fun getInstalledApps(context: Context) {
        val handlerThread = HandlerThread("GetInstalledApps")
        handlerThread.start() // you must first start the handler thread before using it

        val handler = Handler(handlerThread.looper)

        handler.post({
            val id = FirebaseAuth.getInstance().currentUser?.uid
            if(!id.isNullOrEmpty()) {
                val apps = DataUtils.getInstalledApps(context)
                val temp = ArrayList<App>()
                for(s in apps){
                    val app = App(s, NotificationUtils.simplePackageName(context, s))
                    temp.add(app)
                }
                DataUtils.updateInstalledApps(temp, id)
                handlerThread.quit() // will this break it?
            }
        })
    }*/


}