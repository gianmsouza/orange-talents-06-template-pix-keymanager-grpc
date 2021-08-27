package br.com.zup.gian.registrarchave

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository <ChavePix, String> {

    fun findByValorChave(valorChave: String): Optional<ChavePix>
    fun findByIdAndClientId(chavePixId: String, clientId: String): Optional<ChavePix>
    fun findByClientId(clientId: String): List<ChavePix>
}
