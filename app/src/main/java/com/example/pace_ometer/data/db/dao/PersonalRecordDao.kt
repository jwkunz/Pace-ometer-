package com.example.pace_ometer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pace_ometer.data.db.entity.PersonalRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PersonalRecordEntity)

    @Query("SELECT * FROM personal_records WHERE category = :category AND scope = :scope")
    suspend fun get(category: String, scope: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records WHERE scope = :scope")
    fun observeForScope(scope: String): Flow<List<PersonalRecordEntity>>

    @Query("SELECT DISTINCT scope FROM personal_records")
    fun observeScopes(): Flow<List<String>>
}
