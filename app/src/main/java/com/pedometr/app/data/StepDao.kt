package com.pedometr.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM step_entries ORDER BY date DESC")
    fun getAllSteps(): Flow<List<StepEntry>>
    
    @Query("SELECT * FROM step_entries WHERE date = :date LIMIT 1")
    suspend fun getStepsForDate(date: String): StepEntry?
    
    @Query("SELECT * FROM step_entries WHERE date >= :startDate ORDER BY date DESC")
    fun getStepsSince(startDate: String): Flow<List<StepEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stepEntry: StepEntry)
    
    @Query("DELETE FROM step_entries WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
    
    @Query("SELECT SUM(steps) FROM step_entries WHERE date >= :startDate")
    suspend fun getTotalStepsSince(startDate: String): Int?
    
    @Transaction
    suspend fun updateStepsForDate(date: String, steps: Int) {
        val existing = getStepsForDate(date)
        if (existing != null) {
            insertOrUpdate(existing.copy(steps = steps, timestamp = System.currentTimeMillis()))
        } else {
            insertOrUpdate(StepEntry(date = date, steps = steps))
        }
    }
}

