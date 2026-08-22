package com.example.pace_ometer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunEquipmentCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface EquipmentDao {
    @Insert
    suspend fun insert(equipment: EquipmentEntity): Long

    @Update
    suspend fun update(equipment: EquipmentEntity)

    @Query("SELECT * FROM equipment ORDER BY retired ASC, createdAtEpochMs DESC")
    fun observeAll(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment WHERE id = :id")
    fun observeById(id: Long): Flow<EquipmentEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(crossRef: RunEquipmentCrossRef)

    @Query("DELETE FROM run_equipment WHERE runId = :runId AND equipmentId = :equipmentId")
    suspend fun unassign(runId: Long, equipmentId: Long)

    @Query(
        """
        SELECT runs.* FROM runs
        INNER JOIN run_equipment ON runs.id = run_equipment.runId
        WHERE run_equipment.equipmentId = :equipmentId AND runs.isSaved = 1
        ORDER BY runs.startTimeEpochMs DESC
        """
    )
    fun observeRunsForEquipment(equipmentId: Long): Flow<List<RunEntity>>

    @Query(
        """
        SELECT equipment.* FROM equipment
        INNER JOIN run_equipment ON equipment.id = run_equipment.equipmentId
        WHERE run_equipment.runId = :runId
        """
    )
    fun observeEquipmentForRun(runId: Long): Flow<List<EquipmentEntity>>

    @Query(
        """
        SELECT startingDistanceMeters + COALESCE((
            SELECT SUM(runs.totalDistanceMeters) FROM runs
            INNER JOIN run_equipment ON runs.id = run_equipment.runId
            WHERE run_equipment.equipmentId = equipment.id AND runs.isSaved = 1
        ), 0.0)
        FROM equipment WHERE id = :equipmentId
        """
    )
    fun observeTotalDistanceMeters(equipmentId: Long): Flow<Double?>
}
