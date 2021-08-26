package br.com.zup.gian.consultarchave

import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.registrarchave.ChavePixRepository
import io.micronaut.http.HttpResponse

fun porPixId(
    clientId: String,
    chavePixId: String,
    chavePixRepository: ChavePixRepository,
    bcbClient: BCBClient
): HttpResponse<PixKeyDetailsResponse> {
    val chavePixOptional = chavePixRepository.findByIdAndClientId(chavePixId, clientId)

    if (chavePixOptional.isEmpty) {
        return HttpResponse.notFound()
    }

    return bcbClient.consultarChavePix(chavePixOptional.get().valorChave)
}

fun porChave(
    chave: String,
    bcbClient: BCBClient
): HttpResponse<PixKeyDetailsResponse> {
    return bcbClient.consultarChavePix(chave)
}

