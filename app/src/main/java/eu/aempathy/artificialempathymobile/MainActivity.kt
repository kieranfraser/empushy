package eu.aempathy.artificialempathymobile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.RC_SIGN_IN
import eu.aempathy.empushy.utils.EmpushyToggleButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Empushy.initialise(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == RC_SIGN_IN && resultCode == Activity.RESULT_OK){
            EmpushyToggleButton.signInSuccess(applicationContext)
        }
    }
}
