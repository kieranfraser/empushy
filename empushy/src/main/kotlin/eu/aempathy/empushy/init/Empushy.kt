package eu.aempathy.empushy.init

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

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

        /*val service = Intent(activity, EmpushyNotificationService::class.java)
        service.action = Constants.ACTION.STARTFOREGROUND_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.startForegroundService(service)
        else
            activity.startService(service)*/
    }

    fun initEmpushyApp(context: Context): FirebaseApp {
        try {
            val app = FirebaseApp.getInstance("empushy")
            return app
        } catch (e: IllegalStateException) {
            val options = FirebaseOptions.Builder()
                    .setApplicationId("1:410171203943:android:ee551da2ec169d6a") // Required for Analytics.
                    .setApiKey("AIzaSyBhtL8Zdn3dDfsHrGLOK2S3VXwQ8QPDX78") // Required for Auth.
                    .setDatabaseUrl("https://empushy-9151a.firebaseio.com/")
                    .build()
            // Initialize with secondary app.
            val app = FirebaseApp.initializeApp(context, options, "empushy")
            FirebaseDatabase.getInstance(app).setPersistenceEnabled(true);
            return app
        }

    }

    fun loggedIn(context: Context): Boolean{
        try {
            val app = initEmpushyApp(context)
            if( FirebaseAuth.getInstance(app).currentUser != null)
                return true
        } catch(e: Exception){}
        return false
    }
}
