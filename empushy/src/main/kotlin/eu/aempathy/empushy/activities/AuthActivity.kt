package eu.aempathy.empushy.activities

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import eu.aempathy.empushy.services.EmpushyNotificationService
import eu.aempathy.empushy.utils.Constants
import eu.aempathy.empushy.utils.NotificationUtil
import eu.aempathy.empushy.utils.StateUtils
import java.util.*

/**
 * AuthActivity
 * - activity launched when toggle button checked.
 * - used to handle logging in the user and starting the
 *      notification listener service.
 * - handles concurrent library logins and ensures single
 *      foreground service running.
 * - handles consent for notification listener service and
 *      app stats.
 */
class AuthActivity : AppCompatActivity() {

    private val TAG = EMPUSHY_TAG + AuthActivity::class.java.simpleName

    private var firebaseApp: FirebaseApp ?= null
    private var runningRef: DatabaseReference ?= null
    private var providers: List<AuthUI.IdpConfig> = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build())

    /**
     * Setup buttons
     * Check status of permissions
     * Check status of user auth
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_auth)

        val btAccept = findViewById(R.id.bt_empushy_consent_accept) as Button
        val btCancel = findViewById(R.id.bt_empushy_consent_cancel) as Button
        btCancel.setOnClickListener({
            val intent = Intent();
            setResult(RESULT_CANCELED, intent);
            finish()
        })

        val listenerGranted = StateUtils.isNotificationListenerGranted(applicationContext?.contentResolver, packageName)
        val usageGranted = StateUtils.isUsagePermissionGranted(applicationContext)
        if (listenerGranted && usageGranted) {
            firebaseApp = Empushy.initialise(applicationContext)
            if (applicationContext != null) {
                btAccept.text = "Login or Sign up"
                btAccept.setOnClickListener({
                    startActivityForResult(
                            AuthUI.getInstance(firebaseApp!!)
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .build(),
                            Empushy.RC_SIGN_IN)
                })
                try {
                    startActivityForResult(
                            AuthUI.getInstance(firebaseApp!!)
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .build(),
                            Empushy.RC_SIGN_IN)
                }catch(e:Exception){
                    Log.d(TAG, "Exception signing in.")
                }
            }
        } else {
            btAccept.setOnClickListener({ consentGranted(listenerGranted) })
        }
    }

    /**
     * Start settings activity for user permissions if not granted already:
     * 1. NotificationListener
     * 2. UsageStats
     */
    private fun consentGranted(listenerGranted:Boolean){
        if(!listenerGranted){
            Toast.makeText(applicationContext, "Please enable Notification Listener.", Toast.LENGTH_LONG).show()
            startActivityForResult(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 121)
        }
        else{
            val usageIntent = Intent()
            usageIntent.setAction(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivityForResult(usageIntent, 121)
        }
    }

    /**
     * On all permissions granted, start sign in process.
     */
    private fun permissionGranted(){
        Toast.makeText(applicationContext, "Permissions Granted.", Toast.LENGTH_LONG).show()
        firebaseApp = Empushy.initialise(applicationContext)
        if(applicationContext!=null) {
            try {
                startActivityForResult(
                        AuthUI.getInstance(firebaseApp!!)
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        Empushy.RC_SIGN_IN)
            }catch(e:Exception){}
        }
    }

    /**
     * Catches result of permission activities (granted or not)
     * and result of authentication (sign in success or failure)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 121) run {

            val listenerGranted = StateUtils.isNotificationListenerGranted(applicationContext?.contentResolver, packageName)
            val usageGranted = StateUtils.isUsagePermissionGranted(applicationContext)

            if(listenerGranted && usageGranted){
                permissionGranted()
            }
            else{
                Toast.makeText(applicationContext, "Permissions still to be granted before proceeding.", Toast.LENGTH_LONG).show()
                if(!listenerGranted){
                    Toast.makeText(applicationContext, "Please enable Notification Listener.", Toast.LENGTH_LONG).show()
                    startActivityForResult(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 121)
                }
                else{
                    val usageIntent = Intent()
                    usageIntent.setAction(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivityForResult(usageIntent, 121)
                }
            }
        }
        if(requestCode == Empushy.RC_SIGN_IN && resultCode == Activity.RESULT_OK){
            signInSuccess()
        }
    }

    /**
     * On successful sign-in (only executed when all permissions granted)
     * check whether library in use for this user already
     * - if so, don't start foreground notification, but keep user logged in.
     * - if not, start foreground notification
     */
    fun signInSuccess(){
        Log.d(TAG, "Sign in success.")
        try {

            val authInstance = FirebaseAuth.getInstance(firebaseApp!!)
            val ref = FirebaseDatabase.getInstance(firebaseApp!!).reference

            runningRef = ref.child("users").child(authInstance?.currentUser?.uid
                    ?: "none").child("running")
            runningRef?.keepSynced(true)
            runningRef?.addListenerForSingleValueEvent(runningReadListener)
        } catch(e:Exception){

            Log.d(TAG, "Exception starting service")
        }


    }

    /**
     * Query Firebase database to check if user has no other application
     * running the EmPushy service.
     */
    var runningReadListener: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.childrenCount<1){
                Log.d(TAG, "Less than one.. starting service.")
                val myService = Intent(applicationContext, EmpushyNotificationService::class.java)
                myService.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(myService)
                } else {
                    startService(myService)
                }
                runningRef?.child(NotificationUtil.simplePackageName(applicationContext, applicationContext.packageName))
                        ?.setValue(true)
                        ?.addOnCompleteListener {
                            val intent = Intent();
                            setResult(RESULT_OK, intent);
                            finish()
                        }
            }
            else {
                Log.d(TAG, "NOT starting service, more than one.")
                val myService = Intent(applicationContext, EmpushyNotificationService::class.java)
                myService.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                startService(myService)
                stopService(myService)
                val intent = Intent();
                setResult(RESULT_CANCELED, intent);
                finish()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }
}
