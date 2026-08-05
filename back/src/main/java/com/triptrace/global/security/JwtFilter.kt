package com.triptrace.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

/**
 * 인증의 "관문". 모든 요청마다 토큰을 확인해 로그인 상태를 세운다.
 * 토큰이 유효하면 이 요청을 인증된 사용자로 표시하고, 없거나 무효면 그냥 통과시킨다.
 * (통과 후 접근 허용 여부는 SecurityConfig의 규칙이 판단)
 */
class JwtFilter(private val jwtProvider: JwtProvider) : OncePerRequestFilter() {

    /**
     * 요청마다 실행: 헤더의 토큰 → 검증 → 통과 시 인증 정보 등록.
     *
     * 토큰이 무효여도 여기서 요청을 끊지 않는다. 체인을 계속 태워야 permitAll 경로가 정상 동작한다.
     *
     * @throws ServletException 이후 필터·서블릿 처리 중 오류가 날 시
     * @throws IOException 이후 필터·서블릿 처리 중 입출력 오류가 날 시
     */
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        // validateToken이 만료·위변조 예외를 모두 false로 바꿔주므로 여기서 따로 잡지 않는다.
        if (token != null && jwtProvider.validateToken(token)) {
            val memberId = jwtProvider.getMemberId(token)

            val authentication =
                UsernamePasswordAuthenticationToken(memberId, null, emptyList<GrantedAuthority>())
            authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    // "Authorization: Bearer {token}" 에서 토큰 문자열만 꺼낸다
    private fun resolveToken(request: HttpServletRequest): String? {
        val authorization = request.getHeader("Authorization")

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7)
        }

        return null
    }
}
