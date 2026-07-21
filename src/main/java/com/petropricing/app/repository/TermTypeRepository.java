package com.petropricing.app.repository;

import com.petropricing.app.domain.TermType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermTypeRepository extends JpaRepository<TermType, Long> {
}

