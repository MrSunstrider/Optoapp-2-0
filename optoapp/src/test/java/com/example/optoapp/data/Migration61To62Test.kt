package com.example.optoapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class Migration61To62Test {

    @Test
    fun migration_61_62_is_registered() {
        assertEquals(61, MIGRATION_61_62.startVersion)
        assertEquals(62, MIGRATION_61_62.endVersion)
        assertEquals(MIGRATION_61_62, OptoDatabase.MIGRATION_61_62)
    }
}
