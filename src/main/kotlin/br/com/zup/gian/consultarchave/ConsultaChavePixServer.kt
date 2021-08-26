package br.com.zup.gian.consultarchave

import br.com.zup.gian.ConsultaChavePixRequest
import br.com.zup.gian.ConsultaChavePixResponse
import br.com.zup.gian.KeyManagerConsultaChaveServiceGrpc
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.registrarchave.ChavePixRepository
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class ConsultaChavePixServer(
    val chavePixRepository: ChavePixRepository,
    val bcbClient: BCBClient
) :
    KeyManagerConsultaChaveServiceGrpc.KeyManagerConsultaChaveServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun consultar(
        request: ConsultaChavePixRequest,
        responseObserver: StreamObserver<ConsultaChavePixResponse>
    ) {
        if (!request.validarCampos(responseObserver, chavePixRepository)) return

        try {
            val pixKeyDetailsResponse = request.toPixKeyDetails(chavePixRepository, bcbClient)

            if (pixKeyDetailsResponse?.status == HttpStatus.NOT_FOUND) {
                throw IllegalArgumentException("Chave informada não localizada")
            }

            val response = pixKeyDetailsResponse?.body()?.montarResposta(request)

            logger.info("Chave consultada com sucesso")

            responseObserver!!.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.info("Erro na consulta da chave: " + e.message)
            when (e) {
                is IllegalArgumentException -> responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.message)
                        .asRuntimeException()
                )
                else -> responseObserver.onError(
                    Status.UNAVAILABLE.withDescription("Erro de comunicação com os serviços externos")
                        .asRuntimeException()
                )
            }
        }
    }
}