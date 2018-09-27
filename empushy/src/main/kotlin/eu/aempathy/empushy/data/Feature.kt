package eu.aempathy.empushy.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 *
 */
data class Feature(
        @SerializedName("id") var id: String ?= null,
        @SerializedName("name") var name: String ?= "",
        @SerializedName("description") var description: String ?= "none",
        @SerializedName("category") var category: String ?= "none",
        @SerializedName("enabled") var enabled: Boolean ?= false
) : Serializable