package co.datacenter.validacion.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<OrdenEntity, String> {

    Optional<OrdenEntity> findByReferencia(String referencia);
}
