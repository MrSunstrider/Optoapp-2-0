package com.example.optoapp.viewmodel

import com.example.optoapp.data.ISecurityManager
import com.example.optoapp.data.ISessionManager
import com.example.optoapp.data.SecurityManager
import com.example.optoapp.viewmodel.auth.PinDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PinDelegate — PIN validation, creation, update, and input management.
 *
 * Dependencies are fakes: [FakeSecurityManager], [FakeSessionManager].
 */
class PinDelegateTest {

    private class FakeSecurityManager : ISecurityManager {
        private val _userPin = MutableStateFlow("")
        private val _pinHasBeenSet = MutableStateFlow(false)
        override val userPin: Flow<String> = _userPin.asStateFlow()
        override val pinHasBeenSet: Flow<Boolean> = _pinHasBeenSet.asStateFlow()
        override suspend fun savePin(pin: String) {
            _userPin.value = pin
            _pinHasBeenSet.value = pin.isNotEmpty()
        }
    }

    private class FakeSessionManager : ISessionManager {
        private val _pinHasBeenSet = MutableStateFlow(false)
        private val _isPinRequired = MutableStateFlow(true)
        override val isLoggedIn: Flow<Boolean> = MutableStateFlow(false)
        override val opticaId: Flow<String> = MutableStateFlow("test_optica")
        override val opticaRol: Flow<String> = MutableStateFlow("admin")
        override val userEmail: Flow<String> = MutableStateFlow("")
        override val userName: Flow<String> = MutableStateFlow("")
        override val userTimeZone: Flow<String?> = MutableStateFlow(null)
        override val lastLoginTimestamp: Flow<Long> = MutableStateFlow(0L)
        override val pinHasBeenSet: Flow<Boolean> = _pinHasBeenSet.asStateFlow()
        override val isPinRequired: Flow<Boolean> = _isPinRequired.asStateFlow()
        override suspend fun saveSession(opticaId: String, email: String, name: String, rol: String) {}
        override suspend fun clearSession() {}
        override suspend fun setPinRequired(required: Boolean) {
            _isPinRequired.value = required
        }
        override suspend fun saveRememberedEmail(email: String) {}
        override suspend fun getRememberedEmail(): String = ""
        override suspend fun clearRememberedEmail() {}
    }

    @Test
    fun `onPinDigit appends digit to pinInput`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        delegate.onPinDigit("5")
        assertEquals("5", delegate.pinInput.value)
    }

    @Test
    fun `onPinDigit appends multiple digits up to PIN_LENGTH`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        for (d in listOf("1", "2", "3", "4", "5", "6")) {
            delegate.onPinDigit(d)
        }
        assertEquals("123456", delegate.pinInput.value)
    }

    @Test
    fun `onPinDigit rejects digits beyond PIN_LENGTH`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        for (d in listOf("1", "2", "3", "4", "5", "6")) {
            delegate.onPinDigit(d)
        }
        // pinInput should have exactly PIN_LENGTH chars
        assertEquals(SecurityManager.PIN_LENGTH, delegate.pinInput.value.length)
        // trying to add another digit should have no effect
        delegate.onPinDigit("7")
        assertEquals(SecurityManager.PIN_LENGTH, delegate.pinInput.value.length)
        assertEquals("123456", delegate.pinInput.value)
    }

    @Test
    fun `onPinDigit does nothing on empty digit`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        delegate.onPinDigit("")
        assertEquals("", delegate.pinInput.value)
    }

    @Test
    fun `clearPin resets pinInput to empty`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        delegate.onPinDigit("1")
        delegate.onPinDigit("2")
        delegate.onPinDigit("3")
        assertEquals("123", delegate.pinInput.value)
        delegate.clearPin()
        assertEquals("", delegate.pinInput.value)
    }

    @Test
    fun `clearPin from empty remains empty`() {
        val delegate = PinDelegate(FakeSecurityManager(), FakeSessionManager())
        delegate.clearPin()
        assertEquals("", delegate.pinInput.value)
    }

    @Test
    fun `validatePin matching input returns true`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        for (d in listOf("1", "2", "3", "7", "8", "9")) {
            delegate.onPinDigit(d)
        }
        assertTrue(delegate.validatePin())
    }

    @Test
    fun `validatePin nonMatching input returns false`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        for (d in listOf("9", "9", "9", "9", "9", "9")) {
            delegate.onPinDigit(d)
        }
        assertFalse(delegate.validatePin())
    }

    @Test
    fun `validatePin empty input when pin is set returns false`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        // pinInput empty by default
        assertFalse(delegate.validatePin())
    }

    @Test
    fun `validatePin both empty returns true`() = runTest {
        val sec = FakeSecurityManager()
        // userPin is "" by default
        val delegate = PinDelegate(sec, FakeSessionManager())
        // pinInput is "" by default
        assertTrue(delegate.validatePin())
    }

    @Test
    fun `createPin valid pin saves`() = runTest {
        val sec = FakeSecurityManager()
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.createPin("789123")
        assertEquals("789123", sec.userPin.first())
    }

    @Test
    fun `createPin invalid pin does not save`() = runTest {
        val sec = FakeSecurityManager()
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.createPin("12") // too short
        assertEquals("", sec.userPin.first())
    }

    @Test
    fun `createPin empty pin does not save`() = runTest {
        val sec = FakeSecurityManager()
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.createPin("")
        assertEquals("", sec.userPin.first())
    }

    @Test
    fun `createPin weak sequential pattern does not save`() = runTest {
        val sec = FakeSecurityManager()
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.createPin("123456") // weak pattern
        assertEquals("", sec.userPin.first())
    }

    @Test
    fun `updatePin with correct oldPin saves newPin`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.updatePin("123789", "456123")
        assertEquals("456123", sec.userPin.first())
    }

    @Test
    fun `updatePin with wrong oldPin does not save`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.updatePin("wrong", "456123")
        assertEquals("123789", sec.userPin.first()) // unchanged
    }

    @Test
    fun `updatePin with invalid newPin does not save`() = runTest {
        val sec = FakeSecurityManager()
        sec.savePin("123789")
        val delegate = PinDelegate(sec, FakeSessionManager())
        delegate.updatePin("123789", "12") // too short
        assertEquals("123789", sec.userPin.first()) // unchanged
    }

    @Test
    fun `togglePinRequired disabled sets isPinRequired to false`() = runTest {
        val sess = FakeSessionManager()
        val delegate = PinDelegate(FakeSecurityManager(), sess)
        assertTrue(sess.isPinRequired.first())
        delegate.togglePinRequired(false)
        assertFalse(sess.isPinRequired.first())
    }

    @Test
    fun `togglePinRequired enabled sets isPinRequired to true`() = runTest {
        val sess = FakeSessionManager()
        val delegate = PinDelegate(FakeSecurityManager(), sess)
        delegate.togglePinRequired(false)
        delegate.togglePinRequired(true)
        assertTrue(sess.isPinRequired.first())
    }

    @Test
    fun `pinHasBeenSet delegates to securityManager`() {
        val sec = FakeSecurityManager()
        val delegate = PinDelegate(sec, FakeSessionManager())
        assertNotNull(delegate.pinHasBeenSet)
    }

    @Test
    fun `isPinRequired delegates to sessionManager`() {
        val sess = FakeSessionManager()
        val delegate = PinDelegate(FakeSecurityManager(), sess)
        assertNotNull(delegate.isPinRequired)
    }
}
