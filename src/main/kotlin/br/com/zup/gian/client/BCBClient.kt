package br.com.zup.gian.client

import br.com.zup.gian.registrarchave.bcb.CreatePixKeyRequest
import br.com.zup.gian.registrarchave.bcb.CreatePixKeyResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.url}")
interface BCBClient {

    @Post("/api/v1/pix/keys",
        produces = [MediaType.APPLICATION_XML],
        processes = [MediaType.APPLICATION_XML])
    fun registrarChavePix(@Body bcbPixRequest: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>
}
