package eu.aempathy.empushy.utils

import android.app.AppOpsManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log


/**
 * Created by Kieran on 05/04/2018.
 */
class StateUtils {

    companion object {

        val TAG = javaClass.simpleName;

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
    }
}