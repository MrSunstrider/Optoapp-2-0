package com.example.optoapp.viewmodel.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SinOpticaUiStateTest {

    @Test
    fun ownerAction_opensCreateForm_notSelector() {
        val next = SinOpticaUiState().onOwnerCreateAction()
        assertTrue(next.presentsOwnerCreateForm())
        assertFalse(next.waitingMode)
        assertFalse(next.treatsEmptyMembershipsAsCompletedSelection())
        assertEquals(false, next.waitingMode)
    }

    @Test
    fun employeeWait_doesNotOpenOwnerForm() {
        val next = SinOpticaUiState().onEmployeeWaitAction()
        assertFalse(next.presentsOwnerCreateForm())
        assertTrue(next.waitingMode)
    }
}
