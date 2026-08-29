package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceUpdateDao {
    @Query("SELECT * FROM voice_updates ORDER BY timestamp DESC")
    fun getAllUpdates(): Flow<List<VoiceUpdateEntity>>

    @Query("SELECT * FROM voice_updates WHERE status = :status ORDER BY timestamp DESC")
    fun getUpdatesByStatus(status: String): Flow<List<VoiceUpdateEntity>>

    @Query("SELECT * FROM voice_updates WHERE id = :id LIMIT 1")
    suspend fun getUpdateById(id: String): VoiceUpdateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: VoiceUpdateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUpdates(updates: List<VoiceUpdateEntity>)

    @Update
    suspend fun updateUpdate(update: VoiceUpdateEntity)

    @Query("UPDATE voice_updates SET status = :newStatus WHERE id = :id")
    suspend fun updateStatus(id: String, newStatus: String)

    @Query("DELETE FROM voice_updates WHERE id = :id")
    suspend fun deleteUpdate(id: String)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjects(projects: List<ProjectEntity>)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE workerId = :workerId LIMIT 1")
    suspend fun getWorkerById(workerId: String): WorkerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<WorkerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)
}
