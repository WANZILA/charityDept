//package com.example.charityDept.data.sources.local
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import androidx.room.TypeConverters
//import com.example.charityDept.core.Utils.Converters
//import com.example.charityDept.data.model.Child
//
//@Database(entities = [Child::class],   version = 1, exportSchema = false)
//@TypeConverters(Converters::class)
//abstract class AppDatabase : RoomDatabase() {
//
//    abstract  fun childrenDao(): ChildrenDao
//    companion object {
//        @Volatile
//        private  var INSTANCE : AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase? {
//            if (INSTANCE == null){
//                synchronized(AppDatabase::class.java){
//                    if (INSTANCE == null){
//                        INSTANCE = Room.databaseBuilder(
//                                                context.applicationContext,
//                                                AppDatabase::class.java, "zion_kids_db"
//                                            ).fallbackToDestructiveMigration(false).build()
//                    }
//                }
//            }
//            return INSTANCE
//        }
//
//    }
//}
