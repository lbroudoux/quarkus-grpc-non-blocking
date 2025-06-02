package org.acme.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.example.model.Exposition;

@ApplicationScoped
public class ExpositionRepository implements PanacheRepository<Exposition> {
}
