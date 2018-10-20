package io.github.jamiesanson.mammut.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.jamiesanson.mammut.data.models.Attachment
import io.github.jamiesanson.mammut.data.models.Emoji
import io.github.jamiesanson.mammut.data.models.Mention
import io.github.jamiesanson.mammut.data.models.Tag

class RoomConverters {

    private val emojiType = (object: TypeToken<List<Emoji>>() {}).type

    @TypeConverter
    fun emojisToString(emojis: ArrayList<Emoji>): String
         = Gson().toJson(emojis.toList(), emojiType)


    @TypeConverter
    fun stringToEmojis(string: String): ArrayList<Emoji>
        = ArrayList(Gson().fromJson<List<Emoji>>(string, emojiType))


    private val attachmentType = (object: TypeToken<List<Attachment>>() {}).type

    @TypeConverter
    fun attachmentsToString(attachments: ArrayList<Attachment>): String
        = Gson().toJson(attachments.toList(), attachmentType)

    @TypeConverter
    fun stringToAttachments(string: String): ArrayList<Attachment>
        = ArrayList(Gson().fromJson<List<Attachment>>(string, attachmentType))


    private val mentionType = (object: TypeToken<List<Mention>>() {}).type

    @TypeConverter
    fun mentionsToString(mentions: ArrayList<Mention>): String
            = Gson().toJson(mentions.toList(), mentionType)

    @TypeConverter
    fun stringToMentions(string: String): ArrayList<Mention>
            = ArrayList(Gson().fromJson<List<Mention>>(string, mentionType))

    private val tagType = (object: TypeToken<List<Tag>>() {}).type

    @TypeConverter
    fun tagsToString(tags: ArrayList<Tag>): String
            = Gson().toJson(tags.toList(), tagType)

    @TypeConverter
    fun stringToTags(string: String): ArrayList<Tag>
            = ArrayList(Gson().fromJson<List<Tag>>(string, tagType))
}