package com.lms.lms_backend.service;

import com.lms.lms_backend.entity.Rank;
import com.lms.lms_backend.repository.RankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RankService {

    private final RankRepository rankRepository;

    public RankService(RankRepository rankRepository) {
        this.rankRepository = rankRepository;
    }

    public List<Rank> getAllRanks() {
        return rankRepository.findAllByOrderByPriorityAsc();
    }

    public Rank getRankById(Long id) {
        return rankRepository.findById(id).orElse(null);
    }

    public Rank getRankByShortCode(String shortCode) {
        return rankRepository.findByShortCode(shortCode).orElse(null);
    }

    @Transactional
    public Rank createRank(Rank rank) {
        if (rankRepository.existsByRankName(rank.getRankName())) {
            throw new RuntimeException("Rank name already exists!");
        }
        rank.setCreatedAt(LocalDateTime.now());
        rank.setUpdatedAt(LocalDateTime.now());
        return rankRepository.save(rank);
    }

    @Transactional
    public Rank updateRank(Long id, Rank rankDetails) {
        Rank rank = getRankById(id);
        if (rank == null) {
            throw new RuntimeException("Rank not found!");
        }
        rank.setRankName(rankDetails.getRankName());
        rank.setPriority(rankDetails.getPriority());
        rank.setDescription(rankDetails.getDescription());
        rank.setShortCode(rankDetails.getShortCode());
        rank.setUpdatedAt(LocalDateTime.now());
        return rankRepository.save(rank);
    }

    @Transactional
    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }

    @Transactional
    public void initializeDefaultRanks() {
        if (rankRepository.count() == 0) {
            String[][] defaultRanks = {
                    {"TAO", "1", "Trainee Accounts Officer"},
                    {"AO", "2", "Accounts Officer"},
                    {"SO", "3", "Senior Officer"},
                    {"MTO", "4", "Management Trainee Officer"},
                    {"MO", "5", "Medical Officer"},
                    {"EO", "6", "Executive Officer"},
                    {"SEO", "7", "Senior Executive Officer"},
                    {"BM", "8", "Branch Manager"},
                    {"GM", "9", "General Manager"}
            };

            for (String[] rank : defaultRanks) {
                Rank newRank = new Rank();
                newRank.setRankName(rank[0]);
                newRank.setPriority(Integer.parseInt(rank[1]));
                newRank.setDescription(rank[2]);
                newRank.setShortCode(rank[0]);
                rankRepository.save(newRank);
            }
        }
    }
}