package io.github.koss.mammut.data.sqldelight

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import io.github.koss.mammut.Account
import io.github.koss.mammut.Status
import io.github.koss.mammut.data.Database
import io.github.koss.mammut.data.sqldelight.adapters.inlineIdAdapter
import io.github.koss.mammut.data.sqldelight.adapters.serializableAdapter

object DatabaseInitialiser {

    fun getDatabase(appContext: Context, name: String) {
        Database(
            driver = AndroidSqliteDriver(Database.Schema, appContext, name),
            accountAdapter = Account.Adapter(
                idAdapter = inlineIdAdapter()
            ),
            statusAdapter = Status.Adapter(
                idAdapter = inlineIdAdapter(),
                in_reply_to_account_idAdapter = inlineIdAdapter(),
                in_reply_to_idAdapter = inlineIdAdapter(),
                reblog_idAdapter = inlineIdAdapter(),
                video_attachmentsAdapter = serializableAdapter(),
                gifv_attachmentsAdapter = serializableAdapter(),
                photo_attachmentsAdapter = serializableAdapter(),
                mentionsAdapter = serializableAdapter(),
                tagsAdapter = serializableAdapter(),
                applicationAdapter = serializableAdapter()
            )
        )
    }
}