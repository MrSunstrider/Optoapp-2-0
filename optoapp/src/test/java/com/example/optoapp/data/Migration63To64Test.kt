package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration63To64Test {

    @Test
    fun migration_63_64_is_registered() {
        assertEquals(63, MIGRATION_63_64.startVersion)
        assertEquals(64, MIGRATION_63_64.endVersion)
        assertEquals(MIGRATION_63_64, OptoDatabase.MIGRATION_63_64)
    }
}
