package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration62To63Test {

    @Test
    fun migration_62_63_is_registered() {
        assertEquals(62, MIGRATION_62_63.startVersion)
        assertEquals(63, MIGRATION_62_63.endVersion)
        assertEquals(MIGRATION_62_63, OptoDatabase.MIGRATION_62_63)
    }
}
