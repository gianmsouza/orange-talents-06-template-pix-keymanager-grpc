package br.com.zup.gian.registrarchave.itau

import br.com.zup.gian.TipoConta

data class DadosDaContaResponse(
    val tipo: TipoConta,
    val instituicao: InstituicaoResponse,
    val agencia: String,
    val numero: String,
    val titular: TitularResponse
)
