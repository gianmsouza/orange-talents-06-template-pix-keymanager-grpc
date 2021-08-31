package br.com.zup.gian.registrarchave

import br.com.zup.gian.TipoChave
import br.com.zup.gian.TipoConta
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
data class ChavePix(
    @Column(nullable = false)
    val clientId: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val tipoChave: TipoChave,

    @Column(nullable = false)
    val valorChave: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val tipoConta: TipoConta,

    @Column(nullable = false)
    val criadoEm: LocalDateTime,
) {
    @Id
    @Column(nullable = false, unique = true)
    val id: String = UUID.randomUUID().toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChavePix

        if (clientId != other.clientId) return false
        if (tipoChave != other.tipoChave) return false
        if (valorChave != other.valorChave) return false
        if (tipoConta != other.tipoConta) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clientId.hashCode()
        result = 31 * result + tipoChave.hashCode()
        result = 31 * result + valorChave.hashCode()
        result = 31 * result + tipoConta.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}
