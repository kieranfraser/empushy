package eu.aempathy.empushy.data

import com.google.gson.annotations.SerializedName
import org.json.JSONObject
import java.io.Serializable

/**
 * EmpushyNotification
 * - created using StatusBarNotification posted to the NotificationListenerService
 * - useful information extracted
 * - additional information inferred e.g. clicked, placeCategories etc.
 */

data class EmpushyNotification(

        @SerializedName("app") var app: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("appName") var appName: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("ticker") var ticker: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("time") var time: Long ?= NOTIFICATION_LONG_DEFAULT,
        @SerializedName("extra") var extra: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("id") var id: String ?= null,
        @SerializedName("notifyId") var notifyId: Int ?= NOTIFICATION_INTEGER_DEFAULT,
        @SerializedName("empushyNotifyId") var empushyNotifyId: Int ?= NOTIFICATION_INTEGER_DEFAULT,
        @SerializedName("category") var category: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("appLastUsed") var appLastUsed: Long ?= NOTIFICATION_LONG_DEFAULT,
        @SerializedName("extraBigText") var extraBigText: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("infoText") var infoText: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("subText") var subText: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("summaryText") var summaryText: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("extraText") var extraText: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("extraTextLines") var extraTextLines: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("extraTitle") var extraTitle: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("extraTitleBig") var extraTitleBig: String ?= NOTIFICATION_STRING_DEFAULT,
        @SerializedName("clicked") var clicked: Boolean ?= false,
        @SerializedName("removedTime") var removedTime: Long ?= NOTIFICATION_LONG_DEFAULT,
        @SerializedName("placeCategories") var placeCategories: ArrayList<Int> ?= arrayListOf(),
        @SerializedName("hidden") var hidden: Boolean ?= false

) : Serializable{

        fun convertToJSON(): JSONObject{
            val o = JSONObject()
            o.put("app", app)
            o.put("appName", appName)
            o.put("category", category)
            o.put("clicked", clicked)
            o.put("extraText", extraText)
            o.put("extraTitle", extraTitle)
            o.put("hidden", hidden)
            o.put("id", id)
            o.put("infoText", infoText)
            o.put("notifyId", notifyId)
            o.put("removedTime", removedTime)
            o.put("subText", subText)
            o.put("ticker", ticker)
            o.put("time", time)
            return o
        }

}