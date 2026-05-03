package com.alvaronolasco.creditcardtracker.data.dao

import androidx.room.*
import com.alvaronolasco.creditcardtracker.data.entity.NotificationConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationConfigDao {
    @Query("SELECT * FROM notification_configs WHERE cardId = :cardId")
    fun getConfigsByCard(cardId: Int): Flow<List<NotificationConfig>>

    @Query("SELECT * FROM notification_configs WHERE id = :id")
    suspend fun getConfigById(id: Int): NotificationConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigs(configs: List<NotificationConfig>)

    @Update
    suspend fun updateConfig(config: NotificationConfig)

    @Delete
    suspend fun deleteConfig(config: NotificationConfig)
}
