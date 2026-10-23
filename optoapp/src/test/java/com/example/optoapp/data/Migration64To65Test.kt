package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration64To65Test {
    @Test
    fun migration_versions_match() {
        assertEquals(64, MIGRATION_64_65.startVersion)
        assertEquals(65, MIGRATION_64_65.endVersion)
        assertEquals(MIGRATION_64_65, OptoDatabase.MIGRATION_64_65)
    }
}
