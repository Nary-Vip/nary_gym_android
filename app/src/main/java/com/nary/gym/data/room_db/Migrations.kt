package com.nary.gym.data.room_db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `gym_visits` (" +
                "`id` TEXT NOT NULL, " +
                "`checkInAt` INTEGER NOT NULL, " +
                "`checkOutAt` INTEGER, " +
                "`source` TEXT NOT NULL, " +
                "`autoClosed` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_visits_checkInAt` ON `gym_visits` (`checkInAt`)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `serverId` TEXT")
        db.execSQL("UPDATE `workouts` SET `serverId` = `id`")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `syncState` TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `version` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `syncError` TEXT")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workouts` ADD COLUMN `ownerId` TEXT")
    }
}
