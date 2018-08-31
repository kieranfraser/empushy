package eu.aempathy.artificialempathymobile

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import eu.aempathy.empushy.init.Empushy

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Empushy.initialise(this)
    }
}
