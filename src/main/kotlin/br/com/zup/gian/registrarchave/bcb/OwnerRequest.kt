package br.com.zup.gian.registrarchave.bcb

data class OwnerRequest(
    val type: Type,
    val name: String,
    val taxIdNumber: String
)
