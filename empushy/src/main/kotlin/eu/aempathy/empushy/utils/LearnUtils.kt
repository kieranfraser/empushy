package eu.aempathy.empushy.utils

import android.util.Log
import eu.aempathy.empushy.data.EmpushyNotification
import eu.aempathy.empushy.init.Empushy.EMPUSHY_TAG
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * Created by Kieran on 23/09/2018.
 */
class LearnUtils {

    companion object {
        private val TAG = EMPUSHY_TAG+LearnUtils::class.java.simpleName

        fun deliverNotificationNow(notifications: List<EmpushyNotification>, userId: String): ArrayList<Boolean> {
            var result = arrayListOf<Boolean>()
            val client = OkHttpClient()

            val notificationArray = JSONArray()
            for(notification in notifications){
                notificationArray.put(notification.convertToJSON())
            }
            val requestJson = JSONObject()
            requestJson.put("notifications", notificationArray)
            requestJson.put("userId", userId)
            Log.d(TAG, requestJson.toString())

            val request = Request.Builder()
                    .url("https://empushy-learn.herokuapp.com/notification")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                            requestJson.toString()))
                    .build()
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    throw IOException("Unexpected code $response")
                } else {
                    val jsonData = JSONObject(response.body()?.string())
                    val failed = jsonData.getInt("failed")
                    result = jsonArrayToBooleanArray(jsonData.getJSONArray("prediction"))
                    Log.d(TAG, "Failed: "+failed)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception predicting notification: " + e.message+e.toString())
            }
            return result
        }

        fun jsonArrayToBooleanArray(jsonArray: JSONArray): ArrayList<Boolean>{
            val list = ArrayList<Boolean>()
            val len = jsonArray.length()
            for (i in 0 until len) {
                list.add(jsonArray.get(i).toString().toBoolean())
            }
            return list
        }

    }

}