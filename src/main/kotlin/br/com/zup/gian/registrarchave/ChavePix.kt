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
}
