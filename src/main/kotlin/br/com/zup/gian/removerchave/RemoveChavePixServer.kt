package br.com.zup.gian.removerchave

import br.com.zup.gian.KeyManagerRemoveChaveServiceGrpc
import br.com.zup.gian.RemoveChavePixRequest
import br.com.zup.gian.RemoveChavePixResponse
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class RemoveChavePixServer(
    val chavePixRepository: ChavePixRepository,
    val bcbClient: BCBClient,
    val itauClient: ItauClient
) : KeyManagerRemoveChaveServiceGrpc.KeyManagerRemoveChaveServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun remover(request: RemoveChavePixRequest, responseObserver: StreamObserver<RemoveChavePixResponse>) {

        if (!request.validarCampos(responseObserver)) return
        val possivelChave = request.validarSeChavePertenceAoCliente(responseObserver, chavePixRepository)

        if (possivelChave.isEmpty) return

        try {
            val chavePix = possivelChave.get()

            val response = bcbClient.removerChavePix(
                chavePix.valorChave,
                request.toDeletePixKeyRequestBCB(itauClient, chavePix)
            )

            if (response.status == HttpStatus.OK) {
                chavePixRepository.delete(chavePix)

                logger.info("Chave PIX {} excluída com sucesso", chavePix.id)

                responseObserver!!.onNext(RemoveChavePixResponse.newBuilder().setChavePixId(chavePix.id).build())
                responseObserver.onCompleted()
            } else {
                logger.info("Erro ao excluir chave PIX")
                responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("Chave não encontrada com os filtros informados")
                        .asRuntimeException()
                )
                return
            }
        } catch (e: Exception) {
            logger.info("Erro ao excluir chave PIX")

            if (e.message.toString().contains("FORBIDDEN")) {
                responseObserver.onError(
                    Status.PERMISSION_DENIED
                        .withDescription("Operação proibida")
                        .asRuntimeException()
                )
                return
            }

            val error = Status.INTERNAL
                .withDescription(
                    "Não foi possível a comunicação com os serviços externos. Tente novamente mais tarde"
                )
                .asRuntimeException()

            responseObserver?.onError(error)
        }
    }
}