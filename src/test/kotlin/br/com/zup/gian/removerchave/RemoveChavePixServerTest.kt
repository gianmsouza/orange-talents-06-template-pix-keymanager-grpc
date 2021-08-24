package br.com.zup.gian.removerchave

import br.com.zup.gian.KeyManagerRemoveChaveServiceGrpc
import br.com.zup.gian.RemoveChavePixRequest
import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.ChavePix
import br.com.zup.gian.registrarchave.ChavePixRepository
import br.com.zup.gian.registrarchave.itau.DadosDaContaResponse
import br.com.zup.gian.registrarchave.itau.InstituicaoResponse
import br.com.zup.gian.registrarchave.itau.TitularResponse
import br.com.zup.gian.removerchave.bcb.DeletePixKeyRequest
import br.com.zup.gian.removerchave.bcb.DeletePixKeyResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientException
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.lang.RuntimeException
import java.net.ConnectException
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class RemoveChavePixServerTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRemoveChaveServiceGrpc.KeyManagerRemoveChaveServiceBlockingStub
) {
    @field:Inject
    lateinit var itauClient: ItauClient

    @field:Inject
    lateinit var bcbClient: BCBClient

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover uma chave pix existente`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        Mockito.`when`(itauClient.buscarDadosConta(chavePix.clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        val requestBCB = deletePixKeyRequest(chavePix)
        val responseBCB = deletePixKeyResponse(chavePix)

        Mockito.`when`(bcbClient.removerChavePix(chavePix.valorChave, requestBCB))
            .thenReturn(HttpResponse.ok(responseBCB))

        val response = grpcClient.remover(
            RemoveChavePixRequest.newBuilder()
                .setChavePixId(chavePix.id)
                .setClientId(chavePix.clientId)
                .build()
        )

        assertEquals("60701190", requestBCB.participant)
        assertEquals(chavePix.valorChave, requestBCB.key)
        assertEquals(deletePixKeyRequest(chavePix), requestBCB)
        assertEquals(deletePixKeyRequest(chavePix).hashCode(), requestBCB.hashCode())

        assertEquals(chavePix.valorChave, responseBCB.key)
        assertEquals("60701190", responseBCB.participant)
        assertEquals(deletePixKeyResponse(chavePix), responseBCB)
        assertEquals(deletePixKeyResponse(chavePix).hashCode(), responseBCB.hashCode())

        assertNotNull(response.chavePixId)
        assertEquals(chavePix.id, response.chavePixId)
        assertTrue(repository.findAll().size == 0)
    }

    @Test
    fun `deve retornar erro ao tentar excluir uma chave sem passar o valor do clientId`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(UUID.randomUUID().toString())
                    .setClientId("   ")
                    .build()
            )
        }
        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao tentar excluir uma chave sem passar seu id`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId("   ")
                    .setClientId(UUID.randomUUID().toString())
                    .build()
            )
        }
        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao informar um id de chave inexistente`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(UUID.randomUUID().toString())
                    .setClientId(chavePix.clientId)
                    .build()
            )
        }
        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao informar um id de chave que nao pertence ao cliente`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(chavePix.id)
                    .setClientId(UUID.randomUUID().toString())
                    .build()
            )
        }
        with(excecao) {
            assertEquals(Status.PERMISSION_DENIED.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao tentar excluir uma chave que nao existe no bcb`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        Mockito.`when`(itauClient.buscarDadosConta(chavePix.clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        Mockito.`when`(bcbClient.removerChavePix(chavePix.valorChave, deletePixKeyRequest(chavePix)))
            .thenReturn(HttpResponse.notFound(deletePixKeyResponse(chavePix)))

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(chavePix.id)
                    .setClientId(chavePix.clientId)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao excluir uma chave onde o codigo do participante enviado e diferente do bcb`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        Mockito.`when`(itauClient.buscarDadosConta(chavePix.clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        Mockito.`when`(bcbClient.removerChavePix(chavePix.valorChave, deletePixKeyRequest(chavePix)))
            .thenThrow(RuntimeException("FORBIDDEN"))

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(chavePix.id)
                    .setClientId(chavePix.clientId)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.PERMISSION_DENIED.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao nao conseguir se comunicar com as APIS externas`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        Mockito.`when`(itauClient.buscarDadosConta(chavePix.clientId, TipoConta.CONTA_CORRENTE.toString()))
            .thenThrow(RuntimeException())

        Mockito.`when`(bcbClient.removerChavePix(chavePix.valorChave, deletePixKeyRequest(chavePix)))
            .thenThrow(RuntimeException())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemoveChavePixRequest.newBuilder()
                    .setChavePixId(chavePix.id)
                    .setClientId(chavePix.clientId)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INTERNAL.code, this.status.code)
        }
    }

    private fun chavePix(): ChavePix {
        val clientId: String = UUID.randomUUID().toString()
        return ChavePix(clientId, TipoChave.EMAIL, "gian.souza@teste.com.br", TipoConta.CONTA_CORRENTE)
    }

    private fun deletePixKeyRequest(chavePix: ChavePix): DeletePixKeyRequest {
        return DeletePixKeyRequest(chavePix.valorChave, "60701190")
    }

    private fun deletePixKeyResponse(chavePix: ChavePix): DeletePixKeyResponse {
        return DeletePixKeyResponse(chavePix.valorChave, "60701190", LocalDate.now().toString())
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        val instituicao = InstituicaoResponse("ITAÚ UNIBANCO S.A.", "60701190")
        val titular = TitularResponse("5260263c-a3c1-4727-ae32-3bdb2538841b", "Pedro Albuquerque", "33909163009")

        return DadosDaContaResponse(
            TipoConta.CONTA_CORRENTE,
            instituicao,
            "0001",
            "291900",
            titular
        )
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerRemoveChaveServiceGrpc.KeyManagerRemoveChaveServiceBlockingStub? {
            return KeyManagerRemoveChaveServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(ItauClient::class)
    fun itauERPMock(): ItauClient {
        return Mockito.mock(ItauClient::class.java)
    }

    @MockBean(BCBClient::class)
    fun bcpClientMock(): BCBClient {
        return Mockito.mock(BCBClient::class.java)
    }
}
