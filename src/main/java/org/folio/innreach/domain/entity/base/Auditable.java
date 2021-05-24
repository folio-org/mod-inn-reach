package org.folio.innreach.domain.entity.base;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@ToString
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class Auditable<T> {

  @CreatedBy
  @Column(name = "created_by")
  private T createdBy;

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedBy
  @Column(name = "last_modified_by")
  private T lastModifiedBy;

  @LastModifiedDate
  @Column(name = "last_modified_date")
  private OffsetDateTime lastModifiedDate;
}
