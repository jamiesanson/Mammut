package io.github.jamiesanson.mammut.data.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.sys1yagi.mastodon4j.api.entity.Attachment
import com.sys1yagi.mastodon4j.api.entity.GifvAttachment
import com.sys1yagi.mastodon4j.api.entity.PhotoAttachment
import com.sys1yagi.mastodon4j.api.entity.VideoAttachment
import com.sys1yagi.mastodon4j.extension.RuntimeTypeAdapterFactory
import io.github.jamiesanson.mammut.data.models.*

class RoomConverters {

    private val emojiType = (object : TypeToken<List<Emoji>>() {}).type

    @TypeConverter
    fun emojisToString(emojis: ArrayList<Emoji>): String = Gson().toJson(emojis.toList(), emojiType)

    @TypeConverter
    fun stringToEmojis(string: String): ArrayList<Emoji> = ArrayList(Gson().fromJson<List<Emoji>>(string, emojiType))


    private val attachmentType = (object : TypeToken<List<Attachment<*>>>() {}).type

    @TypeConverter
    fun attachmentsToString(attachments: ArrayList<Attachment<*>>): String = GsonBuilder().apply {
        registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory.of(Attachment::class.java, "type", true, true)
                        .registerSubtype(PhotoAttachment::class.java, Attachment.Type.Image.value)
                        .registerSubtype(GifvAttachment::class.java, Attachment.Type.Gifv.value)
                        .registerSubtype(VideoAttachment::class.java, Attachment.Type.Video.value)
        )
    }.create().toJson(attachments.toList(), attachmentType)

    @TypeConverter
    fun stringToAttachments(string: String): ArrayList<Attachment<*>> = ArrayList(GsonBuilder().apply {
        registerTypeAdapterFactory(
                RuntimeTypeAdapterFactory.of(Attachment::class.java, "type", true, true)
                        .registerSubtype(PhotoAttachment::class.java, Attachment.Type.Image.value)
                        .registerSubtype(GifvAttachment::class.java, Attachment.Type.Gifv.value)
                        .registerSubtype(VideoAttachment::class.java, Attachment.Type.Video.value)
        )
    }.create().fromJson<List<Attachment<*>>>(string, attachmentType))


    private val mentionType = (object : TypeToken<List<Mention>>() {}).type

    @TypeConverter
    fun mentionsToString(mentions: ArrayList<Mention>): String = Gson().toJson(mentions.toList(), mentionType)

    @TypeConverter
    fun stringToMentions(string: String): ArrayList<Mention> = ArrayList(Gson().fromJson<List<Mention>>(string, mentionType))

    private val tagType = (object : TypeToken<List<Tag>>() {}).type

    @TypeConverter
    fun tagsToString(tags: ArrayList<Tag>): String = Gson().toJson(tags.toList(), tagType)

    @TypeConverter
    fun stringToTags(string: String): ArrayList<Tag> = ArrayList(Gson().fromJson<List<Tag>>(string, tagType))
}