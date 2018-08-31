package eu.aempathy.empushy.activities

import android.app.Activity
import android.content.Context
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
import eu.aempathy.empushy.services.EmpushyNotificationService
import eu.aempathy.empushy.utils.NotificationUtil
import eu.aempathy.empushy.utils.StateUtils
import java.util.*

class AuthActivity : AppCompatActivity() {

    private val TAG = AuthActivity::class.java.simpleName
    private var firebaseApp: FirebaseApp ?= null
    private var runningRef: DatabaseReference ?= null

    // Choose authentication providers
    var providers: List<AuthUI.IdpConfig> = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_auth)

        // create explanation and consent button
        // on consent given
        val btAccept = findViewById(R.id.bt_empushy_consent_accept) as Button
        val btCancel = findViewById(R.id.bt_empushy_consent_cancel) as Button

        val listenerGranted = StateUtils.isNotificationListenerGranted(applicationContext?.contentResolver, "com.aempathy.heedful")
        val usageGranted = StateUtils.isUsagePermissionGranted(applicationContext)
        if (listenerGranted && usageGranted) {
            Empushy.initEmpushyApp(applicationContext)
            firebaseApp = FirebaseApp.getInstance("empushy")
            if (applicationContext != null) {
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
            btAccept.setOnClickListener({ v -> consentGranted(listenerGranted, usageGranted) })
            btCancel.setOnClickListener({ v -> finish() })
        }
    }

    fun consentGranted(listenerGranted:Boolean, usageGranted:Boolean){
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

    fun permissionGranted(){
        Toast.makeText(applicationContext, "Permissions Granted.", Toast.LENGTH_LONG).show()
        Empushy.initEmpushyApp(applicationContext)
        firebaseApp = FirebaseApp.getInstance("empushy")
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
            signInSuccess(this)
        }
    }

    private fun signInSuccess(context: Context){

        Log.d(TAG, "Sign in success.")
        // check currentuser not null,
        // read db for services running
        // yes, do nothing
        // no, start notification
        // regardless, add app package to db
        try {

            val authInstance = FirebaseAuth.getInstance(firebaseApp!!)
            val ref = FirebaseDatabase.getInstance(firebaseApp!!).reference

            runningRef = ref.child("users").child(authInstance?.currentUser?.uid
                    ?: "none").child("running")
            runningRef?.addListenerForSingleValueEvent(runningReadListener)
        } catch(e:Exception){

            Log.d(TAG, "Exception starting service")
        }


    }

    var runningReadListener: ValueEventListener = object : ValueEventListener {

        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.childrenCount<1){
                val myService = Intent(applicationContext, EmpushyNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(myService)
                } else {
                    startService(myService)
                }
            }
            runningRef?.child(NotificationUtil.simplePackageName(applicationContext, applicationContext.packageName))
                    ?.setValue(true)
                    ?.addOnCompleteListener {
                        finish()
                    }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }
}
