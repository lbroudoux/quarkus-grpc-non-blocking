package org.acme.example.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends PanacheEntity {

   @Column(nullable = false)
   public String email;

   @Column(unique = true)
   public String username;
   public String firstname;
   public String lastname;

   @Column(nullable = false)
   @Enumerated(EnumType.STRING)
   public UserStatus status;


   public boolean isRegistered(){
      return status == UserStatus.REGISTERED;
   }

   public String getUserId() {
      return username;
   }
}
