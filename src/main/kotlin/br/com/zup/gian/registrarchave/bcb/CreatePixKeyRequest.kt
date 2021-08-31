package br.com.zup.gian.registrarchave.bcb

import br.com.zup.gian.TipoChave
import br.com.zup.gian.registrarchave.bcb.BankAccountRequest
import br.com.zup.gian.registrarchave.bcb.OwnerRequest

data class CreatePixKeyRequest(
    val keyType: TipoChave,
    val key: String,
    val bankAccount: BankAccountRequest,
    val owner: OwnerRequest
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreatePixKeyRequest

        if (keyType != other.keyType) return false
        if (key != other.key) return false
        if (bankAccount != other.bankAccount) return false
        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyType.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + bankAccount.hashCode()
        result = 31 * result + owner.hashCode()
        return result
    }
}
