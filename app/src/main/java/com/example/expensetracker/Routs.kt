package com.example.expensetracker

import kotlinx.serialization.Serializable

sealed class Routs {
    @Serializable
    object Home

    @Serializable
    data class AddEdit(val transactionId: Int = -1)
}
