package org.stepic.droid.model.achievements

import com.google.gson.annotations.SerializedName
import java.util.*

class AchievementProgress(
        val id: Long,
        val user: Long,
        val achievement: Long,
        val score: Int,
        val kind: String,

        @SerializedName("create_date")
        val createDate: Date,
        @SerializedName("update_date")
        val updateDate: Date,
        @SerializedName("obtain_date")
        val obtainDate: Date?
)

val EmptyAchievementProgressStub = AchievementProgress(0, 0, 0, 0, String(), Date(0), Date(0), null)