package br.com.zup.gian.listarchaves

import br.com.zup.gian.KeyManagerListaChaveServiceGrpc
import br.com.zup.gian.ListaChavePixRequest
import br.com.zup.gian.ListaChavePixResponse
import br.com.zup.gian.registrarchave.ChavePixRepository
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.time.ZoneId
import javax.inject.Singleton

@Singleton
class ListaChavePixServer(
    val chavePixRepository: ChavePixRepository
) : KeyManagerListaChaveServiceGrpc.KeyManagerListaChaveServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun listar(request: ListaChavePixRequest, responseObserver: StreamObserver<ListaChavePixResponse>) {

        logger.info("Consulta de chaves do cliente {} em andamento", request.clientId)

        if (!request.validarCampos(responseObserver)) return

        val listaChaves = chavePixRepository.findByClientId(request.clientId).map {
            ListaChavePixResponse.ChavePix.newBuilder()
                .setChavePixId(it.id)
                .setClientId(it.clientId)
                .setTipoChave(it.tipoChave)
                .setValorChave(it.valorChave)
                .setTipoConta(it.tipoConta)
                .setCriadoEm(it.criadoEm.atZone(ZoneId.of("UTC")).toInstant().let { dataCriacao ->
                    Timestamp.newBuilder()
                        .setSeconds(dataCriacao.epochSecond)
                        .setNanos(dataCriacao.nano)
                        .build()
                })
                .build()
        }

        responseObserver.onNext(
            ListaChavePixResponse.newBuilder()
                .addAllChavePix(listaChaves)
                .build()
        )

        logger.info("Consulta de chaves do cliente {} finalizada, com {} resultados", request.clientId, listaChaves.size)

        responseObserver.onCompleted()
    }
}