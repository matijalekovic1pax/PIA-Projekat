package com.coworking.repository;

import com.coworking.model.Reservation;
import com.coworking.model.Space;
import com.coworking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberOrderByStartDateTimeDesc(User member);

    List<Reservation> findBySpace(Space space);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.space = :space
        AND r.type = :type
        AND r.status NOT IN ('CANCELLED')
        AND r.startDateTime < :end
        AND r.endDateTime > :start
    """)
    List<Reservation> findConflictingReservations(
        @Param("space") Space space,
        @Param("type") String type,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.space IN :spaces
        AND r.status NOT IN ('CANCELLED')
        AND r.startDateTime >= :start
        AND r.startDateTime < :end
    """)
    List<Reservation> findManagerCalendarReservations(
        @Param("spaces") List<Space> spaces,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.space IN :spaces
        AND r.status NOT IN ('CANCELLED')
        AND r.startDateTime >= :start
        AND r.startDateTime < :end
    """)
    List<Reservation> findForReport(
        @Param("spaces") List<Space> spaces,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    @Query("SELECT r FROM Reservation r WHERE r.space.manager = :manager AND r.status NOT IN ('CANCELLED')")
    List<Reservation> findByManagerAndNotCancelled(@Param("manager") User manager);
}
