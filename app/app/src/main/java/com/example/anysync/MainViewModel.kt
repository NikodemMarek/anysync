package com.example.anysync

import androidx.lifecycle.ViewModel
import com.example.anysync.data.MainDatabase
import com.example.anysync.data.Source
import kotlinx.coroutines.flow.Flow
import kotlin.concurrent.thread

class MainViewModel : ViewModel() {
    private lateinit var database: MainDatabase

    lateinit var sources: Flow<List<Source>>
    fun addSource(source: Source) {
        thread {
            database.sourceDao().insert(source)
        }
    }

    fun removeSource(source: Source) {
        thread {
            database.sourceDao().delete(source)
        }
    }

    fun updateSource(source: Source) {
        thread {
            database.sourceDao().update(source)
        }
    }

    fun init(context: android.content.Context) {
        database = MainDatabase.init(context)!!

        thread {
            sources = database.sourceDao().getAllFlow()
        }
    }
}
