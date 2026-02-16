package com.pedometr.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "step_entries")
data class StepEntry(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val steps: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromLocalDate(date: LocalDate, steps: Int): StepEntry {
            return StepEntry(
                date = date.toString(),
                steps = steps
            )
        }
    }
    
    fun toLocalDate(): LocalDate = LocalDate.parse(date)
}

