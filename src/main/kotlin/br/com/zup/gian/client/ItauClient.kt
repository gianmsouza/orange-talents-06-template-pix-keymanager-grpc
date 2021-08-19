package br.com.zup.gian.client

import br.com.zup.gian.registrarchave.itau.DadosDaContaResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("\${itau.erp}")
interface ItauClient {

    @Get("/api/v1/clientes/{clienteId}/contas{?tipo}")
    fun buscarDadosConta(
        @PathVariable clienteId: String,
        @QueryValue tipo: String
    ): HttpResponse<DadosDaContaResponse>
}