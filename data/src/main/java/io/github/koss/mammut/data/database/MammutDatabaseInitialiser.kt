package io.github.koss.mammut.data.database

import android.content.Context
import androidx.room.Room
import io.github.koss.mammut.data.database.migrations.Migration_1_2

private const val MAMMUT_DB_NAME = "mammut-db"

/**
 * Database initialise for the Mammut Database. Keeps tract of migrations etc.
 */
object MammutDatabaseInitialiser {

    private val migrations = listOf(
            Migration_1_2
    )

    /**
     * Main function for initialising the Mammut Database.
     */
    fun initialise(context: Context): MammutDatabase =
        Room.databaseBuilder(context, MammutDatabase::class.java, MAMMUT_DB_NAME)
                .addMigrations(*migrations.toTypedArray())
                .build()

}