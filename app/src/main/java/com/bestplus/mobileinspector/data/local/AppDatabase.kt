package com.bestplus.mobileinspector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bestplus.mobileinspector.data.local.dao.RouteSheetDao
import com.bestplus.mobileinspector.data.local.entity.Converters
import com.bestplus.mobileinspector.data.local.entity.RouteSheetEntity

@Database(
    entities = [RouteSheetEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeSheetDao(): RouteSheetDao
}
