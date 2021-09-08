package org.folio.innreach.domain.entity.base;

import java.time.OffsetDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
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
