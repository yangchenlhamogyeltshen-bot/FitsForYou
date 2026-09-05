package com.example.fitsforyou.database

import androidx.room.*
import com.example.fitsforyou.model.Clothing
import kotlinx.coroutines.flow.Flow

@Dao
interface ClothingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(clothing: Clothing)

    @Update
    suspend fun update(clothing: Clothing)

    @Delete
    suspend fun delete(clothing: Clothing)

    @Query("SELECT * FROM clothing WHERE userId = :userId ORDER BY name ASC")
    fun getAllClothing(userId: String): Flow<List<Clothing>>

    @Query("SELECT * FROM clothing WHERE id = :id")
    suspend fun getClothingById(id: Int): Clothing?

    @Query("SELECT * FROM clothing WHERE userId = :userId AND category = :category ORDER BY name ASC")
    fun getClothingByCategory(userId: String, category: String): Flow<List<Clothing>>

    @Query("UPDATE clothing SET timesWorn = :timesWorn, lastWorn = :lastWorn WHERE id = :id")
    suspend fun markAsWorn(id: Int, lastWorn: Long, timesWorn: Int)

    @Query("SELECT COUNT(*) FROM clothing WHERE userId = :userId")
    fun countAllClothing(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM clothing WHERE userId = :userId AND isCapsule = 1")
    fun countCapsuleClothing(userId: String): Flow<Int>

    @Query("SELECT * FROM clothing WHERE userId = :userId ORDER BY timesWorn DESC LIMIT 1")
    fun getMostWornClothing(userId: String): Flow<Clothing?>

    @Query("SELECT * FROM clothing WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getRecentlyAddedClothing(userId: String): Flow<Clothing?>
    @Query("SELECT * FROM clothing WHERE userId = :userId ORDER BY timesWorn ASC LIMIT 1")
    fun getLeastWornClothing(userId: String): Flow<Clothing?>

    @Query("SELECT COUNT(*) FROM clothing WHERE userId = :userId AND timesWorn = 0")
    fun countNeverWornClothing(userId: String): Flow<Int>

    @Query("SELECT SUM(timesWorn) FROM clothing WHERE userId = :userId")
    fun getTotalWearCount(userId: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM clothing WHERE userId = :userId AND timesWorn > 0")
    fun countItemsWornAtLeastOnce(userId: String): Flow<Int>
}
