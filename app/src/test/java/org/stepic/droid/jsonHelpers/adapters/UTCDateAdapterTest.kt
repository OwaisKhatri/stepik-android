package org.stepic.droid.jsonHelpers.adapters

import com.google.gson.GsonBuilder
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.stepic.droid.features.deadlines.model.DeadlinesWrapper
import org.stepic.droid.util.toObject
import java.util.*

class UTCDateAdapterTest {
    @Test
    fun deserializationTest() {
        val gson = GsonBuilder().registerTypeAdapter(Date::class.java, UTCDateAdapter()).create()
        val deadlines = """{"course":67,"deadlines":[{"section":123,"deadline":"2012-04-23T18:25:43.511Z"}]}"""

        val wrapper = deadlines.toObject<DeadlinesWrapper>(gson)
        assertEquals(gson.toJson(wrapper), deadlines)
    }
}