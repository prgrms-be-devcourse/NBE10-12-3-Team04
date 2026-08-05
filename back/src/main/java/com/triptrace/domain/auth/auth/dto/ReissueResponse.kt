package com.triptrace.domain.auth.auth.dto

@JvmRecord
data class ReissueResponse(
    val accessToken: String?,
    val tokenType: String?
) {
    constructor(accessToken: String?) : this(accessToken, "Bearer")
}
