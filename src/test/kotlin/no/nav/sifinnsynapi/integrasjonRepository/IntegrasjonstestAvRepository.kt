package no.nav.sifinnsynapi.integrasjonRepository

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.poc.SøknadDAO
import no.nav.sifinnsynapi.poc.SøknadRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
class IntegrasjonstestAvRepository {

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    companion object {
        private val aktørId = AktørId.valueOf("123456")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Lagrer søknad i repository og henter opp basert på journalpostId`() {
        val journalpostId = "12345"

        val søknadDAO = SøknadDAO(
                id = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                journalpostId = journalpostId,
                saksId = "2222",
                opprettet = LocalDateTime.now(),
                søknad =
                //language=json
                """
                    {
                        "søknadId": "05ce3630-76eb-40f4-87a3-a5d55af58e40",
                        "språk": "nb"
                    }
                    """.trimIndent()
        )
        repository.save(søknadDAO)

        val found = repository.findByJournalpostId(journalpostId)

        assert(found != null && found.journalpostId == journalpostId)
    }

    @Test
     fun `Prøver å hente opp søknad som ikke eksisterer basert på journalpostId`() {
        val journalpostId = "12345"
        val journalpostIdSomIkkeEksisterer = "54321"

        val søknadDAO = SøknadDAO(
                id = UUID.randomUUID(),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                journalpostId = journalpostId,
                saksId = "2222",
                opprettet = LocalDateTime.now(),
                søknad =
                //language=json
                """
                    {
                        "søknadId": "05ce3630-76eb-40f4-87a3-a5d55af58e40",
                        "språk": "nb"
                    }
                    """.trimIndent()
        )
        repository.save(søknadDAO)

        val found = repository.findByJournalpostId(journalpostIdSomIkkeEksisterer)

        assert(found == null)
    }
}