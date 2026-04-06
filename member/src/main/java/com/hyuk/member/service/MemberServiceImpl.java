package com.hyuk.member.service;

import com.hyuk.common.Snowflake;
import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.entity.MemberEntity;
import com.hyuk.member.entity.enums.Role;
import com.hyuk.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final Snowflake snowflake;
    private final ModelMapper modelMapper;

    @Override
    public MemberResponse joinUser(String email, String name, String provider, String providerId) {
        MemberEntity member = memberRepository.findByEmail(email)
                .map(m -> {
                    m.addRole(Role.USER);
                    return m;
                })
                .orElseGet(() -> memberRepository.save(MemberEntity.createUserMember(
                        snowflake.nextId(), email, name, provider, providerId
                )));

        return modelMapper.map(member, MemberResponse.class);
    }

    @Override
    public MemberResponse joinRider(String email, String name, String provider, String providerId) {
        MemberEntity member = memberRepository.findByEmail(email)
                .map(m -> {
                    m.addRole(Role.RIDER);
                    return m;
                })
                .orElseGet(() -> memberRepository.save(MemberEntity.createRiderMember(
                        snowflake.nextId(), email, name, provider, providerId
                )));

        return modelMapper.map(member, MemberResponse.class);
    }

    @Override
    public MemberResponse createAddress(Long id, String address, String detailAddress) {
        MemberEntity member = memberRepository.findById(id).orElse(null);

        if (member == null) {
            throw new UsernameNotFoundException("회원 정보를 찾을 수 없습니다");
        }

        member.addAddress(address, detailAddress);

        return modelMapper.map(member, MemberResponse.class);
    }

    @Override
    public MemberResponse updateAddress(Long id, String address, String detailAddress) {
        MemberEntity member = memberRepository.findById(id).orElse(null);

        if (member == null) {
            throw new UsernameNotFoundException("회원 정보를 찾을 수 없습니다");
        }

        member.updateAddress(address, detailAddress);

        return modelMapper.map(member, MemberResponse.class);
    }
}
