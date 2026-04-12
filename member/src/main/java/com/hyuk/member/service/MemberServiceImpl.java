package com.hyuk.member.service;

import com.hyuk.common.Snowflake;
import com.hyuk.member.dto.AddressResponse;
import com.hyuk.member.dto.MemberResponse;
import com.hyuk.member.entity.MemberEntity;
import com.hyuk.member.entity.enums.Role;
import com.hyuk.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;

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
    public MemberResponse joinSeller(String email, String name, String provider, String providerId) {
        MemberEntity member = memberRepository.findByEmail(email)
                .map(m -> {
                    m.addRole(Role.SELLER);
                    return m;
                })
                .orElseGet(() -> memberRepository.save(MemberEntity.createSellerMember(
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

    @Override
    public AddressResponse getAddress(Long id) {
        MemberEntity member = memberRepository.findById(id).orElse(null);

        if (member == null) {
            throw new RuntimeException("회원 정보를 찾을 수 없습니다");
        }

        return modelMapper.map(member, AddressResponse.class);
    }

    @Override
    public boolean hasAddress(Long id) {
        MemberEntity member = memberRepository.findById(id).orElse(null);

        if (member == null) {
            throw new RuntimeException("회원 정보를 찾을 수 없습니다");
        }

        System.out.println("member address : " + member.getAddress());

        return member.getAddress() == null;
    }

    @Override
    public MemberResponse getMyInfo(Long id) {
        MemberEntity member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다"));
        return modelMapper.map(member, MemberResponse.class);
    }

    @Override
    public void createDummyRiders() {
        double baseLat = 37.394258;
        double baseLon = 126.652129;

        for (int i = 1; i <= 100; i++) {
            Long id = snowflake.nextId();
            MemberEntity rider = MemberEntity.createRiderMember(
                    id,
                    "rider" + i + "@test.com",
                    "테스트라이더" + i,
                    "google",
                    "provider-" + id
            );

            memberRepository.save(rider);

            double lat = baseLat + (Math.random() - 0.5) * 0.04;
            double lon = baseLon + (Math.random() - 0.5) * 0.04;

            String value = String.format(
                "{\"userId\":\"%d\",\"latitude\":%f,\"longitude\":%f}", id, lat, lon
            );

            stringRedisTemplate.opsForValue().set("rider:" + id, value);
        }
    }
}
