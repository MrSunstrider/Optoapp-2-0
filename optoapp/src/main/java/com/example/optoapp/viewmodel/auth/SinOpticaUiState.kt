package com.example.optoapp.viewmodel.auth

data class SinOpticaUiState(
    val waitingMode: Boolean = false,
    val showOwnerForm: Boolean = false,
) {
    fun onOwnerCreateAction(): SinOpticaUiState =
        copy(showOwnerForm = true, waitingMode = false)

    fun onEmployeeWaitAction(): SinOpticaUiState =
        copy(waitingMode = true, showOwnerForm = false)

    fun presentsOwnerCreateForm(): Boolean = showOwnerForm && !waitingMode

    fun treatsEmptyMembershipsAsCompletedSelection(): Boolean = false
}
