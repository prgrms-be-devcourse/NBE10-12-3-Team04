package com.triptrace.domain.auth.auth.oauth;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UsernameGeneratorTest {

    @Autowired
    private UsernameGenerator usernameGenerator;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일 앞부분 + 랜덤 4자리로 닉네임을 만든다.")
    void generate() {
        String username = usernameGenerator.generate("traveler@gmail.com");

        assertThat(username).matches("traveler\\d{4}");
    }

    @Test
    @DisplayName("이미 존재하는 닉네임과 겹치지 않는 값을 만든다.")
    void generateAvoidsExisting() {
        // 같은 접두사를 쓰는 회원이 있어도 랜덤 숫자가 달라 충돌하지 않는다.
        memberRepository.save(new Member("taken@test.com", "traveler0001", "hashed", null, MemberStatus.ACTIVE));

        String username = usernameGenerator.generate("traveler@gmail.com");

        assertThat(memberRepository.existsByUsername(username)).isFalse();
    }

    @Test
    @DisplayName("이메일 로컬파트에서 영숫자가 아닌 문자는 제거한다.")
    void generateSanitizesLocalPart() {
        String username = usernameGenerator.generate("my.travel+tag@gmail.com");

        assertThat(username).matches("mytraveltag\\d{4}");
    }

    @Test
    @DisplayName("쓸 수 있는 문자가 없으면 기본 접두사를 쓴다.")
    void generateFallsBackToDefaultPrefix() {
        assertThat(usernameGenerator.generate("한글@gmail.com")).matches("user\\d{4}");
        assertThat(usernameGenerator.generate(null)).matches("user\\d{4}");
    }

    @Test
    @DisplayName("생성한 닉네임은 username 컬럼 길이(50자)를 넘지 않는다.")
    void generateRespectsColumnLength() {
        String longLocalPart = "a".repeat(80);

        assertThat(usernameGenerator.generate(longLocalPart + "@gmail.com")).hasSizeLessThanOrEqualTo(50);
    }
}
