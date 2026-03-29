package com.darkempire78.opencalculator.bookmarks

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class Bookmark (
    @SerializedName("calculation") var calculation: String,
    @SerializedName("result") var result: String,
    @SerializedName("time") var time: String = System.currentTimeMillis().toString(),
    @SerializedName("id") var id: String = UUID.randomUUID().toString()
)