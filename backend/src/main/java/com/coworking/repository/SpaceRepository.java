package com.coworking.repository;

import com.coworking.model.Space;
import com.coworking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {

    List<Space> findByManager(User manager);

    List<Space> findByStatus(String status);

    @Query("SELECT DISTINCT s.city FROM Space s WHERE s.status = 'ACTIVE' ORDER BY s.city")
    List<String> findDistinctCities();

    @Query("SELECT COUNT(s) FROM Space s WHERE s.status = 'ACTIVE'")
    long countActive();

    @Query("""
        SELECT s FROM Space s
        WHERE s.status = 'ACTIVE'
        AND (:name IS NULL OR :name = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:cities IS NULL OR s.city IN :cities)
    """)
    List<Space> searchSpaces(
        @Param("name") String name,
        @Param("cities") List<String> cities
    );

    @Query("""
        SELECT s FROM Space s
        LEFT JOIN s.feedbacks f
        WHERE s.status = 'ACTIVE'
        GROUP BY s
        ORDER BY COUNT(CASE WHEN f.isLike = true THEN 1 END) DESC
    """)
    List<Space> findTopSpaces();
}
