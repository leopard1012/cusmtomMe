package com.custom.me.order.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByOrderNumber(String orderNumber);

    long countByOrderStatus(String status);

    Product findFirstByOrderStatusOrderByIdAsc(String status);

    List<Product> findByOrderStatusOrderByIdAsc(String status);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "update product set order_status = :nextStatus where order_status = :nowStatus", nativeQuery = true)
    void updateProductStatus(@Param("nowStatus") String nowStatus, @Param("nextStatus") String nextStatus);
}
