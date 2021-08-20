package br.com.zup.gian.registrarchave.bcb

import br.com.zup.gian.TipoChave

data class CreatePixKeyResponse(
    val keyType: TipoChave,
    val key: String,
    val bankAccount: BankAccountResponse,
    val owner: OwnerResponse,
    val createdAt: String
)
