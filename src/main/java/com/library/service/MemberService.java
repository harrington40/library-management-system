package com.library.service;

import com.library.model.Member;
import com.library.repository.MemberRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(String id) {
        return memberRepository.findById(id);
    }

    public Member addMember(Member member) {
        member.setMembershipDate(LocalDate.now().toString());
        return memberRepository.save(member);
    }

    public Member updateMember(String id, Member memberDetails) {
        return memberRepository.findById(id)
                .map(member -> {
                    member.setName(memberDetails.getName());
                    member.setEmail(memberDetails.getEmail());
                    member.setPhone(memberDetails.getPhone());
                    member.setAddress(memberDetails.getAddress());
                    return memberRepository.save(member);
                })
                .orElseGet(() -> {
                    memberDetails.setId(id);
                    return memberRepository.save(memberDetails);
                });
    }

    public void deleteMember(String id) {
        memberRepository.deleteById(id);
    }

    public List<Member> searchMembersByName(String name) {
        return memberRepository.findByNameContainingIgnoreCase(name);
    }
}