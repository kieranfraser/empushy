package eu.aempathy.empushy.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import eu.aempathy.empushy.R

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_detail)
    }
}
