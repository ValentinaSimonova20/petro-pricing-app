package com.petropricing.app.repository;

import com.petropricing.app.domain.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {
}

