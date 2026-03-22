package com.coworking.repository;

import com.coworking.model.Feedback;
import com.coworking.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findBySpace(Space space);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.space = :space AND f.isLike = true")
    long countLikes(@Param("space") Space space);

    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.space = :space AND f.isLike = false")
    long countDislikes(@Param("space") Space space);

    @Query("""
        SELECT f.space.name, COUNT(CASE WHEN f.isLike = true THEN 1 END)
        FROM Feedback f
        GROUP BY f.space.name
        ORDER BY COUNT(CASE WHEN f.isLike = true THEN 1 END) DESC
    """)
    List<Object[]> findPopularityStats();
}
