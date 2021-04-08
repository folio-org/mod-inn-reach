package org.folio.innreach.domain.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ClientKey {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String key1;
    private String key2;

    public ClientKey() {
    }

    public ClientKey(String key1, String key2) {
        this.key1 = key1;
        this.key2 = key2;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey1() {
        return key1;
    }

    public void setKey1(String key1) {
        this.key1 = key1;
    }

    public String getKey2() {
        return key2;
    }

    public void setKey2(String key2) {
        this.key2 = key2;
    }
}
