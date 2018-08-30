package eu.aempathy.empushy.init

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import eu.aempathy.empushy.services.EmpushyNotificationService
import eu.aempathy.empushy.utils.Constants

/**
 * Created by Kieran on 07/06/2018.
 */

object Empushy {

    private val TAG = Empushy::class.java.simpleName

    val RC_SIGN_IN = 21192

    fun initialise(activity: Activity) {
        Log.d(TAG, "Initialised library")
        /*Intent mServiceIntent = new Intent(activity, CempyNotificationService.class);
        activity.startService(mServiceIntent);*/
        initEmpushyApp(activity)
        /*FirebaseApp firebaseApp = FirebaseApp.getInstance("empushy");
        // Create and launch sign-in intent
        activity.startActivityForResult(
                AuthUI.getInstance(firebaseApp)
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);*/

        val service = Intent(activity, EmpushyNotificationService::class.java)
        service.action = Constants.ACTION.STARTFOREGROUND_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.startForegroundService(service)
        else
            activity.startService(service)
    }

    fun initEmpushyApp(context: Context) {
        try {
            FirebaseApp.getInstance("empushy")
        } catch (e: IllegalStateException) {
            val options = FirebaseOptions.Builder()
                    .setApplicationId("1:410171203943:android:ee551da2ec169d6a") // Required for Analytics.
                    .setApiKey("AIzaSyBhtL8Zdn3dDfsHrGLOK2S3VXwQ8QPDX78") // Required for Auth.
                    .setDatabaseUrl("https://empushy-9151a.firebaseio.com/")
                    .build()
            // Initialize with secondary app.
            FirebaseApp.initializeApp(context, options, "empushy")
        }

    }
}
