package com.pettpro.domain.db.model


data class User(
    val id: String = "",
    val name: String = "",
    val login: String = "",
    val password: String = "",
    val arrayOfExpence: List<Expence> = emtyList(),
    val arrayOfIncomes: List<Income> = emtyList()
)


