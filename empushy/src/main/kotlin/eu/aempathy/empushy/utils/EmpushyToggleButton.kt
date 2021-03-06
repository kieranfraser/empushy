package eu.aempathy.empushy.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import eu.aempathy.empushy.R
import eu.aempathy.empushy.activities.AuthActivity
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.services.EmpushyNotificationService

/**
 * EmPushy Toggle Button
 * - executes functionality of XML Toggle Button
 * which is to be added to an apps settings screen to enable users to avail
 * of EmPushy functionality by logging-in or signing-up to the service.
 *
 * Note: Must also override activity onResume() to keep user EmPushy auth
 * state synced.
 */

class EmpushyToggleButton : LinearLayout {

    private val TAG = EmpushyToggleButton::class.java.simpleName
    private var ref:DatabaseReference ?= null

    private var mContext: Context ?= null

    constructor(context: Context) : super(context) {
        this.mContext = context
        setUpToggle()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs,
                R.styleable.EmpushyToggleButton, 0, 0)
        /*val titleText = a.getString(R.styleable.EmpushyToggleButton_titleText)
        *//*val valueColor = a.getColor(R.styleable.EmpushyToggleButton_valueColor,
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

        if(StateUtils.isNetworkAvailable(context)) {

            val firebaseApp = Empushy.initialise(activity)
            val authInstance: FirebaseAuth = FirebaseAuth.getInstance(firebaseApp)
            ref = FirebaseDatabase.getInstance(firebaseApp).reference

            if (authInstance.currentUser != null) {
                tb.isChecked = true
                Log.d(TAG, "Ensure service started.")
                val myService = Intent(context, EmpushyNotificationService::class.java)
                myService.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(myService)
                } else {
                    context.startService(myService)
                }
            }
            tb.setOnCheckedChangeListener { compoundButton, isChecked ->
                if (isChecked) {
                    val intent = Intent(context, AuthActivity::class.java)
                    (context as Activity).startActivity(intent)
                } else {
                    if (authInstance.currentUser != null) {

                        ref?.child("users")?.child(authInstance.currentUser?.uid?:"none")?.child("running")
                                ?.child(NotificationUtil.simplePackageName(context, context.packageName))?.removeValue()
                        // delete all apps running service
                        // notificationlistenerservice running should pick up and log out
                        // removes running notification (this code)
                        val myService = Intent(context, EmpushyNotificationService::class.java)
                        myService.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                        context.startService(myService)
                        context.stopService(myService)
                        authInstance.signOut()
                        // checks if notification service running.. if so, stop
                        Toast.makeText(activity, "Logged out of EmPushy.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        else{
            tb.setOnClickListener({
                tb.isChecked = false
                Toast.makeText(context, "You must have an internet connection to toggle this setting.", Toast.LENGTH_LONG).show()})
        }
    }

    fun update(context: Context){
        val tb = (getChildAt(0) as LinearLayout).getChildAt(1) as ToggleButton
        tb.isChecked = Empushy.loggedIn(context)
    }
}
