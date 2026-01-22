package com.example.expensetracker

import kotlinx.serialization.Serializable

sealed class Routs {
    @Serializable
    object Home

    @Serializable
    data class AddEdit(val transactionId: Int = -1)

    @Serializable
    object Analytics

    @Serializable
    object PaymentMethods

    @Serializable
    object AutoLearned

    @Serializable
    object Categories

    @Serializable
    object RecurringPayments

    @Serializable
    object Dues
}