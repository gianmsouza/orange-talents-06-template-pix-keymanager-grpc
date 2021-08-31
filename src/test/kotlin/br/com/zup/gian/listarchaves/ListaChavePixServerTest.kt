package br.com.zup.gian.listarchaves

import br.com.zup.gian.*
import br.com.zup.gian.registrarchave.ChavePix
import br.com.zup.gian.registrarchave.ChavePixRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import net.bytebuddy.asm.Advice
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ListaChavePixServerTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerListaChaveServiceGrpc.KeyManagerListaChaveServiceBlockingStub
) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve retornar os dados da chave pix do cliente`() {
        val clientId = UUID.randomUUID().toString()

        val chavePix1 = ChavePix(
            clientId,
            TipoChave.EMAIL,
            "gian@teste.com",
            TipoConta.CONTA_CORRENTE,
            LocalDateTime.now()
        )

        val chavePix2 = ChavePix(
            clientId,
            TipoChave.CPF,
            "08843488856",
            TipoConta.CONTA_POUPANCA,
            LocalDateTime.now()
        )

        repository.save(chavePix1)
        repository.save(chavePix2)

        val response = grpcClient.listar(
            ListaChavePixRequest.newBuilder()
                .setClientId(clientId)
                .build()
        )

        assertTrue(response.chavePixList.size == 2)
        assertTrue(repository.findAll()[0] == chavePix1)
    }

    @Test
    fun `deve retornar erro ao nao informar o clientId para consultar a chave`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.listar(
                ListaChavePixRequest.newBuilder()
                    .setClientId(" ")
                    .build()
            )
        }

        assertEquals(Status.INVALID_ARGUMENT.code, excecao.status.code)
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerListaChaveServiceGrpc.KeyManagerListaChaveServiceBlockingStub? {
            return KeyManagerListaChaveServiceGrpc.newBlockingStub(channel)
        }
    }
}