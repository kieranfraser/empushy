package eu.aempathy.empushy.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import eu.aempathy.empushy.init.Empushy

/**
 * Created by Kieran on 09/08/2018.
 */
class BootReceiver: BroadcastReceiver() {

    private val TAG = BootReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Heedful Boot.")
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Empushy.initEmpushyApp(context)
            val firebaseApp = FirebaseApp.getInstance("empushy")
            val authInstance = FirebaseAuth.getInstance(firebaseApp!!)
            if (authInstance.currentUser != null) {
                Log.d(TAG, "User logged in - starting notification listener on boot.")

                val myService = Intent(context, EmpushyNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(myService)
                } else {
                    context.startService(myService)
                }
            } else {
                Log.d(TAG, "User logged out - not starting services on boot.")
            }
        }
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