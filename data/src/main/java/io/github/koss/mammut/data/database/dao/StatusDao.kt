package io.github.koss.mammut.data.database.dao

import androidx.room.*
import io.github.koss.mammut.data.database.entities.feed.Status
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNewPage(page: List<Status>)

    @Query("SELECT * from status ORDER BY createdAt DESC")
    fun getAllAsPublisher(): Flow<List<Status>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(statuses: List<Status>): List<Long>

    @Query("SELECT * FROM status ORDER BY createdAt DESC LIMIT 1")
    fun getLatest(): Status?

    @Query("SELECT * FROM status ORDER BY createdAt ASC LIMIT 1")
    fun getEarliest(): Status?

    @Query("DELETE FROM status WHERE id == :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM status")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStatus(status: Status)

    @Query("DELETE FROM status WHERE source = :source")
    fun deleteByFeed(source: String)

    @Query("SELECT MAX(statusIndex) + 1 FROM status WHERE source = :source")
    fun getNextIndexInFeed(source: String): Int

    @Query("SELECT MIN(statusIndex) - 1 FROM status WHERE source = :source")
    fun getPreviousIndexInFeed(source: String): Int

    @Query("SELECT * FROM status WHERE source = :source ORDER BY createdAt DESC LIMIT :count")
    fun getMostRecent(count: Int, source: String): List<Status>

    @Transaction
    fun deleteAllAndInsert(page: List<Status>) {
        deleteAll()
        insertAll(page)
    }
}