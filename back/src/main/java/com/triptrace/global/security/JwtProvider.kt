package com.triptrace.global.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * 토큰 자체를 다루는 클래스. (발급 + 검증)
 * 발급은 로그인/재발급 시 AuthService가, 검증은 요청마다 JwtFilter가 사용한다.
 */
@Component
class JwtProvider(
    @Value("\${custom.jwt.secretKey}") secret: String,
    // val 프로퍼티에 붙는 애노테이션은 타겟이 모호해지므로 생성자 파라미터임을 명시한다.
    @param:Value("\${custom.accessToken.expirationSeconds}") private val accessTokenExpirationSeconds: Long
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    // [발급] 로그인 성공 시 내려줄 액세스 토큰 생성
    fun generateAccessToken(memberId: Long, email: String?): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirationSeconds * 1000)

        return Jwts.builder()
            .claim("memberId", memberId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    }

    // [발급] DB에 저장할 리프레시 토큰 생성 (JWT 아닌 UUID)
    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    /**
     * [검증] 액세스 토큰이 위변조/만료되지 않았는지 확인한다.
     *
     * 아래 예외를 모두 잡아 false로 바꾸므로 이 함수 자체는 예외를 던지지 않는다.
     * 헤더가 비어 있는 요청에서도 필터가 터지지 않아야 하므로 token은 null을 허용한다.
     */
    fun validateToken(token: String?): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * [해석] 토큰에서 로그인 사용자 식별자를 꺼낸다.
     *
     * @throws ExpiredJwtException 토큰 만료 시
     * @throws MalformedJwtException 토큰 형식이 올바르지 않을 시
     * @throws SignatureException 서명이 일치하지 않을 시
     * @throws IllegalArgumentException token이 null이거나 비어 있을 시
     */
    fun getMemberId(token: String?): Long = (parseClaims(token)["memberId"] as Number).toLong()

    /**
     * [해석] 토큰에서 로그인 사용자 이메일을 꺼낸다.
     *
     * @throws ExpiredJwtException 토큰 만료 시
     * @throws MalformedJwtException 토큰 형식이 올바르지 않을 시
     * @throws SignatureException 서명이 일치하지 않을 시
     * @throws IllegalArgumentException token이 null이거나 비어 있을 시
     */
    fun getEmail(token: String?): String? = parseClaims(token).get("email", String::class.java)

    /**
     * @throws ExpiredJwtException 토큰 만료 시
     * @throws MalformedJwtException 토큰 형식이 올바르지 않을 시
     * @throws SignatureException 서명이 일치하지 않을 시
     * @throws IllegalArgumentException token이 null이거나 비어 있을 시
     */
    private fun parseClaims(token: String?): Claims =
        Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
}
