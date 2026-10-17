package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration58To59Test {

    @Test
    fun migration_58_59_is_registered() {
        assertEquals(58, MIGRATION_58_59.startVersion)
        assertEquals(59, MIGRATION_58_59.endVersion)
        assertEquals(MIGRATION_58_59, OptoDatabase.MIGRATION_58_59)
    }
}
