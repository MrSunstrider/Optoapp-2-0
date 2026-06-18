package com.example.optoapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.optoapp.data.arqueo.IArqueoCajaRepo
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Inject

/**
 * Structural verification that ArqueoCajaViewModel is properly wired for Hilt DI.
 *
 * NOTE: @HiltViewModel has @Retention(CLASS) so it is NOT visible at
 * runtime via reflection. We verify Hilt integration indirectly by
 * checking the @Inject constructor and IArqueoCajaRepo parameter.
 */
class ArqueoCajaViewModelHiltWiringTest {

    @Test
    fun `ArqueoCajaViewModel extends ViewModel`() {
        assertTrue(
            "ArqueoCajaViewModel must extend ViewModel",
            ViewModel::class.java.isAssignableFrom(ArqueoCajaViewModel::class.java)
        )
    }

    @Test
    fun `ArqueoCajaViewModel constructor accepts IArqueoCajaRepo`() {
        val constructors = ArqueoCajaViewModel::class.java.declaredConstructors
        assertTrue("ArqueoCajaViewModel must have at least one constructor", constructors.isNotEmpty())
        val primaryCtor = constructors.first()
        val hasRepoParam = primaryCtor.parameterTypes.any {
            IArqueoCajaRepo::class.java.isAssignableFrom(it)
        }
        assertTrue("Constructor must accept IArqueoCajaRepo", hasRepoParam)
    }

    @Test
    fun `ArqueoCajaViewModel constructor is annotated with Inject`() {
        val constructors = ArqueoCajaViewModel::class.java.declaredConstructors
        val primaryCtor = constructors.first()
        val injectAnnotation = primaryCtor.getAnnotation(Inject::class.java)
        assertNotNull("Constructor must be annotated with @Inject", injectAnnotation)
    }
}
