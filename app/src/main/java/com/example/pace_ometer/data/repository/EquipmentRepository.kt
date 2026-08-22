package com.example.pace_ometer.data.repository

import com.example.pace_ometer.data.db.dao.EquipmentDao
import com.example.pace_ometer.data.db.entity.EquipmentEntity
import com.example.pace_ometer.data.db.entity.RunEntity
import com.example.pace_ometer.data.db.entity.RunEquipmentCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EquipmentRepository(private val equipmentDao: EquipmentDao) {

    fun observeAll(): Flow<List<EquipmentEntity>> = equipmentDao.observeAll()

    fun observeById(id: Long): Flow<EquipmentEntity?> = equipmentDao.observeById(id)

    fun observeAssignedRuns(equipmentId: Long): Flow<List<RunEntity>> =
        equipmentDao.observeRunsForEquipment(equipmentId)

    fun observeEquipmentForRun(runId: Long): Flow<List<EquipmentEntity>> =
        equipmentDao.observeEquipmentForRun(runId)

    fun observeTotalDistanceMeters(equipmentId: Long): Flow<Double> =
        equipmentDao.observeTotalDistanceMeters(equipmentId).map { it ?: 0.0 }

    suspend fun addEquipment(name: String, type: String, startingDistanceMeters: Double): Long =
        equipmentDao.insert(
            EquipmentEntity(
                name = name,
                type = type,
                startingDistanceMeters = startingDistanceMeters,
                createdAtEpochMs = System.currentTimeMillis()
            )
        )

    suspend fun updateEquipment(equipment: EquipmentEntity) = equipmentDao.update(equipment)

    suspend fun setRetired(equipment: EquipmentEntity, retired: Boolean) =
        equipmentDao.update(
            equipment.copy(
                retired = retired,
                retiredAtEpochMs = if (retired) System.currentTimeMillis() else null
            )
        )

    suspend fun assignRun(runId: Long, equipmentId: Long) =
        equipmentDao.assign(RunEquipmentCrossRef(runId, equipmentId))

    suspend fun unassignRun(runId: Long, equipmentId: Long) = equipmentDao.unassign(runId, equipmentId)
}
