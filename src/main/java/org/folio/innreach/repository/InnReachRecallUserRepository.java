package org.folio.innreach.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.folio.innreach.domain.entity.InnReachRecallUser;

public interface InnReachRecallUserRepository extends JpaRepository<InnReachRecallUser, UUID> {
}
