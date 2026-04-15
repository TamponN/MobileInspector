package com.bestplus.mobileinspector.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bestplus.mobileinspector.data.local.entity.RouteSheetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteSheetDao {

    @Query("SELECT * FROM route_sheets ORDER BY planDateTime ASC")
    fun observeAll(): Flow<List<RouteSheetEntity>>

    @Query("SELECT * FROM route_sheets WHERE uuidDocument = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): RouteSheetEntity?

    @Query("SELECT * FROM route_sheets")
    suspend fun getAll(): List<RouteSheetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RouteSheetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<RouteSheetEntity>)

    @Update
    suspend fun update(entity: RouteSheetEntity)

    @Query("DELETE FROM route_sheets WHERE uuidDocument = :uuid")
    suspend fun deleteByUuid(uuid: String)

    @Query("DELETE FROM route_sheets")
    suspend fun deleteAll()

    @Query("DELETE FROM route_sheets WHERE statusTask = 'Закрыт'")
    suspend fun deleteClosed()
}
