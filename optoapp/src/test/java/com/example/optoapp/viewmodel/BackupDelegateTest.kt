package com.example.optoapp.viewmodel

import com.example.optoapp.viewmodel.auth.BackupDelegate
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BackupDelegate pure validation logic extracted to companion object methods.
 *
 * - [BackupDelegate.checkExportAdmin]
 * - [BackupDelegate.checkRestoreAdmin]
 * - [BackupDelegate.validateSourceOpticaId]
 * - [BackupDelegate.validateOpticaIdMatch]
 *
 * Supabase RPC and repository persistence require integration tests.
 */
class BackupDelegateTest {

    @Test
    fun `checkExportAdmin nonAdmin returns error message`() {
        val msg = BackupDelegate.checkExportAdmin("user")
        assertNotNull(msg)
        assertTrue(msg!!.contains("admin"))
    }

    @Test
    fun `checkExportAdmin admin returns null`() {
        assertNull(BackupDelegate.checkExportAdmin("admin"))
    }

    @Test
    fun `checkExportAdmin gerente returns error message`() {
        assertNotNull(BackupDelegate.checkExportAdmin("gerente"))
    }

    @Test
    fun `checkExportAdmin viewer returns error message`() {
        assertNotNull(BackupDelegate.checkExportAdmin("viewer"))
    }

    @Test
    fun `checkExportAdmin uppercase ADMIN returns null`() {
        assertNull(BackupDelegate.checkExportAdmin("ADMIN"))
    }

    @Test
    fun `checkExportAdmin mixed case Admin returns null`() {
        assertNull(BackupDelegate.checkExportAdmin("Admin"))
    }

    @Test
    fun `checkExportAdmin with whitespace returns null`() {
        assertNull(BackupDelegate.checkExportAdmin("  admin  "))
    }

    @Test
    fun `checkExportAdmin empty string returns error`() {
        assertNotNull(BackupDelegate.checkExportAdmin(""))
    }

    @Test
    fun `checkRestoreAdmin nonAdmin returns error message`() {
        val msg = BackupDelegate.checkRestoreAdmin("user")
        assertNotNull(msg)
        assertTrue(msg!!.contains("admin"))
    }

    @Test
    fun `checkRestoreAdmin admin returns null`() {
        assertNull(BackupDelegate.checkRestoreAdmin("admin"))
    }

    @Test
    fun `checkRestoreAdmin gerente returns error message`() {
        assertNotNull(BackupDelegate.checkRestoreAdmin("gerente"))
    }

    @Test
    fun `validateSourceOpticaId null returns error message`() {
        assertNotNull(BackupDelegate.validateSourceOpticaId(null))
    }

    @Test
    fun `validateSourceOpticaId blank returns error message`() {
        assertNotNull(BackupDelegate.validateSourceOpticaId(""))
    }

    @Test
    fun `validateSourceOpticaId whitespace returns error message`() {
        assertNotNull(BackupDelegate.validateSourceOpticaId("   "))
    }

    @Test
    fun `validateSourceOpticaId valid returns null`() {
        assertNull(BackupDelegate.validateSourceOpticaId("opt_abc123"))
    }

    @Test
    fun `validateOpticaIdMatch matching returns null`() {
        assertNull(BackupDelegate.validateOpticaIdMatch("opt_abc", "opt_abc"))
    }

    @Test
    fun `validateOpticaIdMatch different returns error message`() {
        assertNotNull(BackupDelegate.validateOpticaIdMatch("opt_abc", "opt_xyz"))
    }

    @Test
    fun `validateOpticaIdMatch case sensitive rejects different case`() {
        assertNotNull(BackupDelegate.validateOpticaIdMatch("OPT_ABC", "opt_abc"))
    }

    @Test
    fun `validateOpticaIdMatch error message mentions optica`() {
        val msg = BackupDelegate.validateOpticaIdMatch("opt_abc", "opt_xyz")
        assertNotNull(msg)
        assertTrue(msg!!.contains("óptica"))
    }
}
