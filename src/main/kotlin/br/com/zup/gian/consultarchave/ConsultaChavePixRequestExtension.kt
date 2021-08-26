package br.com.zup.gian.consultarchave

import br.com.zup.gian.ConsultaChavePixRequest
import br.com.zup.gian.ConsultaChavePixRequest.FiltroCase.CHAVE
import br.com.zup.gian.ConsultaChavePixRequest.FiltroCase.PIXID
import br.com.zup.gian.ConsultaChavePixResponse
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.registrarchave.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpResponse

fun ConsultaChavePixRequest.validarCampos(
    responseObserver: StreamObserver<ConsultaChavePixResponse>,
    chavePixRepository: ChavePixRepository
): Boolean {
    if (filtroCase == PIXID) {
        if (pixId.chavePixId.isNullOrBlank() || pixId.clientId.isNullOrBlank()) {
            val e = Status.INVALID_ARGUMENT
                .withDescription("chavePixId e clientId precisam ser informados")
                .asRuntimeException()

            responseObserver?.onError(e)
            return false
        }

        var possivelChave = chavePixRepository.findById(this.pixId.chavePixId)

        if (possivelChave.isEmpty) {
            val e = Status.INVALID_ARGUMENT
                .withDescription("Chave pix não encontrada")
                .asRuntimeException()

            responseObserver?.onError(e)
            return false
        }

        if (possivelChave.get().clientId != this.pixId.clientId) {
            val e = Status.PERMISSION_DENIED
                .withDescription("Chave pix não pertence a este cliente")
                .asRuntimeException()

            responseObserver?.onError(e)
            return false
        }
    } else if (filtroCase == CHAVE) {
        if (this.chave.length > 77) {
            val e = Status.INVALID_ARGUMENT
                .withDescription("Chave pix não deve ter mais de 77 caracteres")
                .asRuntimeException()

            responseObserver?.onError(e)
            return false
        }
    }

    return true
}

fun ConsultaChavePixRequest.toPixKeyDetails(
    chavePixRepository: ChavePixRepository,
    bcbClient: BCBClient
): HttpResponse<PixKeyDetailsResponse>? {
    val filtro = when (filtroCase) {
        PIXID -> pixId.let { porPixId(it.clientId, it.chavePixId, chavePixRepository, bcbClient) }
        CHAVE -> porChave(chave, bcbClient)

        else -> HttpResponse.notFound()
    }

    return filtro
}


