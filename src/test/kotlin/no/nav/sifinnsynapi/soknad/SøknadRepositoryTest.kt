package no.nav.sifinnsynapi.soknad

import assertk.assertions.isEqualTo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import org.junit.Assert.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class SøknadRepositoryTest {

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    companion object {
        private val aktørId = AktørId.valueOf("123456")
        private val aktørIdSomIkkeEksisterer = AktørId.valueOf("54321")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val journalpostId = "12345"
    }

    @BeforeAll
    internal fun setUp() {
        assertNotNull(repository)
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Lagrer søknad i repository og henter opp basert på journalpostId`() {
        var søknadHentetFraRepository = repository.findByJournalpostId(journalpostId)
        assert(søknadHentetFraRepository == null)

        val søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        søknadHentetFraRepository = repository.findByJournalpostId(journalpostId)

        assert(søknadHentetFraRepository != null && søknadHentetFraRepository.journalpostId == journalpostId && søknadHentetFraRepository.aktørId == aktørId)
    }

    @Test
    fun `Prøver å hente opp søknad som ikke eksisterer basert på journalpostId`() {
        val journalpostIdSomIkkeEksisterer = "54321"

        val søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        val søknadHentetFraRepository = repository.findByJournalpostId(journalpostIdSomIkkeEksisterer)

        assert(søknadHentetFraRepository == null)
    }

    @Test
    fun `Lagre to søknader med samme aktørId i repository og finne de basert på aktørId`() {
        var søknaderHentetFraRepository = repository.findAllByAktørId(aktørId)
        assert(søknaderHentetFraRepository.size == 0)

        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        søknaderHentetFraRepository = repository.findAllByAktørId(aktørId)

        assert(søknaderHentetFraRepository.size == 2)
    }

    @Test
    fun `Hente søknader som ikke finnes basert på aktørId`() {
        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        val søknaderHentetFraRepository = repository.findAllByAktørId(aktørIdSomIkkeEksisterer)

        assert(søknaderHentetFraRepository.isEmpty())
    }

    @Test
    fun `Sjekke som søknad eksisterer basert på aktørId og journalpostId`() {
        var eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostId)
        assertFalse(eksistererSøknad)

        val søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostId)

        assert(eksistererSøknad)
    }

    @Test
    fun `Sjekke om søknad eksisterer ved bruk av aktørId som ikke eksisterer`() {
        val søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        val eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørIdSomIkkeEksisterer, journalpostId)

        assertFalse(eksistererSøknad)
    }

    @Test
    fun `Lagrer to ulike søknader med forskjellig journalpostId men lik aktørId`() {
        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        var eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostId)
        assertTrue(eksistererSøknad)

        val journalpostIdSomIkkeEksisterer = "222222"
        eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostIdSomIkkeEksisterer)
        assertFalse(eksistererSøknad)

        val ulikJournalpostId = "1111111"
        søknadDAO = lagSøknadDAO(customJournalpostId = ulikJournalpostId)
        repository.save(søknadDAO)
        eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, ulikJournalpostId)
        assertTrue(eksistererSøknad)
    }

    @Test
    fun `gitt søknader i database, forvent 2 unike brukere`() {
        repository.saveAll(listOf(
                lagSøknadDAO(),
                lagSøknadDAO(customAktørId = AktørId("789010")),
                lagSøknadDAO(customAktørId = AktørId("789010"))
        ))

        assertk.assertThat(repository.finnAntallUnikeSøkere()).isEqualTo(2)
    }


    @Test
    fun `gitt lagrede søknader, forvent 1 av hver søknadstype`() {
        repository.saveAll(listOf(
                lagSøknadDAO(søknadstype = Søknadstype.PP_SYKT_BARN),
                lagSøknadDAO(søknadstype = Søknadstype.PP_ETTERSENDING),
                lagSøknadDAO(søknadstype = Søknadstype.OMP_UTVIDET_RETT),
                lagSøknadDAO(søknadstype = Søknadstype.OMP_UTBETALING_ARBEIDSTAKER),
                lagSøknadDAO(søknadstype = Søknadstype.OMP_UTBETALING_SNF),
                lagSøknadDAO(søknadstype = Søknadstype.OMP_ETTERSENDING),
                lagSøknadDAO(søknadstype = Søknadstype.OMD_OVERFØRING)
        )).forEach {
            assertk.assertThat(repository.finnAntallSøknaderGittSøknadstype(it.søknadstype.name)).isEqualTo(1)
        }
    }


    private fun lagSøknadDAO(
            customAktørId: AktørId = aktørId,
            customJournalpostId: String = journalpostId,
            søknadstype: Søknadstype = Søknadstype.OMP_UTBETALING_SNF
    ): SøknadDAO = SøknadDAO(
            id = UUID.randomUUID(),
            aktørId = customAktørId,
            fødselsnummer = fødselsnummer,
            søknadstype = søknadstype,
            status = SøknadsStatus.MOTTATT,
            journalpostId = customJournalpostId,
            saksId = "2222",
            opprettet = ZonedDateTime.now(),
            søknad =
            //language=json
            """
                    {
                        "søknadId": "05ce3630-76eb-40f4-87a3-a5d55af58e40",
                        "språk": "nb"
                    }
                    """.trimIndent()
    )
}
