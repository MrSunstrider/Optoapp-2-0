package com.example.optoapp.data.arqueo

interface IArqueoCajaRepo {
    suspend fun insertArqueo(arqueo: ArqueoCaja)
}
