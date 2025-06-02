package org.acme.example.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

@Entity
@Table(name = "api_tokens")
public class ApiToken extends PanacheEntity {

   public String name;
   public String token;

   @Temporal(TemporalType.TIMESTAMP)
   @Column(name = "valid_until", nullable = false, columnDefinition = "TIMESTAMP")
   public Date validUntil;

   @ManyToOne(fetch = FetchType.EAGER)
   public User user;

   public boolean isValid() {
      return validUntil != null && validUntil.after(new Date());
   }
}
