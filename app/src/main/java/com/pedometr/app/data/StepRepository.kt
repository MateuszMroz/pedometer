package com.pedometr.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class StepRepository(private val stepDao: StepDao) {
    
    fun getAllSteps(): Flow<List<StepEntry>> = stepDao.getAllSteps()
    
    fun getStepsForLast3Months(): Flow<List<StepEntry>> {
        val threeMonthsAgo = LocalDate.now().minusMonths(3).toString()
        return stepDao.getStepsSince(threeMonthsAgo)
    }
    
    suspend fun getStepsForDate(date: LocalDate): Int {
        return stepDao.getStepsForDate(date.toString())?.steps ?: 0
    }
    
    suspend fun updateStepsForToday(steps: Int) {
        val today = LocalDate.now().toString()
        stepDao.updateStepsForDate(today, steps)
    }
    
    suspend fun updateStepsForDate(date: LocalDate, steps: Int) {
        stepDao.updateStepsForDate(date.toString(), steps)
    }
    
    suspend fun cleanOldData() {
        val threeMonthsAgo = LocalDate.now().minusMonths(3).toString()
        stepDao.deleteOlderThan(threeMonthsAgo)
    }
    
    suspend fun getTotalStepsThisMonth(): Int {
        val firstDayOfMonth = LocalDate.now().withDayOfMonth(1).toString()
        return stepDao.getTotalStepsSince(firstDayOfMonth) ?: 0
    }
}

