package com.mek.miniats.user.repository;

import com.mek.miniats.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    List<Profile> findAllByOrderByEmailAsc();
}
