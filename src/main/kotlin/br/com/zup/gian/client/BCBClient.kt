package br.com.zup.gian.client

import br.com.zup.gian.registrarchave.bcb.CreatePixKeyRequest
import br.com.zup.gian.registrarchave.bcb.CreatePixKeyResponse
import br.com.zup.gian.removerchave.bcb.DeletePixKeyRequest
import br.com.zup.gian.removerchave.bcb.DeletePixKeyResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.url}")
interface BCBClient {

    @Post(
        "/api/v1/pix/keys",
        produces = [MediaType.APPLICATION_XML],
        processes = [MediaType.APPLICATION_XML]
    )
    fun registrarChavePix(@Body bcbPixRequest: CreatePixKeyRequest): HttpResponse<CreatePixKeyResponse>

    @Delete(
        "/api/v1/pix/keys/{key}",
        produces = [MediaType.APPLICATION_XML],
        processes = [MediaType.APPLICATION_XML]
    )
    fun removerChavePix(@PathVariable key: String, @Body request: DeletePixKeyRequest) : HttpResponse<DeletePixKeyResponse>
}
