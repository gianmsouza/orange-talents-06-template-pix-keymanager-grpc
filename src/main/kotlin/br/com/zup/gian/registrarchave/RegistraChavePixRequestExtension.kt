package br.com.zup.gian.registrarchave

import br.com.zup.gian.RegistraChavePixRequest
import br.com.zup.gian.RegistraChavePixResponse
import br.com.zup.gian.TipoConta
import br.com.zup.gian.client.ItauClient
import br.com.zup.gian.registrarchave.bcb.*
import io.grpc.Status
import io.grpc.stub.StreamObserver
import java.util.*

fun RegistraChavePixRequest.validarCampos(
    responseObserver: StreamObserver<RegistraChavePixResponse>?,
    itauClient: ItauClient,
    chavePixRepository: ChavePixRepository
): Boolean {

    if (!id.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$".toRegex())) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("ID do cliente deve estar no formato UUID")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (tipoChave.toString() == "CPF" && !valorChave.matches("^[0-9]{11}$".toRegex())) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("CPF precisa estar no formato 00000000000")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (tipoChave.toString() == "PHONE" && !valorChave.matches("^\\+[1-9][0-9]\\d{11}\$".toRegex())) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("Telefone precisa estar no formato +5585988714077")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (tipoChave.toString() == "EMAIL" && !valorChave.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$".toRegex())) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("Email precisa estar em um formato válido")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (tipoChave.toString() == "RANDOM" && !valorChave.trim().isBlank()) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("Valor da chave não deve ser informado na opção de Chave Aleatória")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    if (valorChave.length > 77) {
        val e = Status.INVALID_ARGUMENT
            .withDescription("Valor da chave não deve conter mais de 77 caracteres")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    val dadosDaContaResponse = itauClient.buscarDadosConta(this.id, this.tipoConta.toString())

    if (dadosDaContaResponse.body() == null) {
        val e = Status.NOT_FOUND
            .withDescription(
                "Cliente não encontrado no ERP Itaú. " +
                        "Verifique também se o tipo de conta informado está correto"
            )
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    val possivelChave = chavePixRepository.findByValorChave(valorChave)

    if (possivelChave.isPresent) {
        val e = Status.ALREADY_EXISTS
            .withDescription("Chave já cadastrada no sistema")
            .asRuntimeException()

        responseObserver?.onError(e)
        return false
    }

    return true
}

fun RegistraChavePixRequest.toModel(): ChavePix {
    return ChavePix(
        clientId = id,
        tipoChave = tipoChave,
        valorChave = when {
            this.valorChave.trim().isBlank() -> UUID.randomUUID().toString()
            else -> this.valorChave
        },
        tipoConta = tipoConta
    )
}

fun RegistraChavePixRequest.toBCBCreatePixkeyRequest(itauClient: ItauClient): CreatePixKeyRequest {
    val dadosDaContaResponse = itauClient.buscarDadosConta(this.id, this.tipoConta.toString())

    val ispb = dadosDaContaResponse.body().instituicao.ispb
    val branch = dadosDaContaResponse.body().agencia
    val accountNumber = dadosDaContaResponse.body().numero
    val accountType = when (dadosDaContaResponse.body().tipo) {
        TipoConta.CONTA_CORRENTE -> AccountType.CACC
        else -> AccountType.SVGS
    }
    val bankAccount: BankAccountRequest = BankAccountRequest(ispb, branch, accountNumber, accountType)

    val name = dadosDaContaResponse.body().titular.nome
    val taxIdNumber = dadosDaContaResponse.body().titular.cpf
    val owner: OwnerRequest = OwnerRequest(Type.NATURAL_PERSON, name, taxIdNumber)

    return CreatePixKeyRequest(tipoChave, valorChave, bankAccount, owner)
}