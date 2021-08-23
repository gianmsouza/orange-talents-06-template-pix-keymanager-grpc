package br.com.zup.gian.removerchave

import br.com.zup.gian.RemoveChavePixRequest
import br.com.zup.gian.RemoveChavePixResponse
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.ChavePix
import br.com.zup.gian.registrarchave.ChavePixRepository
import br.com.zup.gian.removerchave.bcb.DeletePixKeyRequest
import br.com.zup.gian.removerchave.bcb.DeletePixKeyResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.*

fun RemoveChavePixRequest.validarCampos(responseObserver: StreamObserver<RemoveChavePixResponse>): Boolean {
    if (this.clientId.trim().isBlank()) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("ClientId não pode estar em branco")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (this.chavePixId.trim().isBlank()) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("ChavePixId não pode estar em branco")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }
    return true
}

fun RemoveChavePixRequest.validarSeChavePertenceAoCliente(
    responseObserver: StreamObserver<RemoveChavePixResponse>,
    chavePixRepository: ChavePixRepository
): Optional<ChavePix> {
    var possivelChave = chavePixRepository.findById(this.chavePixId)

    if (possivelChave.isEmpty) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("Chave pix não encontrada")
            .asRuntimeException()

        responseObserver?.onError(e)
    } else {
        if (possivelChave.get().clientId != this.clientId) {
            val e = Status.PERMISSION_DENIED
                .withDescription("Chave pix não pertence a este cliente")
                .asRuntimeException()

            responseObserver?.onError(e)
            possivelChave = Optional.empty()
        }
    }
    return possivelChave
}

fun RemoveChavePixRequest.toDeletePixKeyRequestBCB(
    itauClient: ItauClient,
    chavePix: ChavePix
): DeletePixKeyRequest {
    val dadosDaContaResponse = itauClient.buscarDadosConta(this.clientId, chavePix.tipoConta.toString())
    return DeletePixKeyRequest(chavePix.valorChave, dadosDaContaResponse.body().instituicao.ispb)
}