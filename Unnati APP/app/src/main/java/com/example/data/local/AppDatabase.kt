package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [VoiceUpdateEntity::class, ProjectEntity::class, WorkerEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voiceUpdateDao(): VoiceUpdateDao
    abstract fun projectDao(): ProjectDao
    abstract fun workerDao(): WorkerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "time_agent_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database)
                    }
                }
            }

            suspend fun populateDatabase(database: AppDatabase) {
                val projectDao = database.projectDao()
                val workerDao = database.workerDao()
                val updateDao = database.voiceUpdateDao()

                // Default Projects
                val projects = listOf(
                    ProjectEntity(
                        id = "PRJ-01",
                        name = "OIL Pipeline Expansion",
                        code = "OPE-24",
                        location = "Barmer-Salaya Corridor, Rajasthan",
                        client = "Oil India Ltd / Bharat Petro Infra",
                        activeWorkersCount = 164
                    ),
                    ProjectEntity(
                        id = "PRJ-02",
                        name = "Mumbai Metro Line 3",
                        code = "MML-3",
                        location = "Colaba-Bandra-SEEPZ, Mumbai",
                        client = "MMRC / L&T Infra",
                        activeWorkersCount = 148
                    ),
                    ProjectEntity(
                        id = "PRJ-03",
                        name = "Bandra-Worli Sea Link Phase 2",
                        code = "BWSL-2",
                        location = "Worli Sea Face, Mumbai",
                        client = "MSRDC",
                        activeWorkersCount = 92
                    ),
                    ProjectEntity(
                        id = "PRJ-04",
                        name = "Navi Mumbai Airport Terminal 2",
                        code = "NMI-T2",
                        location = "Ulwe, Navi Mumbai",
                        client = "CIDCO / Adani Airports",
                        activeWorkersCount = 310
                    )
                )
                projectDao.insertProjects(projects)

                // Default Workers
                val workers = listOf(
                    WorkerEntity(
                        workerId = "WK-10245",
                        name = "Rajesh Sharma",
                        role = "Site Supervisor",
                        department = "Piping & Field Operations",
                        phoneNumber = "+91 98201 45892",
                        assignedProjectId = "PRJ-01",
                        shift = "Morning Shift (07:00 - 15:30)"
                    ),
                    WorkerEntity(
                        workerId = "WK-10882",
                        name = "Amit Patel",
                        role = "Quality Inspector",
                        department = "QA / QC Inspection",
                        phoneNumber = "+91 98765 43210",
                        assignedProjectId = "PRJ-01",
                        shift = "General Shift (09:00 - 18:00)"
                    ),
                    WorkerEntity(
                        workerId = "WK-11409",
                        name = "Suresh Yadav",
                        role = "Safety Marshal",
                        department = "HSE & Site Safety",
                        phoneNumber = "+91 97112 33445",
                        assignedProjectId = "PRJ-01",
                        shift = "Day Shift (08:00 - 17:00)"
                    )
                )
                workerDao.insertWorkers(workers)
                // Seeding of updates removed for clean initialization.
            }
        }
    }
}
