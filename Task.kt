package id.memotugas.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val course: String,
    val title: String,
    val description: String,
    val deadline: Long,
    val instructionUri: String? = null,
    val resultUri: String? = null,
    val status: String = "Belum dikerjakan"
)
