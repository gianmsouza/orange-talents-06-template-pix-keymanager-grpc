package br.com.zup.gian.consultarchave

import br.com.zup.gian.ConsultaChavePixRequest
import br.com.zup.gian.ConsultaChavePixResponse
import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import br.com.zup.gian.registrarchave.bcb.AccountType
import br.com.zup.gian.registrarchave.bcb.BankAccountResponse
import br.com.zup.gian.registrarchave.bcb.OwnerResponse
import com.google.protobuf.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId

class PixKeyDetailsResponse(
    val keyType: TipoChave,
    val key: String,
    val bankAccount: BankAccountResponse,
    val owner: OwnerResponse,
    val createdAt: LocalDateTime
) {
    fun montarResposta(request: ConsultaChavePixRequest): ConsultaChavePixResponse {
        return ConsultaChavePixResponse.newBuilder()
            .setClientId(request.pixId.clientId)
            .setChavePixId(request.pixId.chavePixId)
            .setChave(
                ConsultaChavePixResponse.ChavePix.newBuilder()
                    .setTipoChave(keyType)
                    .setValorChave(key)
                    .setCriadoEm(createdAt.atZone(ZoneId.of("UTC")).toInstant().let {
                        Timestamp.newBuilder()
                            .setSeconds(it.epochSecond)
                            .setNanos(it.nano)
                            .build()
                    })
                    .setConta(
                        ConsultaChavePixResponse.ChavePix.DadosDaConta.newBuilder()
                            .setNomeTitular(owner.name)
                            .setCpfTitular(owner.taxIdNumber)
                            .setInstituicaoFinanceira(
                                when (bankAccount.participant) {
                                    "60701190" -> "ITAÚ UNIBANCO"
                                    else -> "OUTRO BANCO"
                                }
                            )
                            .setAgencia(bankAccount.branch)
                            .setNumeroConta(bankAccount.accountNumber)
                            .setTipoConta(
                                when (bankAccount.accountType) {
                                    AccountType.CACC -> TipoConta.CONTA_CORRENTE
                                    else -> TipoConta.CONTA_POUPANCA
                                }
                            ).build()
                    )
            )
            .build()
    }
}
