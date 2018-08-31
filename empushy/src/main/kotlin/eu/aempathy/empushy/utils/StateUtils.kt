package eu.aempathy.empushy.utils

import android.app.AppOpsManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Context.ACTIVITY_SERVICE
import android.app.ActivityManager
import eu.aempathy.empushy.activities.AuthActivity
import android.content.Context.ACTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkInfo


/**
 * Created by Kieran on 05/04/2018.
 */
class StateUtils {

    companion object {

        private val TAG = StateUtils::class.java.simpleName
        /**
         * Returns whether or not the notification listener service has
         * been granted access to the device notifications by the user.
         */
        fun isNotificationListenerGranted(contentResolver: ContentResolver?, packageName: String): Boolean {
            if(contentResolver!=null){
                val listeners = Settings.Secure.getString(contentResolver,
                        "enabled_notification_listeners")
                if (listeners != null) {
                    Log.d(TAG, "Listeners are : " + listeners.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    return if (listeners.toString().contains(packageName))
                        true
                    else
                        false
                } else
                    return false
            }
            return false
        }

        fun isUsagePermissionGranted(context: Context?):Boolean{
            if(context!=null) {
                try {
                    val packageManager = context.packageManager
                    val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
                    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
                    return mode == AppOpsManager.MODE_ALLOWED
                } catch (e: PackageManager.NameNotFoundException) {
                    return false
                }
            }
            return false

        }

        fun isNamedProcessRunning(context: Context, processName: String): Boolean {
            Log.d(TAG, "Getting running processes")
            /*val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val processes = manager.runningAppProcesses
            for (process in processes) {
                Log.d(TAG, "Process name: "+process.processName)
                if (processName == process.processName) {
                    return true
                }
            }
            return false*/
            val actvityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val procInfos = actvityManager.runningAppProcesses
            for (runningProInfo in procInfos) {
                Log.d("Running Processes", "()()" + runningProInfo.processName)
            }
            return false
        }

        fun isNetworkAvailable(context:Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            return if (connectivityManager is ConnectivityManager) {
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected ?: false
            } else false
        }
    }
}