package org.folio.innreach.domain.entity.base;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.util.ProxyUtils;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class AbstractEntity extends Auditable<String> implements Identifiable<UUID> {

  @Id
  private UUID id;


  protected AbstractEntity(UUID id) {
    setId(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || !getClass().equals(ProxyUtils.getUserClass(o))) {
      return false;
    }

    AbstractEntity that = (AbstractEntity) o;

    return this.getId() != null && Objects.equals(this.getId(), that.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .toString();
  }

}
