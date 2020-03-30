package no.nav.eessi.pensjon

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.lang.syntax.elements.MethodsShouldConjunction
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SliceRule
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AnalyzeClasses(packagesOf = [EessiPensjonBegrensInnsynApplication::class])
class ArchitectureTest {

    @ArchTest
    val packagesShouldNotHaveCyclicDependencies: SliceRule =
            slices().matching("..(*)..").should().beFreeOfCycles()

    @ArchTest
    val servicesShouldNotDependOnEachOther: SliceRule =
            slices().matching("..services.(**)").should().notDependOnEachOther()

    @ArchTest
    fun `Check architecture`(importedClasses: JavaClasses) {

        val root = EessiPensjonBegrensInnsynApplication::class.qualifiedName!!
                .replace("." + EessiPensjonBegrensInnsynApplication::class.simpleName, "")

        layeredArchitecture()
                //Define components
                .layer("App").definedBy(root)
                .layer("Begrens Innsyn").definedBy("$root.begrens.innsyn..")
                .layer("Config").definedBy("$root.config..")
                .layer("Health").definedBy("$root.health..")
                .layer("Logging").definedBy("$root.logging..")
                .layer("Metrics").definedBy("$root.metrics..")
                .layer("Security STS").definedBy("$root.security.sts..")
                .layer("Service EUX").definedBy("$root.services.eux..")
                .layer("Service PersonV3").definedBy("$root.services.personv3..")
                //define rules
                .whereLayer("App").mayNotBeAccessedByAnyLayer()
                .whereLayer("Config").mayNotBeAccessedByAnyLayer()
                .whereLayer("Health").mayNotBeAccessedByAnyLayer()
                .whereLayer("Begrens Innsyn").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service EUX").mayOnlyBeAccessedByLayers("Begrens Innsyn")
                .whereLayer("Service PersonV3").mayOnlyBeAccessedByLayers("Begrens Innsyn")
                .whereLayer("Logging").mayOnlyBeAccessedByLayers("Config", "Security STS")
                .whereLayer("Security STS").mayOnlyBeAccessedByLayers("Config", "Service PersonV3")

                //Verify rules
                .check(importedClasses)
    }

    @ArchTest
    fun `avoid JUnit4-classes`(importedClasses: JavaClasses) {
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
                .check(importedClasses)

        noClasses()
                .should()
                .beAnnotatedWith("org.junit.runner.RunWith")
                .because(junitReason)
                .check(importedClasses)

        noMethods()
                .should()
                .beAnnotatedWith("org.junit.Test")
                .orShould().beAnnotatedWith("org.junit.Ignore")
                .because(junitReason)
                .check(importedClasses)
    }

    @ArchTest
    val everyArchTestMustCallCheck: MethodsShouldConjunction =
            methods().that().areAnnotatedWith(ArchTest::class.java).should(CallCheckMethod)
}

object CallCheckMethod : ArchCondition<JavaMethod>("call check()-method") {
    override fun check(method: JavaMethod?, events: ConditionEvents?) {
        requireNotNull(method); requireNotNull(events)
        if (method.methodCallsFromSelf!!.none { it.target.name == "check" && it.targetOwner.isAssignableTo(ArchRule::class.java) }) {
            events.add(SimpleConditionEvent.violated(method, "Method ${method.fullName} does not call check()"))
        }
    }

}
