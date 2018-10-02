package eu.aempathy.empushy.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import eu.aempathy.empushy.utils.StateUtils
import java.util.*

/**
 * Check if user has features.. if get common features
 */
class FeatureManager(f: ArrayList<Feature>, c: List<String>, r: DatabaseReference, u: String) {

    val TAG = EMPUSHY_TAG + FeatureManager::class.java.simpleName

    companion object {
        val USER_PATH = "users"
        val FEATURE_PATH = "features"
        val COMMON_FEATURE_PATH = "common/features"
        val FEATURE_ENABLED_PATH = "enabled"
    }

    var features: ArrayList<Feature>
    var categories: List<String>
    val ref: DatabaseReference
    val userId: String

    // initializer block
    init {
        features = f
        ref = r
        userId = u
        categories = c
    }

    fun updateFeatures(featureList: ArrayList<Feature>){
        for(feature in featureList){
            try {
                ref.child(USER_PATH).child(userId).child(FEATURE_PATH).child(feature.id!!).setValue(feature)
            } catch(e: Exception){
                Log.d(TAG, "Could not update feature.")
            }
        }
    }

    fun updateFeatureEnabled(context: Context, featureId: String?, enabled: Boolean){
        if(StateUtils.isNetworkAvailable(context)) {
            if (featureId != null && features.isNotEmpty()) {

                try {
                    ref.child(USER_PATH).child(userId).child(FEATURE_PATH)
                            .child(featureId).child(FEATURE_ENABLED_PATH).setValue(enabled)

                    for(f in features){
                        if(f.id == featureId)
                            f.enabled = enabled
                    }
                    val temp = arrayListOf<Feature>()
                    Collections.copy(temp, features)
                    features.clear()
                    features.addAll(temp)

                } catch (e: Exception) {
                    Log.d(TAG, "Could not update feature.")
                }
            }
        } else{
            Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFeatureList():ArrayList<Feature>{return features}

    fun recommend(){}

    fun simulate(){}

    fun abstractionScore(){}


}