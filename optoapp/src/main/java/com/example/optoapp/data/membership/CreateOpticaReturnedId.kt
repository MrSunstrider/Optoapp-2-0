package com.example.optoapp.data.membership

object CreateOpticaReturnedId {
    fun persistableId(serverReturned: String?): Result<String> {
        val id = serverReturned?.trim().orEmpty()
        if (id.isBlank()) {
            return Result.failure(IllegalStateException("RPC create_optica_for_current_user returned blank id"))
        }
        return Result.success(id)
    }
}
