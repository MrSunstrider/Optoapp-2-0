package com.example.optoapp.viewmodel.auth

import com.example.optoapp.ui.navigation.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PostLoginNavigationTest {

    @Test
    fun sizeOne_skipsSelector_goesPinWhenRequired() {
        val dest = PostLoginNavigation.dest(
            count = 1,
            needsOnboarding = false,
            fetchError = false,
            isPinRequired = true,
            pinHasBeenSet = false,
        )

        assertEquals(Route.CreatePin.route, dest)
        assertNotEquals(Route.SeleccionOptica.route, dest)
    }

    @Test
    fun requiredAndPinSet_goesPin() {
        val dest = PostLoginNavigation.dest(
            count = 1,
            needsOnboarding = false,
            fetchError = false,
            isPinRequired = true,
            pinHasBeenSet = true,
        )

        assertEquals(Route.Pin.route, dest)
    }

    @Test
    fun sizeOne_skipsSelector_goesMainWhenPinNotRequired() {
        val dest = PostLoginNavigation.dest(
            count = 1,
            needsOnboarding = false,
            fetchError = false,
            isPinRequired = false,
            pinHasBeenSet = true,
        )

        assertEquals(Route.Main.route, dest)
        assertNotEquals(Route.SeleccionOptica.route, dest)
    }

    @Test
    fun sizeGreaterThanOne_goesSelector() {
        val dest = PostLoginNavigation.dest(
            count = 2,
            needsOnboarding = false,
            fetchError = false,
            isPinRequired = false,
            pinHasBeenSet = false,
        )

        assertEquals(Route.SeleccionOptica.route, dest)
    }

    @Test
    fun empty_withoutFetchError_goesSinOptica() {
        val dest = PostLoginNavigation.dest(
            count = 0,
            needsOnboarding = true,
            fetchError = false,
            isPinRequired = false,
            pinHasBeenSet = false,
        )

        assertEquals(Route.SinOptica.route, dest)
    }

    @Test
    fun fetchError_doesNotGoSinOptica() {
        val dest = PostLoginNavigation.dest(
            count = 0,
            needsOnboarding = true,
            fetchError = true,
            isPinRequired = false,
            pinHasBeenSet = false,
        )

        assertNotEquals(Route.SinOptica.route, dest)
        assertNotEquals(Route.SeleccionOptica.route, dest)
    }
}
