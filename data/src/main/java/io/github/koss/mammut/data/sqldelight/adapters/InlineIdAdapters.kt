package io.github.koss.mammut.data.sqldelight.adapters

import com.squareup.sqldelight.ColumnAdapter
import io.github.koss.mammut.data.sqldelight.AccountId
import io.github.koss.mammut.data.sqldelight.StatusId
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
inline fun <reified T: Any> inlineIdAdapter(): ColumnAdapter<T, Long> =
    when (T::class.java) {
        StatusId::class.java -> StatusIdAdapter()
        AccountId::class.java -> AccountIdAdapter()
        else -> throw IllegalArgumentException("No inline ID set for type: ${T::class.java.simpleName}")
    } as ColumnAdapter<T, Long>

class StatusIdAdapter: ColumnAdapter<StatusId, Long> {
    override fun decode(databaseValue: Long): StatusId = StatusId(databaseValue)

    override fun encode(value: StatusId): Long = value.value
}

class AccountIdAdapter: ColumnAdapter<AccountId, Long> {
    override fun decode(databaseValue: Long): AccountId = AccountId(databaseValue)

    override fun encode(value: AccountId): Long = value.value
}