package com.songi.cabinet.file

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager

class WatchDogCopy (application: Application) : AndroidViewModel(application) {
    private var TAG = "WatchDogCopy"
    val outputWorkInfoItems: LiveData<List<WorkInfo>>
    val progressWorkInfoItems: LiveData<List<WorkInfo>>
    private val workManager: WorkManager = WorkManager.getInstance(application)

    init {
        outputWorkInfoItems = workManager.getWorkInfosByTagLiveData("PROGRESS")
        progressWorkInfoItems = workManager.getWorkInfosByTagLiveData("COPY_BUILDER")
    }
}