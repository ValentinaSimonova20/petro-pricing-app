package com.petropricing.app.repository;

import com.petropricing.app.domain.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {

    @Query("select distinct d.period from DemandForecast d where d.period is not null order by d.period")
    List<LocalDate> findDistinctPeriods();
}
