package com.petropricing.app.repository;

import com.petropricing.app.domain.Formula;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormulaRepository extends JpaRepository<Formula, Long> {
}

