package com.practice.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.practice.database.common.DATABASE_FEED
import com.practice.database.dao.MyFeedDao
import com.practice.database.entity.MyFeedEntity

@Database(
    entities = [
        MyFeedEntity::class
    ],
    version = 1
)
@TypeConverters(ListConverter::class)
abstract class FeedDatabase : RoomDatabase() {
    abstract val myFeedDao: MyFeedDao

    companion object {
        @Volatile // makes the field immediately made visible to other threads
        private var INSTANCE: FeedDatabase? = null

//        @RenameColumn(
//            tableName = "subscriber_data_table",
//            fromColumnName = "subscriber_id",
//            toColumnName = "subscriber_id_revised"
//        )
//        class Migration1To2: AutoMigrationSpec
//
//        @RenameColumn(
//            tableName = "subscriber_data_table",
//            fromColumnName = "subscriber_name",
//            toColumnName = "subscriber_name_revised"
//        )
//        class Migration2To3: AutoMigrationSpec

        fun getInstance(context: Context): FeedDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context,
                        FeedDatabase::class.java,
                        DATABASE_FEED
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

class ListConverter {
    @TypeConverter
    fun listToJson(value: List<String>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun jsonToList(value: String): List<String>? {
        return Gson().fromJson(value,Array<String>::class.java)?.toList()
    }
}