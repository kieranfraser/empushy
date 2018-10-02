package eu.aempathy.empushy.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Used in the Detail activity to summarise the notifications
 * by application, and active/hidden status.
 */
data class AppSummaryItem(

        @SerializedName("app") var app: String ?= null,
        @SerializedName("appName") var appName: String ?= null,
        @SerializedName("active") var active: ArrayList<EmpushyNotification> ?= ArrayList()

) : Serializable