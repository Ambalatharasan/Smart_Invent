package com.stockwise.api.repository;

import com.stockwise.api.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    @Query("select distinct i.category from InventoryItem i order by i.category asc")
    List<String> findDistinctCategories();

    @Query("select i.sku from InventoryItem i where upper(i.sku) like 'ITEM-%'")
    List<String> findGeneratedItemNumbers();

    @Query("""
            select i from InventoryItem i
            where lower(i.name) like lower(concat('%', :term, '%'))
               or lower(i.sku) like lower(concat('%', :term, '%'))
               or lower(i.category) like lower(concat('%', :term, '%'))
               or lower(i.supplier) like lower(concat('%', :term, '%'))
               or lower(i.location) like lower(concat('%', :term, '%'))
            order by i.name asc
            """)
    List<InventoryItem> search(@Param("term") String term);

    @Query("""
            select i from InventoryItem i
            where (:term is null
               or lower(i.name) like lower(concat('%', :term, '%'))
               or lower(i.sku) like lower(concat('%', :term, '%'))
               or lower(i.category) like lower(concat('%', :term, '%'))
               or lower(i.supplier) like lower(concat('%', :term, '%'))
               or lower(i.location) like lower(concat('%', :term, '%')))
              and (:category is null or lower(i.category) = lower(:category))
              and (:status is null
               or (:status = 'OUT' and i.quantity = 0)
               or (:status = 'LOW' and i.quantity > 0 and i.quantity <= i.reorderPoint)
               or (:status = 'HEALTHY' and i.quantity > i.reorderPoint))
            order by i.name asc
            """)
    List<InventoryItem> findFiltered(
            @Param("term") String term,
            @Param("category") String category,
            @Param("status") String status
    );

    @Query("select i from InventoryItem i where i.quantity <= i.reorderPoint order by i.quantity asc, i.name asc")
    List<InventoryItem> findRestockCandidates();
}
