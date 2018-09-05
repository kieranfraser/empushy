package eu.aempathy.empushy.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * EmpushyNotification
 * - created using StatusBarNotification posted to the NotificationListenerService
 * - useful information extracted
 * - additional information inferred e.g. clicked, placeCategories etc.
 */
data class EmpushyNotification(

        @SerializedName("app") var app: String ?= null,
        @SerializedName("appName") var appName: String ?= null,
        @SerializedName("ticker") var ticker: String ?= null,
        @SerializedName("time") var time: Long ?= 0,
        @SerializedName("extra") var extra: String ?= null,
        @SerializedName("id") var id: String ?= null,
        @SerializedName("notifyId") var notifyId: Int ?= null,
        @SerializedName("category") var category: String ?= null,
        @SerializedName("appLastUsed") var appLastUsed: Long ?= null,
        @SerializedName("extraBigText") var extraBigText: String ?= null,
        @SerializedName("infoText") var infoText: String ?= null,
        @SerializedName("subText") var subText: String ?= null,
        @SerializedName("summaryText") var summaryText: String ?= null,
        @SerializedName("extraText") var extraText: String ?= null,
        @SerializedName("extraTextLines") var extraTextLines: String ?= null,
        @SerializedName("extraTitle") var extraTitle: String ?= null,
        @SerializedName("extraTitleBig") var extraTitleBig: String ?= null,
        @SerializedName("clicked") var clicked: Boolean ?= false,
        @SerializedName("removedTime") var removedTime: Long ?= null,
        @SerializedName("placeCategories") var placeCategories: ArrayList<Int> ?= null,
        @SerializedName("hidden") var hidden: Boolean ?= false

) : Serializable