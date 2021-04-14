package org.folio.innreach.domain.entity;

import lombok.*;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name", "localServerCode"})
@Entity
@Table(name = "central_server")
public class CentralServer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String name;
  private String description;

  @Column(name = "local_server_code")
  private String localServerCode;

  @Embedded
  private CentralServerConfiguration centralServerConfiguration;
}
