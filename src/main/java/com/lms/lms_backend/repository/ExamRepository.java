package com.lms.lms_backend.repository;

import com.lms.lms_backend.entity.Exam;
import com.lms.lms_backend.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByStatus(String status);
    List<Exam> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.startTime <= :currentTime AND e.endTime >= :currentTime")
    List<Exam> findActiveExams(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.fromRank.priority BETWEEN :minPriority AND :maxPriority")
    List<Exam> findExamsByRankPriority(@Param("minPriority") Integer minPriority, @Param("maxPriority") Integer maxPriority);

    List<Exam> findByFromRankAndToRank(Rank fromRank, Rank toRank);
}