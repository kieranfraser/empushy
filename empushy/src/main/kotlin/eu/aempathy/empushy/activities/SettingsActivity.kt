package eu.aempathy.empushy.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import android.widget.CompoundButton
import android.widget.ExpandableListView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import eu.aempathy.empushy.R
import eu.aempathy.empushy.adapters.FeatureExpandableListAdapter
import eu.aempathy.empushy.data.Feature
import eu.aempathy.empushy.data.FeatureManager
import eu.aempathy.empushy.data.FeatureManager.Companion.COMMON_FEATURE_PATH
import eu.aempathy.empushy.data.FeatureManager.Companion.FEATURE_PATH
import eu.aempathy.empushy.data.FeatureManager.Companion.USER_PATH
import eu.aempathy.empushy.init.Empushy
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import java.util.*
import kotlin.collections.HashMap

/**
 * SettingsActivity
 * -
 */
class SettingsActivity : AppCompatActivity() {

    val TAG = EMPUSHY_TAG+SettingsActivity::class.java.simpleName

    var ref: DatabaseReference ?=null
    var featureManager: FeatureManager ?= null
    var authInstance: FirebaseAuth ?= null
    var featureRef: DatabaseReference ?= null
    var featureListener: ValueEventListener ?= null

    var expandableListView: ExpandableListView ?= null
    var adapter: FeatureExpandableListAdapter ?= null

    var featureData: HashMap<String, List<Feature>> ?= null
    var titleList: List<String> ?= null
    var expandedCats: ArrayList<Int> ?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_settings)

        try {
            val firebaseApp = Empushy.initialise(applicationContext)
            authInstance = FirebaseAuth.getInstance(firebaseApp)
            ref = FirebaseDatabase.getInstance(firebaseApp).reference

            featureManager = FeatureManager(arrayListOf(), listOf(), ref!!, authInstance?.currentUser?.uid!!)
            expandableListView = findViewById(R.id.elv_features)
            expandedCats = arrayListOf()
            // Get features.. listener
            getFeatures()
            setUpUI()

        } catch(e:Exception){finish()}

    }

    private fun setUpUI(){
        featureData = HashMap<String, List<Feature>>()

        titleList = featureManager?.categories
        for(cat in titleList?: listOf()) {
            featureData?.put(cat, featureManager?.features?.filter { feature -> feature.category == cat }?: listOf())
        }
        if (expandableListView != null) {
            adapter = FeatureExpandableListAdapter(this, titleList?: listOf(), featureData?: hashMapOf(),
                    {button: CompoundButton, checked: Boolean -> checkChanged(button, checked) })
            expandableListView!!.setAdapter(adapter)

            expandableListView!!.setOnGroupExpandListener {
                groupPosition ->
                expandedCats?.add(groupPosition)
            }


            expandableListView!!.setOnGroupCollapseListener {
                groupPosition ->
                val toRemove = expandedCats?.filter { i -> i == groupPosition }
                for(i in toRemove?: listOf()){
                    expandedCats?.remove(i)
                }
            }

            for(cat in expandedCats?.toMutableList()?: arrayListOf()){
                expandableListView?.expandGroup(cat)
            }

            /*expandableListView!!.setOnChildClickListener {
                parent, v, groupPosition, childPosition, id ->
                Toast.makeText(applicationContext,
                        "Clicked: " + titleList?.get(groupPosition) + " -> " +
                                featureData?.get(titleList?.get(groupPosition))?.get(childPosition), Toast.LENGTH_SHORT).show()
                false
            }*/
        }

    }

    private fun checkChanged(button: CompoundButton, checked: Boolean){
        val featureId = button.tag as String
        featureManager?.updateFeatureEnabled(featureId, checked)
    }

    /**
     * Call to get user features
     *  - set FeatureManager features if not empty
     *  - if empty call getCommonFeatures() e.g. for new user
     *  - keepSynced
     */
    private fun getFeatures(){
        try {
            featureRef = ref?.child(USER_PATH)?.child(authInstance!!.currentUser!!.uid)?.child(FEATURE_PATH)
            featureListener = featureRef?.addValueEventListener(readListener)
        } catch(e: Exception){Log.d(TAG, "Unable to get features.")}
    }

    var readListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(snapshot.hasChildren()) {
                val featureList = ArrayList<Feature>()
                val categoryList = ArrayList<String>()
                for (child in snapshot.children) {
                    val feature = child?.getValue(Feature::class.java)
                    if (feature != null) {
                        featureList.add(feature)
                        categoryList.add(feature.category?:"none")
                    }
                }
                featureManager?.features = featureList
                featureManager?.categories = categoryList.distinct()
                setUpUI()
            }
            else{
                getCommonFeatures()
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (featureRef != null && featureListener != null) {
                featureRef?.removeEventListener(featureListener!!)
            }
        } catch (e: Exception){Log.d(TAG, "Could not remove listener.")}
    }

    /**
     * Call to get common features
     *  - calls featureManager update() to update user in FB
     *  - oneTimeCall
     */
    private fun getCommonFeatures(){
        try {
            ref?.child(COMMON_FEATURE_PATH)?.addListenerForSingleValueEvent(commonReadListener)
        } catch(e: Exception){Log.d(TAG, "Unable to get common features.")}
    }

    var commonReadListener: ValueEventListener = object : ValueEventListener  {

        override fun onDataChange(snapshot: DataSnapshot) {

            if(snapshot.hasChildren()) {
                val featureList = ArrayList<Feature>()
                for (child in snapshot.children) {
                    val feature = child?.getValue(Feature::class.java)
                    if (feature != null) {
                        featureList.add(feature)
                    }
                }
                featureManager?.updateFeatures(featureList)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {}
    }

}
