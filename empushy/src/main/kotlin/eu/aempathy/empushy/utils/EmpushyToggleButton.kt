package eu.aempathy.empushy.utils

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import com.firebase.ui.auth.AuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import eu.aempathy.empushy.R
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.RC_SIGN_IN
import eu.aempathy.empushy.services.EmpushyNotificationService
import eu.aempathy.empushy.services.EmpushyNotificationService.Companion.ANDROID_CHANNEL_ID
import java.util.*

/**
 * Created by Kieran on 29/06/2018.
 */

class EmpushyToggleButton : LinearLayout {

    companion object {
        fun signInSuccess(context: Context){
            val myService = Intent(context, EmpushyNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(myService)
            } else {
                context.startService(myService)
            }
        }
    }
    // Choose authentication providers
    var providers: List<AuthUI.IdpConfig> = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build())
    private var mContext: Context ?= null

    constructor(context: Context) : super(context) {
        this.mContext = context
        setUpToggle()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.EmpushyToggleButton, 0, 0)
        val titleText = a.getString(R.styleable.EmpushyToggleButton_titleText)
        /*val valueColor = a.getColor(R.styleable.EmpushyToggleButton_valueColor,
                android.R.color.holo_blue_light)*/
        a.recycle()

        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val inflater = context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.empushy_toggle_button, this, true)

        setUpToggle()

    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setUpToggle()
    }

    fun setUpToggle() {
        val tb = (getChildAt(0) as LinearLayout).getChildAt(1) as ToggleButton
        val activity = tb.context as Activity

        Empushy.initEmpushyApp(activity)

        val firebaseApp = FirebaseApp.getInstance("empushy")
        val authInstance = FirebaseAuth.getInstance(firebaseApp)
        if (authInstance.currentUser != null) {
            tb.isChecked = true
        }
        tb.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                activity.startActivityForResult(
                        AuthUI.getInstance(firebaseApp)
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN)
            } else {
                if (authInstance.currentUser != null) {
                    authInstance.signOut()
                    val nm = (context as Activity).getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        nm.deleteNotificationChannel(ANDROID_CHANNEL_ID)
                    } else {
                        nm.cancelAll()
                    }
                    val myService = Intent(context, EmpushyNotificationService::class.java)
                    (context as Activity).stopService(myService)
                    Toast.makeText(activity, "Logged out of EmPushy.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
