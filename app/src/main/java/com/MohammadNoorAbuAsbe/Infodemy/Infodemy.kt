package com.MohammadNoorAbuAsbe.Infodemy

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.MohammadNoorAbuAsbe.Infodemy.workers.GradeCheckWorker
import java.util.concurrent.TimeUnit

class Infodemy : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule the WorkManager task
        val gradeCheckRequest = PeriodicWorkRequestBuilder<GradeCheckWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "GradeCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            gradeCheckRequest
        )
    }
}