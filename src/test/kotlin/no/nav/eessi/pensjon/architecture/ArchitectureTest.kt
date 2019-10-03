package no.nav.eessi.pensjon.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import no.nav.eessi.pensjon.EessiPensjonBegrensInnsynApplication
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class ArchitectureTest {

    companion object {

        @JvmStatic
        private val root = EessiPensjonBegrensInnsynApplication::class.qualifiedName!!
                .replace("." + EessiPensjonBegrensInnsynApplication::class.simpleName, "")

        @JvmStatic
        lateinit var classesToAnalyze: JavaClasses

        @BeforeAll
        @JvmStatic
        fun `extract classes`() {
            classesToAnalyze = ClassFileImporter().importPackages(root)

            assertTrue(classesToAnalyze.size > 150, "Sanity check on no. of classes to analyze")
            assertTrue(classesToAnalyze.size < 800, "Sanity check on no. of classes to analyze")
        }
    }

    @Test
    fun `Packages should not have cyclic depenedencies`() {
        slices().matching("$root.(*)..").should().beFreeOfCycles().check(classesToAnalyze)
    }


    @Test
    fun `Services should not depend on eachother`() {
        slices().matching("..$root.services.(**)").should().notDependOnEachOther().check(classesToAnalyze)
    }

    @Test
    fun `Check architecture`() {
        val ROOT = "begrens.innsyn"
        val Config = "begrens.innsyn.Config"
        val Health = "begrens.innsyn.Health"
        val JSON = "begrens.innsyn.json"
        val Listeners = "begrens.innsyn.listeners"
        val Logging = "begrens.innsyn.logging"
        val Metrics = "begrens.innsyn.metrics"
        val STS = "begrens.innsyn.security.sts"
        val EuxService = "begrens.innsyn.services.eux"
        val PersonV3Service = "begrens.innsyn.services.personv3"

        val packages: Map<String, String> = mapOf(
                ROOT to root,
                Config to "$root.config",
                Health to "$root.health",
                // TODO BegrensInnsyn to "$root.begrens.innsyn",
                JSON to "$root.json",
                Listeners to "$root.listeners",
                Logging to "$root.logging",
                // TODO Logging to "$root.logging",begrens.innsyn
                Metrics to "$root.metrics",
                STS to "$root.security.sts",
                EuxService to "$root.services.eux",
                PersonV3Service to "$root.services.personv3"
        )

        /*
        TODO do something about the dependencies surrounding STS, but there is a bit too much black magic there for me ...
        TODO look at/refactor the relationship between journalforing.JournalpostModel and services.journalpost.JournalpostService ...
         */
        layeredArchitecture()
                //Define components
                .layer(ROOT).definedBy(packages[ROOT])
                .layer(Config).definedBy(packages[Config])
                .layer(Health).definedBy(packages[Health])
                .layer(JSON).definedBy(packages[JSON])
                .layer(Listeners).definedBy(packages[Listeners])
                .layer(Logging).definedBy(packages[Logging])
                .layer(Metrics).definedBy(packages[Metrics])
                .layer(STS).definedBy(packages[STS])
                .layer(EuxService).definedBy(packages[EuxService])
                .layer(PersonV3Service).definedBy(packages[PersonV3Service])
                //define rules
                .whereLayer(ROOT).mayNotBeAccessedByAnyLayer()
                .whereLayer(Config).mayNotBeAccessedByAnyLayer()
                .whereLayer(Health).mayNotBeAccessedByAnyLayer()
                .whereLayer(Listeners).mayOnlyBeAccessedByLayers(ROOT)
                .whereLayer(Logging).mayOnlyBeAccessedByLayers(Config, STS)
                .whereLayer(STS).mayOnlyBeAccessedByLayers(Config, PersonV3Service)
/*              .whereLayer(EuxService).mayOnlyBeAccessedByLayers(Journalforing)
                .whereLayer(PersonV3Service).mayOnlyBeAccessedByLayers(ROOT, Journalforing)
                TODO legg til disse når BegrensInnsyn er klar
*/
                //Verify rules
                .check(classesToAnalyze)
    }

    @Test
    fun `avoid JUnit4-classes`() {
        val junitReason = "We use JUnit5 (but had to include JUnit4 because spring-kafka-test needs it to compile)"

        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.junit",
                        "org.junit.runners",
                        "org.junit.experimental..",
                        "org.junit.function",
                        "org.junit.matchers",
                        "org.junit.rules",
                        "org.junit.runner..",
                        "org.junit.validator"
                ).because(junitReason)
                .check(classesToAnalyze)

                noClasses()
                        .should()
                        .beAnnotatedWith("org.junit.runner.RunWith")
                        .because(junitReason)
                        .check(classesToAnalyze)

                noMethods()
                        .should()
                        .beAnnotatedWith("org.junit.Test")
                        .orShould().beAnnotatedWith("org.junit.Ignore")
                        .because(junitReason)
                        .check(classesToAnalyze)
    }
}
