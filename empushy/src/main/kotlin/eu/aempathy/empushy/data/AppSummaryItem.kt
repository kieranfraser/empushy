package eu.aempathy.empushy.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by Kieran on 19/03/2018.
 */
data class AppSummaryItem(

        @SerializedName("app") var app: String ?= null,
        @SerializedName("appName") var appName: String ?= null,
        @SerializedName("active") var active: ArrayList<EmpushyNotification> ?= ArrayList(),
        @SerializedName("hidden") var hidden: ArrayList<EmpushyNotification> ?= ArrayList()

) : Serializable