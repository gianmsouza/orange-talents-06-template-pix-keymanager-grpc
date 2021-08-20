package br.com.zup.gian.registrarchave

import br.com.zup.gian.KeyManagerServiceGrpc
import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import br.com.zup.gian.client.BCBClient
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.bcb.*
import br.com.zup.gian.registrarchave.itau.DadosDaContaResponse
import br.com.zup.gian.registrarchave.itau.InstituicaoResponse
import br.com.zup.gian.registrarchave.itau.TitularResponse
import com.google.api.Http
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.Mockito
import java.beans.beancontext.BeanContextProxy
import java.lang.RuntimeException
import java.time.LocalDate
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class PixKeyManagerServerTest(
    val repository: ChavePixRepository,
    val grpcClient: KeyManagerServiceGrpc.KeyManagerServiceBlockingStub
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

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest()))
            .thenReturn(HttpResponse.ok(createPixKeyResponse()))

        val response = grpcClient.registraChavePix(
            RegistraChavePixRequest.newBuilder()
                .setId(clientId)
                .setTipoChave(TipoChave.EMAIL)
                .setValorChave("gian.souza@teste.com.br")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        )

        assertNotNull(response.id)
        assertTrue(repository.existsById(response.id))
    }

    @Test
    fun `nao deve adicionar uma nova chave pix quando ja cadastrada no banco central`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest()))
            .thenThrow(RuntimeException("UNPROCESSABLE_ENTITY"))

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registraChavePix(
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
            HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.registrarChavePix(createPixKeyRequest()))
            .thenThrow(RuntimeException())

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registraChavePix(
                RegistraChavePixRequest.newBuilder()
                    .setId(clientId)
                    .setTipoChave(TipoChave.EMAIL)
                    .setValorChave("gian.souza@teste.com.br")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        with(excecao) {
            assertEquals(Status.INTERNAL.code, this.status.code)
        }
    }

    @Test
    fun `nao deve inserir chave se cliente nao encontrado no erp itau`() {
        val clientId: String = UUID.randomUUID().toString()

        Mockito.`when`(itauClient.buscarDadosConta(clientId, TipoConta.CONTA_CORRENTE.toString())).thenReturn(
            HttpResponse.ok(null))

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registraChavePix(
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
            TipoConta.CONTA_CORRENTE
        )
        repository.save(chavePix)

        val excecao = assertThrows<StatusRuntimeException> {
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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
            grpcClient.registraChavePix(
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

    @Test
    fun `cria um objetos req e resp para o cadastro de uma nova chave pix`() {
        val chavePix = ChavePix(
            clientId = "123456",
            tipoChave = TipoChave.EMAIL,
            valorChave = "gian@teste.com",
            tipoConta = TipoConta.CONTA_CORRENTE
        )
        assertNotNull(chavePix)
        assertEquals("123456", chavePix.clientId)
        assertEquals(TipoChave.EMAIL, chavePix.tipoChave)
        assertEquals("gian@teste.com", chavePix.valorChave)
        assertEquals(TipoConta.CONTA_CORRENTE, chavePix.tipoConta)

        val createPixKeyRequest = createPixKeyRequest()
        assertEquals(TipoChave.EMAIL, createPixKeyRequest.keyType)
        assertEquals("gian.souza@teste.com.br", createPixKeyRequest.key)

        val ownerRequest = createPixKeyRequest.owner
        assertEquals(Type.NATURAL_PERSON, ownerRequest.type)
        assertEquals("Pedro Albuquerque", ownerRequest.name)
        assertEquals("33909163009", ownerRequest.taxIdNumber)

        val bankRequest = createPixKeyRequest.bankAccount
        assertEquals("60701190", bankRequest.participant)
        assertEquals("0001", bankRequest.branch)
        assertEquals("291900", bankRequest.accountNumber)
        assertEquals(AccountType.CACC, bankRequest.accountType)

        val createPixKeyResponse = createPixKeyResponse()
        assertEquals(TipoChave.EMAIL, createPixKeyResponse.keyType)
        assertEquals("gian.souza@teste.com.br", createPixKeyResponse.key)

        val ownerResponse = createPixKeyResponse.owner
        assertEquals(Type.NATURAL_PERSON, ownerResponse.type)
        assertEquals("Pedro Albuquerque", ownerResponse.name)
        assertEquals("33909163009", ownerResponse.taxIdNumber)

        val bankResponse = createPixKeyResponse.bankAccount
        assertEquals("60701190", bankResponse.participant)
        assertEquals("0001", bankResponse.branch)
        assertEquals("291900", bankResponse.accountNumber)
        assertEquals(AccountType.CACC, bankResponse.accountType)
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
        return CreatePixKeyResponse(TipoChave.EMAIL, "gian.souza@teste.com.br", bankAccount, owner, LocalDate.now().toString())
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): KeyManagerServiceGrpc.KeyManagerServiceBlockingStub? {
            return KeyManagerServiceGrpc.newBlockingStub(channel)
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