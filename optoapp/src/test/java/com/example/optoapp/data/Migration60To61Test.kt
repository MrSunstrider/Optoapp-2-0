package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration60To61Test {

    @Test
    fun migration_60_61_is_registered() {
        assertEquals(60, MIGRATION_60_61.startVersion)
        assertEquals(61, MIGRATION_60_61.endVersion)
        assertEquals(MIGRATION_60_61, OptoDatabase.MIGRATION_60_61)
    }
}
