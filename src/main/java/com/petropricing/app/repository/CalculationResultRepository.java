package com.petropricing.app.repository;

import com.petropricing.app.domain.CalculationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculationResultRepository extends JpaRepository<CalculationResult, Long> {
}
