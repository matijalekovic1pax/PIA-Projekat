package com.coworking.repository;

import com.coworking.model.Space;
import com.coworking.model.SpaceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpaceItemRepository extends JpaRepository<SpaceItem, Long> {
    List<SpaceItem> findBySpaceAndType(Space space, String type);
    List<SpaceItem> findBySpace(Space space);
}
