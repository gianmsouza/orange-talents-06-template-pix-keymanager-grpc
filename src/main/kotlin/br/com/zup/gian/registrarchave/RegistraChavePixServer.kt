package br.com.zup.gian.registrarchave


import br.com.zup.gian.KeyManagerRegistraChaveServiceGrpc
import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.RegistraChavePixResponse
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class RegistraChavePixServer(
    val chavePixRepository: ChavePixRepository,
    val itauClient: ItauClient,
    val bcbClient: BCBClient
) : KeyManagerRegistraChaveServiceGrpc.KeyManagerRegistraChaveServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun registrar(
        request: RegistraChavePixRequest,
        responseObserver: StreamObserver<RegistraChavePixResponse>
    ) {
        logger.info("Novo registro de chave PIX em andamento")

        if (!request.validarCampos(responseObserver, itauClient, chavePixRepository)) {
            logger.info("Registro de chave PIX falhou na validação de campos")
            return
        }

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