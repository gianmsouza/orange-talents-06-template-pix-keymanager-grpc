package br.com.zup.gian.listarchaves

import br.com.zup.gian.ListaChavePixRequest
import br.com.zup.gian.ListaChavePixResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver

fun ListaChavePixRequest.validarCampos(responseObserver: StreamObserver<ListaChavePixResponse>): Boolean {
    if (clientId.isNullOrBlank()) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("clientId precisa ser informado")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }
    return true
}