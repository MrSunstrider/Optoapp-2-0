package com.example.optoapp.viewmodel

import com.example.optoapp.domain.AppThemeMode
import com.example.optoapp.domain.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: FakeThemeModeStore
    private lateinit var vm: ThemeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeThemeModeStore()
        vm = ThemeViewModel(store)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggle_fromSystemDark_locksLight() = runTest {
        store.mode.value = AppThemeMode.SYSTEM
        vm.toggle(systemDark = true)
        advanceUntilIdle()
        assertEquals(AppThemeMode.LIGHT, store.mode.value)
    }

    @Test
    fun toggle_fromLight_locksDark() = runTest {
        store.mode.value = AppThemeMode.LIGHT
        vm.toggle(systemDark = false)
        advanceUntilIdle()
        assertEquals(AppThemeMode.DARK, store.mode.value)
    }

    private class FakeThemeModeStore : ThemeModeStore {
        override val mode = MutableStateFlow(AppThemeMode.SYSTEM)
        override suspend fun setMode(mode: AppThemeMode) {
            this.mode.value = mode
        }
    }
}
