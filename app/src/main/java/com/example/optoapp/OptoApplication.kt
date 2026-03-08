package com.example.optoapp

import android.app.Application
import com.example.optoapp.data.OptoDatabase
import com.example.optoapp.data.OptoRepository
import com.example.optoapp.data.SecurityManager

class OptoApplication : Application() {
    val database by lazy { OptoDatabase.getDatabase(this) }
    val repository by lazy { 
        OptoRepository(
            database.pacienteDao(),
            database.evaluacionDao(),
            database.dispensacionDao(),
            database.pagoDao()
        ) 
    }
    val securityManager by lazy { SecurityManager(this) }
}
