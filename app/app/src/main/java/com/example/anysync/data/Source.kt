package com.example.anysync.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class Actions {
    NONE,
    GET,
    SET,
    GET_SET, ;

    companion object {
        fun fromInt(value: Int) = entries.firstOrNull { it.ordinal == value }
    }
}

@Entity
data class Source(
    @ColumnInfo
    val name: String,

    @ColumnInfo
    val label: String = name,

    @ColumnInfo
    val path: String,

    @ColumnInfo
    val host: String,

    @ColumnInfo
    val actions: Actions,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

fun Source.url() = "$host/$name"

@Dao
interface SourceDao {
    @Query("SELECT * FROM source")
    fun getAllFlow(): Flow<List<Source>>

    @Insert
    fun insert(vararg sources: Source)

    @Delete
    fun delete(source: Source)

    @Update
    fun update(source: Source)
}