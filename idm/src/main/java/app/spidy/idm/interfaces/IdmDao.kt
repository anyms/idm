package app.spidy.idm.interfaces

import androidx.room.*
import app.spidy.idm.data.Snapshot

@Dao
interface IdmDao {
    @Query("SELECT * FROM snapshot")
    fun getSnapshots(): List<Snapshot>

    @Query("SELECT * FROM snapshot WHERE uId = :uId")
    fun getSnapshot(uId: String): Snapshot

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun putSnapshot(snapshot: Snapshot)

    @Update
    fun updateSnapshot(snapshot: Snapshot)

    @Delete
    fun removeSnapshot(snapshot: Snapshot)

    @Query("DELETE FROM snapshot")
    fun clearAllSnapshots()
}