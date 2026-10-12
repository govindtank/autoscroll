package com.droidtank.autoscroll.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_configs ORDER BY lastUsedTimestamp DESC")
    fun getAllConfigs(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_configs WHERE isEnabled = 1")
    fun getEnabledConfigs(): Flow<List<AppConfig>>

    @Query("SELECT * FROM app_configs WHERE packageName = :packageName LIMIT 1")
    suspend fun getConfig(packageName: String): AppConfig?

    @Upsert
    suspend fun upsertConfig(config: AppConfig)

    @Query("DELETE FROM app_configs WHERE packageName = :packageName")
    suspend fun deleteConfig(packageName: String)

    @Query("UPDATE app_configs SET sessionScrollCount = :count WHERE packageName = :packageName")
    suspend fun updateScrollCount(packageName: String, count: Int)

    @Query("UPDATE app_configs SET lastUsedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastUsed(packageName: String, timestamp: Long)
}

@Dao
interface GestureRecordingDao {
    @Query("SELECT * FROM gesture_recordings ORDER BY usageCount DESC, createdAt DESC")
    fun getAllGestures(): Flow<List<GestureRecording>>

    @Query("SELECT * FROM gesture_recordings WHERE packageName = :packageName OR packageName IS NULL ORDER BY usageCount DESC")
    fun getGesturesForApp(packageName: String): Flow<List<GestureRecording>>

    @Query("SELECT * FROM gesture_recordings WHERE id = :id LIMIT 1")
    suspend fun getGesture(id: Long): GestureRecording?

    @Upsert
    suspend fun upsertGesture(gesture: GestureRecording): Long

    @Query("UPDATE gesture_recordings SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("DELETE FROM gesture_recordings WHERE id = :id")
    suspend fun deleteGesture(id: Long)

    @Query("SELECT id, name, iconHint, totalDurationMs, usageCount FROM gesture_recordings ORDER BY usageCount DESC")
    fun getGestureSummaries(): Flow<List<GestureSummaryRaw>>
}

/**
 * Raw projection for gesture summaries (avoids loading heavy strokesJson).
 */
data class GestureSummaryRaw(
    val id: Long,
    val name: String,
    val iconHint: String?,
    val totalDurationMs: Long,
    val usageCount: Int
)
