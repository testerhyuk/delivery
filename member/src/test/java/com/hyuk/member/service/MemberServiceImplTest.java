package com.hyuk.member.service;

import com.hyuk.common.Snowflake;
import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.entity.MemberEntity;
import com.hyuk.member.entity.enums.Role;
import com.hyuk.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {
    @Mock
    private MemberRepository memberRepository;

    @Mock
    private Snowflake snowflake;

    @Spy
    private ModelMapper modelMapper;

    @InjectMocks
    private MemberServiceImpl memberService;

    private final String email = "test@gmail.com";
    private final String name = "혁";
    private final String provider = "google";
    private final String providerId = "sub_12345";
    private final Long generatedId = 99999999L;

    @Test
    @DisplayName("신규 유저 가입 테스트 - DB에 없을 때 새로운 엔티티 생성")
    void joinUser_NewMember() {
        given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
        given(snowflake.nextId()).willReturn(generatedId);

        MemberEntity newMember = MemberEntity.createUserMember(generatedId, email, name, provider, providerId);
        given(memberRepository.save(any(MemberEntity.class))).willReturn(newMember);

        MemberResponse response = memberService.joinUser(email, name, provider, providerId);

        // then
        assertThat(response.getEmail()).isEqualTo(email);
        assertThat(response.getRoles()).contains(Role.USER);
        verify(memberRepository, times(1)).save(any(MemberEntity.class));
    }

    @Test
    @DisplayName("기존 유저 권한 확장 테스트 - USER가 RIDER로 가입 시 Role 추가 확인")
    void joinRider_ExistingMember_AddRole() {
        MemberEntity existingMember = MemberEntity.createUserMember(generatedId, email, name, provider, providerId);
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(existingMember));

        MemberResponse response = memberService.joinRider(email, name, provider, providerId);

        assertThat(response.getRoles()).contains(Role.USER, Role.RIDER);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("신규 라이더 가입 테스트 - DB에 없을 때 RIDER 권한으로 생성")
    void joinRider_NewMember() {
        String riderEmail = "rider_new@gmail.com";
        given(memberRepository.findByEmail(riderEmail)).willReturn(Optional.empty());
        given(snowflake.nextId()).willReturn(generatedId);

        MemberEntity newRider = MemberEntity.createRiderMember(generatedId, riderEmail, name, provider, providerId);
        given(memberRepository.save(any(MemberEntity.class))).willReturn(newRider);

        MemberResponse response = memberService.joinRider(riderEmail, name, provider, providerId);

        assertThat(response.getEmail()).isEqualTo(riderEmail);
        assertThat(response.getRoles()).contains(Role.RIDER);
        assertThat(response.getRoles()).doesNotContain(Role.USER);
        verify(memberRepository, times(1)).save(any(MemberEntity.class));
    }
}