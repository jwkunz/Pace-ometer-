package com.example.pace_ometer.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Many-to-many join: a run may use several pieces of equipment, and equipment may be reused across runs. */
@Entity(
    tableName = "run_equipment",
    primaryKeys = ["runId", "equipmentId"],
    foreignKeys = [
        ForeignKey(
            entity = RunEntity::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EquipmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("runId"), Index("equipmentId")]
)
data class RunEquipmentCrossRef(
    val runId: Long,
    val equipmentId: Long
)
