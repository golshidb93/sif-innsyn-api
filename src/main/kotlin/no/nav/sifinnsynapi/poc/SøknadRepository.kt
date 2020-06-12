package no.nav.sifinnsynapi.poc

import org.springframework.data.jpa.repository.JpaRepository

interface SøknadRepository: JpaRepository<SøknadDAO, Long> {
    fun findAllByAktørId(aktørId: String): List<SøknadDAO>
}
