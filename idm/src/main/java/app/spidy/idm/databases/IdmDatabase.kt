package app.spidy.idm.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.spidy.idm.converters.StringArrayListConverter
import app.spidy.idm.data.Snapshot
import app.spidy.idm.interfaces.IdmDao

@Database(entities = [Snapshot::class], version = 1, exportSchema = false)
@TypeConverters(StringArrayListConverter::class)
abstract class IdmDatabase: RoomDatabase() {
    abstract fun idmDao(): IdmDao
}