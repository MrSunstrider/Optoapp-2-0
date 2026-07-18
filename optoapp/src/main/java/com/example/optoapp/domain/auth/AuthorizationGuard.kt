package com.example.optoapp.domain.auth

object AuthorizationGuard {
    fun requireRole(actualRole: String, requiredRoles: Set<String>, operation: String) {
        val normalized = actualRole.trim().lowercase()
        require(normalized in requiredRoles) {
            "Unauthorized: role '$normalized' cannot perform '$operation'"
        }
    }
}
