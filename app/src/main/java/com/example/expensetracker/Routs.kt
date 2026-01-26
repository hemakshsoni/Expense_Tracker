package com.example.expensetracker

import kotlinx.serialization.Serializable

sealed class Routs {

    @Serializable
    object Setup // Add this new route
    @Serializable
    object Home

    @Serializable
    object Review

    @Serializable
    object Analytics

    @Serializable
    object Activity

    @Serializable
    object More

    @Serializable
    data class AddEdit(val transactionId: Int = -1, val initialTab: Int = 0)

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
