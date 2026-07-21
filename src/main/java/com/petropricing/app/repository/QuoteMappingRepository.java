package com.petropricing.app.repository;

import com.petropricing.app.domain.QuoteMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteMappingRepository extends JpaRepository<QuoteMapping, Long> {
}

