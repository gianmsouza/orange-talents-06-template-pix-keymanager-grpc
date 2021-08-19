package br.com.zup.gian.registrarchave.bcb

import br.com.zup.gian.registrarchave.bcb.AccountType

class BankAccountRequest(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
)
