package io.github.jamiesanson.mammut.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import io.github.jamiesanson.mammut.data.models.Emoji

class RoomConverters {

    @TypeConverter
    fun emojisToString(emojis: List<Emoji>): String
         = Gson().toJson(emojis)


    @TypeConverter
    fun stringToEmojis(string: String): List<Emoji>
        = Gson().fromJson<List<Emoji>>(string, List::class.java)

}