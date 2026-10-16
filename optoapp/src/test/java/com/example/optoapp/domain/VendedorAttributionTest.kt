package com.example.optoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VendedorAttributionTest {

    @Test
    fun forCreate_usesSignedInUser() {
        assertEquals("uid-1", VendedorAttribution.forCreate("uid-1"))
        assertEquals("uid-1", VendedorAttribution.forCreate("  uid-1  "))
    }

    @Test
    fun forCreate_blankBecomesNull() {
        assertNull(VendedorAttribution.forCreate(null))
        assertNull(VendedorAttribution.forCreate(""))
        assertNull(VendedorAttribution.forCreate("   "))
    }

    @Test
    fun forEdit_keepsPersistedWhenEditOmitsSeller() {
        assertEquals("seller-a", VendedorAttribution.forEdit(null, "seller-a"))
        assertEquals("seller-a", VendedorAttribution.forEdit("  ", "seller-a"))
    }

    @Test
    fun forEdit_prefersExplicitReassignment() {
        assertEquals("seller-b", VendedorAttribution.forEdit("seller-b", "seller-a"))
    }
}
