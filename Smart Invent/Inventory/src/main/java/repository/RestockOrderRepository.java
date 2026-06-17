package com.stockwise.api.repository;

import com.stockwise.api.entity.RestockOrder;
import com.stockwise.api.entity.RestockOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestockOrderRepository extends JpaRepository<RestockOrder, Long> {
    List<RestockOrder> findByStatusOrderByCreatedAtDesc(RestockOrderStatus status);

    List<RestockOrder> findTop100ByOrderByCreatedAtDesc();

    List<RestockOrder> findAllByOrderByCreatedAtDesc();
}
