package no.kartverket.nibas.config

import org.hibernate.envers.AuditReader
import org.hibernate.envers.AuditReaderFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import javax.persistence.EntityManagerFactory

@Configuration
@EnableJpaAuditing()
class JpaAuditingConfig(val entityManagerFactory: EntityManagerFactory) {

    @Bean
    fun auditReader(): AuditReader? {
        return AuditReaderFactory.get(entityManagerFactory.createEntityManager())
    }

}