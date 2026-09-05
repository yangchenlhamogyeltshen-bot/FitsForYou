package com.example.fitsforyou.database

import androidx.room.*
import com.example.fitsforyou.model.Outfit
import com.example.fitsforyou.model.OutfitClothingCrossRef
import com.example.fitsforyou.model.OutfitWithClothing
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: Outfit): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfitClothingCrossRef(crossRef: OutfitClothingCrossRef)

    @Update
    suspend fun updateOutfit(outfit: Outfit)

    @Transaction
    @Query("SELECT * FROM outfits WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOutfitsWithClothing(userId: String): Flow<List<OutfitWithClothing>>

    @Delete
    suspend fun deleteOutfit(outfit: Outfit)

    @Query("DELETE FROM OutfitClothingCrossRef WHERE id = :outfitId")
    suspend fun deleteCrossRefsForOutfit(outfitId: Long)

    @Query("DELETE FROM OutfitClothingCrossRef WHERE clothingId = :itemId")
    suspend fun deleteCrossRefsForItem(itemId: Int)

    @Transaction
    suspend fun deleteOutfitWithRefs(outfit: Outfit) {
        deleteCrossRefsForOutfit(outfit.id)
        deleteOutfit(outfit)
    }

    @Transaction
    suspend fun insertOutfitWithItems(outfit: Outfit, clothingIds: List<Int>) {
        val outfitId = insertOutfit(outfit)
        clothingIds.forEach { clothingId ->
            insertOutfitClothingCrossRef(OutfitClothingCrossRef(outfitId, clothingId))
        }
    }

    @Query("SELECT COUNT(*) FROM outfits WHERE userId = :userId")
    fun countTotalOutfits(userId: String): Flow<Int>
}
