package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RankRepository extends JpaRepository<Rank, Long> {
    Optional<Rank> findByRankName(String rankName);
    Optional<Rank> findByShortCode(String shortCode);
    List<Rank> findAllByOrderByPriorityAsc();
    boolean existsByRankName(String rankName);
}