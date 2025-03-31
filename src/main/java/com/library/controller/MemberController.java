package com.library.controller;

import com.library.model.Member;
import com.library.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<Member> getAllMembers() {
        return memberService.getAllMembers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Member> getMemberById(@PathVariable String id) {
        return memberService.getMemberById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Member addMember(@RequestBody Member member) {
        return memberService.addMember(member);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Member> updateMember(@PathVariable String id, @RequestBody Member memberDetails) {
        return ResponseEntity.ok(memberService.updateMember(id, memberDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable String id) {
        memberService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Member> searchByName(@RequestParam String name) {
        return memberService.searchMembersByName(name);
    }
}