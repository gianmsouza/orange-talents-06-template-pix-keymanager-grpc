package br.com.zup.gian.removerchave.bcb

data class DeletePixKeyResponse(
    val key: String,
    val participant: String,
    val deletedAt: String
)
