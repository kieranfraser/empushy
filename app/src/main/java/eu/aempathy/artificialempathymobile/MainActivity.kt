package eu.aempathy.artificialempathymobile

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import eu.aempathy.empushy.init.Empushy
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Example use of EmPushy Library
 * 1. In entry point of app, initialise the library.
 *
 * 2. In app "settings" section, add EmPushy toggle button
 *    to activity_layout.xml (example in activity_main.xml)
 *
 * 3. If app "settings" activity, override onResume and
 *    add update function for toggle button.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Initialise EmPushy in main entry point to application.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Empushy.initialise(this)
    }

    /**
     * Override onResume to keep toggle button synced with
     * user auth state.
     */
    override fun onResume() {
        tb_empushy.update(this)
        super.onResume()
    }
}
