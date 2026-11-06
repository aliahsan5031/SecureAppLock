package com.example.applock.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IntruderDao {
    @Insert
    suspend fun insertIntruder(intruder: IntruderEntity)

    @Query("SELECT * FROM intruders ORDER BY timestamp DESC")
    fun getAllIntruders(): Flow<List<IntruderEntity>>
}
