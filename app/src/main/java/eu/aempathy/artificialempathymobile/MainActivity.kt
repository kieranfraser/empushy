package eu.aempathy.artificialempathymobile

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import eu.aempathy.empushy.init.Empushy
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Empushy.initialise(this)
    }

    override fun onResume() {
        tb_empushy.update(this)
        super.onResume()
    }
}
