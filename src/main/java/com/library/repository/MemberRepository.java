package com.library.repository;

import com.library.model.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MemberRepository extends MongoRepository<Member, String> {
    List<Member> findByNameContainingIgnoreCase(String name);
    Member findByEmail(String email);
}