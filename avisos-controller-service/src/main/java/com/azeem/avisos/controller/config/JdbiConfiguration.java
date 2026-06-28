/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

package com.azeem.avisos.controller.config;

import com.azeem.avisos.controller.repository.AlarmAnalysisRepository;
import com.azeem.avisos.controller.repository.AlarmRepository;
import com.azeem.avisos.controller.repository.NodeFleetMetricRepository;
import com.azeem.avisos.controller.repository.NodeRepository;
import com.azeem.avisos.controller.repository.StaffRepository;
import com.azeem.avisos.controller.repository.TelemetryRepository;
import com.azeem.avisos.controller.security.repository.AuthRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Replaces {@code JdbiProvider} -- creates the DataSource, Jdbi, and all JDBI repositories. */
@Configuration
public class JdbiConfiguration {

  @Bean
  public DataSource avisosDataSource(AvisosDatabaseProperties dbProps) {
    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(dbProps.url());
    hikariConfig.setMaximumPoolSize(5);
    hikariConfig.setMinimumIdle(1);
    hikariConfig.setPoolName("avisos-sqlite-pool");

    return new HikariDataSource(hikariConfig);
  }

  @Bean
  public Jdbi jdbi(@Qualifier("avisosDataSource") DataSource avisosDataSource) {
    Jdbi jdbi = Jdbi.create(avisosDataSource);
    jdbi.installPlugin(new SqlObjectPlugin());
    return jdbi;
  }

  @Bean
  public AlarmRepository alarmRepository(Jdbi jdbi) {
    AlarmRepository repo = jdbi.onDemand(AlarmRepository.class);
    repo.initAlarmTable();
    if (repo.countColumn("s3_image_key") == 0) {
      repo.addS3ImageKeyColumn();
    }
    return repo;
  }

  @Bean
  public NodeFleetMetricRepository nodeFleetMetricRepository(Jdbi jdbi) {
    NodeFleetMetricRepository repo = jdbi.onDemand(NodeFleetMetricRepository.class);
    repo.initNodeFleetMetricTable();
    return repo;
  }

  @Bean
  public AlarmAnalysisRepository alarmAnalysisRepository(Jdbi jdbi) {
    AlarmAnalysisRepository repo = jdbi.onDemand(AlarmAnalysisRepository.class);
    repo.initAnalysisTable();
    return repo;
  }

  @Bean
  public NodeRepository nodeRepository(Jdbi jdbi) {
    NodeRepository repo = jdbi.onDemand(NodeRepository.class);
    repo.initNodeTable();
    return repo;
  }

  @Bean
  public TelemetryRepository telemetryRepository(Jdbi jdbi) {
    TelemetryRepository repo = jdbi.onDemand(TelemetryRepository.class);
    repo.initAuditTable();
    return repo;
  }

  @Bean
  public StaffRepository staffRepository(Jdbi jdbi) {
    StaffRepository repo = jdbi.onDemand(StaffRepository.class);
    repo.initStaffTable();
    return repo;
  }

  @Bean
  public AuthRepository authRepository(Jdbi jdbi) {
    AuthRepository repo = jdbi.onDemand(AuthRepository.class);
    repo.initUserTable();
    return repo;
  }
}
