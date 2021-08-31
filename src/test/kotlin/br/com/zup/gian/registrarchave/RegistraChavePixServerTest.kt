package br.com.zup.gian.registrarchave

import br.com.zup.gian.KeyManagerRegistraChaveServiceGrpc
import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.bcb.*
import br.com.zup.gian.registrarchave.itau.DadosDaContaResponse
import br.com.zup.gian.registrarchave.itau.InstituicaoResponse
import br.com.zup.gian.registrarchave.itau.TitularResponse
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals as assertEquals

@MicronautTest(transactional = false)
internal class RegistraChavePixServerTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerRegistraChaveServiceGrpc.KeyManagerRegistraChaveServiceBlockingStub
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
    fun `deve adicionar uma nova chave pix`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        val createPixKeyRequest = createPixKeyRequest()
        val createPixKeyResponse = createPixKeyResponse()

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest))
            .thenReturn(HttpResponse.ok(createPixKeyResponse))

        val response = grpcClient.registrar(
            RegistraChavePixRequest.newBuilder()
                .setId(clientId)
                .setTipoChave(TipoChave.EMAIL)
                .setValorChave("gian.souza@teste.com.br")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        )

        assertNotNull(response.id)
        assertTrue(repository.existsById(response.id))
        assertEquals(createPixKeyRequest.key, createPixKeyResponse.key)
        assertEquals(createPixKeyRequest.bankAccount.accountNumber, createPixKeyResponse.bankAccount.accountNumber)
        assertEquals(createPixKeyRequest.bankAccount.accountType, createPixKeyResponse.bankAccount.accountType)
        assertEquals(createPixKeyRequest.bankAccount.branch, createPixKeyResponse.bankAccount.branch)
        assertEquals(createPixKeyRequest.bankAccount.participant, createPixKeyResponse.bankAccount.participant)
        assertEquals(createPixKeyRequest.owner.name, createPixKeyResponse.owner.name)
        assertEquals(createPixKeyRequest.owner.taxIdNumber, createPixKeyResponse.owner.taxIdNumber)
        assertEquals(createPixKeyRequest.owner.type, createPixKeyResponse.owner.type)
    }

    @Test
    fun `nao deve adicionar uma nova chave pix quando ja cadastrada no banco central`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest()))
            .thenThrow(
                HttpClientResponseException(
                    "UNPROCESSABLE_ENTITY",
                    HttpResponse.notFound("UNPROCESSABLE_ENTITY")
                )
            )

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@teste.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
        }
    }

    @Test
    fun `nao deve adicionar uma nova chave pix quando houver algum erro de comunicacao`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse())
        )

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest()))
            .thenThrow(RuntimeException())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@teste.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.UNAVAILABLE.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave se cliente nao encontrado no erp itau`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(null)
        )

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@teste.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.NOT_FOUND.code, status.code)
        }

        assertTrue(repository.findAll().size == 0)
    }

    @Test
    fun `nao deve inserir chave ja existente na base de dados`() {
        val clientId: String = UUID.randomUUID().toString()
        val chavePix = ChavePix(
            clientId,
            TipoChave.EMAIL,
            "gian.souza@teste.com.br",
            TipoConta.CONTA_CORRENTE,
            LocalDateTime.now()
        )
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@teste.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com email invalido`() {
        val clientId: String = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com telefone invalido`() {
        val clientId: String = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.PHONE)
                    .setValorChave("+554699900")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com cpf invalido`() {
        val clientId: String = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.CPF)
                    .setValorChave("088438")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com mais de 77 caracteres`() {
        val clientId: String = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("teste".repeat(20) + "@com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com id em formato diferente de UUID`() {
        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId("123456")
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("teste".repeat(20) + "@com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave com valor da chave preenchido e tipo chave random`() {
        val clientId: String = UUID.randomUUID().toString()

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.RANDOM)
                    .setValorChave("teste")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
        }
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

    private fun createPixKeyRequest(): CreatePixKeyRequest {
        val bankAccount = BankAccountRequest("60701190", "0001", "291900", AccountType.CACC)
        val owner = OwnerRequest(Type.NATURAL_PERSON, "Pedro Albuquerque", "33909163009")
        return CreatePixKeyRequest(TipoChave.EMAIL, "gian.souza@teste.com.br", bankAccount, owner)
    }

    private fun createPixKeyResponse(): CreatePixKeyResponse {
        val bankAccount = BankAccountResponse("60701190", "0001", "291900", AccountType.CACC)
        val owner = OwnerResponse(Type.NATURAL_PERSON, "Pedro Albuquerque", "33909163009")
        return CreatePixKeyResponse(
            TipoChave.EMAIL,
            "gian.souza@teste.com.br",
            bankAccount,
            owner,
            "2021-08-27T16:57:14.822983"
        )
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeyManagerRegistraChaveServiceGrpc.KeyManagerRegistraChaveServiceBlockingStub? {
            return KeyManagerRegistraChaveServiceGrpc.newBlockingStub(channel)
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