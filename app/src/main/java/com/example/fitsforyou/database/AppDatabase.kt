package com.example.fitsforyou.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fitsforyou.model.Clothing
import com.example.fitsforyou.model.Outfit
import com.example.fitsforyou.model.OutfitClothingCrossRef
import com.example.fitsforyou.model.WearEvent
import com.example.fitsforyou.model.User

@Database(
    entities = [Clothing::class, Outfit::class, OutfitClothingCrossRef::class, WearEvent::class, User::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clothingDao(): ClothingDao
    abstract fun outfitDao(): OutfitDao
    abstract fun wearEventDao(): WearEventDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitsforyou_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
