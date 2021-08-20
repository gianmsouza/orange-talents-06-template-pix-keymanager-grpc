package br.com.zup.gian.registrarchave.bcb

data class BankAccountResponse(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
)
