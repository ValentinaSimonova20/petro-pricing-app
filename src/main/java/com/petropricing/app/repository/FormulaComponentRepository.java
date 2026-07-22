package com.petropricing.app.repository;

import com.petropricing.app.domain.FormulaComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormulaComponentRepository extends JpaRepository<FormulaComponent, Long> {
    List<FormulaComponent> findByFormulaKey(String formulaKey);
}
