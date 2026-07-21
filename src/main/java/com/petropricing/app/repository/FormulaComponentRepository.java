package com.petropricing.app.repository;

import com.petropricing.app.domain.FormulaComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormulaComponentRepository extends JpaRepository<FormulaComponent, Long> {
}

