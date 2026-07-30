package com.jesusbarocio.bedererai.repository;

import com.jesusbarocio.bedererai.entity.SwingAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SwingAnalysisRepository extends JpaRepository<SwingAnalysis, Long> {
    List<SwingAnalysis> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
