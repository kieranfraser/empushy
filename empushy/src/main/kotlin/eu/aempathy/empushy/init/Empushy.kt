package eu.aempathy.empushy.init

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

    val EMPUSHY_TAG = "EMPUSHY_LIB: "
    private val TAG = EMPUSHY_TAG + Empushy::class.java.simpleName
    val RC_SIGN_IN = 21192

    /**
     * Initialise the EmPushy library
     * - setup Firebase app instances using secondary (EmPushy) app
     * so no interference with other FB instances.
     */
    fun initialise(context: Context): FirebaseApp {
        Log.d(TAG, "Initialising EmPushy")
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

    /**
     * Returns user current EmPushy auth state.
     */
    fun loggedIn(context: Context): Boolean{
        try {
            val app = initialise(context)
            if( FirebaseAuth.getInstance(app).currentUser != null)
                return true
        } catch(e: Exception){}
        return false
    }
}
