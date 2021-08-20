package br.com.zup.gian.registrarchave

import br.com.zup.gian.KeyManagerServiceGrpc
import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.RegistraChavePixResponse
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientResponseException
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class PixKeyManagerServer(
    val chavePixRepository: ChavePixRepository,
    val itauClient: ItauClient,
    val bcbClient: BCBClient
) :
    KeyManagerServiceGrpc.KeyManagerServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun registraChavePix(
        request: RegistraChavePixRequest?,
        responseObserver: StreamObserver<RegistraChavePixResponse>?
    ) {
        logger.info("Novo registro de chave PIX em andamento")

        val validarCampos = request?.validarCampos(responseObserver, itauClient, chavePixRepository)

        if (validarCampos!!) {
            try {
                val bcbCreatePixKeyRequest = request.toBCBCreatePixkeyRequest(itauClient)
                bcbClient.registrarChavePix(bcbCreatePixKeyRequest)

                val chavePix: ChavePix = request.toModel()
                chavePixRepository.save(chavePix)

                val response = RegistraChavePixResponse.newBuilder()
                    .setId(chavePix.id)
                    .build()

                logger.info("Registro de chave PIX com sucesso")

                responseObserver!!.onNext(response)
                responseObserver.onCompleted()
            } catch (error: Exception) {
                logger.info("Registro de chave PIX com falha")

                if (error.message != null && error.message.toString().contains("UNPROCESSABLE_ENTITY")) {
                    logger.error("Chave já cadastrada no Banco Central")

                    val e = Status.ALREADY_EXISTS
                        .withDescription(
                            "Chave já cadastrada no Banco Central"
                        )
                        .asRuntimeException()

                    responseObserver?.onError(e)
                    return
                }
                val e = Status.INTERNAL
                    .withDescription(
                        "Não foi possível a comunicação com o serviço do Banco Central"
                    )
                    .asRuntimeException()

                responseObserver?.onError(e)
            }
        }
    }
}