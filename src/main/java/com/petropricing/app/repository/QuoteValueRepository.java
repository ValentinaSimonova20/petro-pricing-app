package com.petropricing.app.repository;

import com.petropricing.app.domain.QuoteValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteValueRepository extends JpaRepository<QuoteValue, Long> {
}

