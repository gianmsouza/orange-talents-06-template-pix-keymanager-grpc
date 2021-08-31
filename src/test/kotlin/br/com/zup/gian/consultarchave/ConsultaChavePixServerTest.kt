package br.com.zup.gian.consultarchave

import br.com.zup.gian.ConsultaChavePixRequest.FiltroPorPixId
import br.com.zup.gian.ConsultaChavePixRequest.newBuilder
import br.com.zup.gian.KeyManagerConsultaChaveServiceGrpc
import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.registrarchave.ChavePix
import br.com.zup.gian.registrarchave.ChavePixRepository
import br.com.zup.gian.registrarchave.bcb.AccountType
import br.com.zup.gian.registrarchave.bcb.BankAccountResponse
import br.com.zup.gian.registrarchave.bcb.OwnerResponse
import br.com.zup.gian.registrarchave.bcb.Type
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class ConsultaChavePixServerTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerConsultaChaveServiceGrpc.KeyManagerConsultaChaveServiceBlockingStub
) {
    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @field:Inject
    lateinit var bcbClient: BCBClient

    @Test
    fun `deve retornar os dados de uma chave atraves da consulta por pixId e clientId`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        val pixKeyDetailsResponse = pixKeyDetailsResponse()

        Mockito.`when`(bcbClient.consultarChavePix(chavePix.valorChave))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse))

        val response = grpcClient.consultar(
            newBuilder()
                .setPixId(
                    FiltroPorPixId.newBuilder()
                        .setChavePixId(chavePix.id)
                        .setClientId(chavePix.clientId).build()
                )
                .build()
        )

        with(response) {
            assertEquals(chavePix.clientId, clientId)
            assertEquals(chavePix.id, chavePixId)
            assertEquals(TipoChave.valueOf(chavePix.tipoChave.name), chave.tipoChave)
            assertEquals(chavePix.valorChave, chave.valorChave)
            assertEquals(TipoChave.EMAIL, pixKeyDetailsResponse.keyType)
            assertEquals(chavePix.valorChave, pixKeyDetailsResponse.key)
            assertEquals("60701190", pixKeyDetailsResponse.bankAccount.participant)
            assertEquals("33909163009", pixKeyDetailsResponse.owner.taxIdNumber)
        }
    }

    @Test
    fun `deve retornar os dados de uma chave atraves da consulta por chave`() {
        Mockito.`when`(bcbClient.consultarChavePix("gian@teste.com"))
            .thenReturn(HttpResponse.ok(pixKeyDetailsResponse()))

        val response = grpcClient.consultar(
            newBuilder()
                .setChave("gian@teste.com")
                .build()
        )

        with(response) {
            assertEquals("gian@teste.com", chave.valorChave)
        }
    }

    @Test
    fun `deve retornar erro ao tentar consultar chave sem passar o pixId junto ao clientId`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setPixId(
                        FiltroPorPixId.newBuilder()
                            .setChavePixId("")
                            .setClientId("").build()
                    )
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao tentar consultar chave com pixId invalido`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setPixId(
                        FiltroPorPixId.newBuilder()
                            .setChavePixId(UUID.randomUUID().toString())
                            .setClientId(chavePix.clientId).build()
                    )
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao tentar consultar chave onde o cliente nao e dono da chave`() {
        val chavePix = chavePix()
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setPixId(
                        FiltroPorPixId.newBuilder()
                            .setChavePixId(chavePix.id)
                            .setClientId(UUID.randomUUID().toString()).build()
                    )
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.PERMISSION_DENIED.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro quando valor da chave informada nao existir`() {
        Mockito.`when`(bcbClient.consultarChavePix("gian@teste.com"))
            .thenReturn(HttpResponse.notFound())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setChave("gian@teste.com")
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao nao conseguir se comunicar com os servicos externos`() {
        Mockito.`when`(bcbClient.consultarChavePix("gian@teste.com"))
            .thenThrow(RuntimeException())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setChave("gian@teste.com")
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.UNAVAILABLE.code, this.status.code)
        }
    }

    @Test
    fun `deve retornar erro ao informar o valor da chave com mais de 77 caracteres`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.consultar(
                newBuilder()
                    .setChave("gian@teste.com".repeat(40))
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        val bankAccount = BankAccountResponse("60701190", "0001", "291900", AccountType.CACC)
        val owner = OwnerResponse(Type.NATURAL_PERSON, "Pedro Albuquerque", "33909163009")
        return PixKeyDetailsResponse(TipoChave.EMAIL, "gian@teste.com", bankAccount, owner, LocalDateTime.now())
    }

    private fun chavePix(): ChavePix {
        val clientId = UUID.randomUUID().toString()
        return ChavePix(
            clientId = clientId,
            tipoChave = TipoChave.EMAIL,
            valorChave = "gian@teste.com",
            tipoConta = TipoConta.CONTA_CORRENTE,
            criadoEm = LocalDateTime.now()
        )
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerConsultaChaveServiceGrpc.KeyManagerConsultaChaveServiceBlockingStub? {
            return KeyManagerConsultaChaveServiceGrpc.newBlockingStub(channel)
        }
    }

    @MockBean(BCBClient::class)
    fun bcpClientMock(): BCBClient {
        return Mockito.mock(BCBClient::class.java)
    }
}