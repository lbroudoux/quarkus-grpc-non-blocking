package org.acme.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.example.model.ApiToken;

@ApplicationScoped
public class ApiTokenRepository implements PanacheRepository<ApiToken> {

   public ApiToken findByToken(String token) {
      return find("token", token).firstResult();
   }
}
