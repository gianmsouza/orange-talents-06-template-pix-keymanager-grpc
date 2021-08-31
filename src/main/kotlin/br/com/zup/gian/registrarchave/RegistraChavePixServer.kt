package br.com.zup.gian.registrarchave


import br.com.zup.gian.KeyManagerRegistraChaveServiceGrpc
import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.RegistraChavePixResponse
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.client.exceptions.HttpClientResponseException
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

        try {
            if (!request.validarCampos(responseObserver, itauClient, chavePixRepository)) {
                logger.info("Registro de chave PIX falhou na validação de campos")
                return
            }

            val bcbCreatePixKeyRequest = request.toBCBCreatePixkeyRequest(itauClient)
            val bcbCreatePixKeyResponse = bcbClient.registrarChavePix(bcbCreatePixKeyRequest)

            val chavePix = request.toModel(bcbCreatePixKeyResponse.body())
            chavePixRepository.save(chavePix)

            val response = RegistraChavePixResponse.newBuilder()
                .setId(chavePix.id)
                .build()

            logger.info("Registro de chave PIX com sucesso")

            responseObserver!!.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.info("Registro de chave PIX com falha: ${e.message}")
            when (e) {
                is HttpClientResponseException -> responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription("Chave já cadastrada no BCB").asRuntimeException()
                )
                else -> responseObserver.onError(
                    Status.UNAVAILABLE.withDescription("Erro na comunicação com os serviços externos")
                        .asRuntimeException()
                )
            }
        }
    }
}