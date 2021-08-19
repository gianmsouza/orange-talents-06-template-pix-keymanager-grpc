package br.com.zup.gian.registrarchave.bcb

import br.com.zup.gian.TipoChave
import br.com.zup.gian.registrarchave.bcb.BankAccountRequest
import br.com.zup.gian.registrarchave.bcb.OwnerRequest

class CreatePixKeyRequest(
    val keyType: TipoChave,
    val key: String,
    val bankAccount: BankAccountRequest,
    val owner: OwnerRequest
)
