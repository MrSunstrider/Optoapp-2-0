package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration59To60Test {

    @Test
    fun migration_59_60_is_registered() {
        assertEquals(59, MIGRATION_59_60.startVersion)
        assertEquals(60, MIGRATION_59_60.endVersion)
        assertEquals(MIGRATION_59_60, OptoDatabase.MIGRATION_59_60)
    }
}
