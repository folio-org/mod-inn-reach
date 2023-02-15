package org.folio.innreach.domain.entity.base;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import java.time.OffsetDateTime;

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
public class Auditable {

  @CreatedBy
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "created_by_userid")),
      @AttributeOverride(name = "name", column = @Column(name = "created_by_username"))
  })
  private AuditableUser createdBy;

  @CreatedDate
  @Column(name = "created_date")
  private OffsetDateTime createdDate;

  @LastModifiedBy
  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "id", column = @Column(name = "updated_by_userid")),
      @AttributeOverride(name = "name", column = @Column(name = "updated_by_username"))
  })
  private AuditableUser updatedBy;

  @LastModifiedDate
  @Column(name = "updated_date")
  private OffsetDateTime updatedDate;

}
