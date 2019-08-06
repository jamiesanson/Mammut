package io.github.koss.mammut.data.sqldelight.adapters

import com.google.gson.Gson
import com.squareup.sqldelight.ColumnAdapter

class GsonSerializableColumnAdapter<T: Any>(
    private val type: Class<T>
): ColumnAdapter<T, String> {

    override fun decode(databaseValue: String): T =
        gson.fromJson(databaseValue, type)

    override fun encode(value: T): String =
        gson.toJson(value)

    companion object {
        private val gson = Gson()
    }
}

inline fun <reified T: Any> serializableAdapter() =
    GsonSerializableColumnAdapter(type = T::class.java)