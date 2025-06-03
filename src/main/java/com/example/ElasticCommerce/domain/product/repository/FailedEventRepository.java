package com.example.ElasticCommerce.domain.product.repository;

import com.example.ElasticCommerce.domain.product.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEvent, Long> {

    // retryCount가 MAX_RETRY_COUNT 미만인 것만 조회할 때 사용
    List<FailedEvent> findByRetryCountLessThan(int maxRetryCount);

}