package no.nav.eessi.pensjon.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.lang.syntax.elements.MethodsShouldConjunction
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import no.nav.eessi.pensjon.EessiPensjonBegrensInnsynApplication
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RestController

class ArchitectureTest {

    companion object {

        @JvmStatic
        private val root = EessiPensjonBegrensInnsynApplication::class.qualifiedName!!.replace("." + EessiPensjonBegrensInnsynApplication::class.simpleName, "")

        @JvmStatic
        lateinit var allClasses: JavaClasses

        @JvmStatic
        lateinit var productionClasses: JavaClasses

        @JvmStatic
        lateinit var testClasses: JavaClasses

        @BeforeAll
        @JvmStatic
        fun `extract classes`() {
            allClasses = ClassFileImporter().importPackages(root)

            productionClasses = ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .withImportOption(ImportOption.DoNotIncludeJars())
                .importPackages(root)

            testClasses = ClassFileImporter()
                .withImportOption{ !ImportOption.DoNotIncludeTests().includes(it) }
                .importPackages(root)
        }
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

    @Test
    fun `no cycles on top level`() {
        slices()
            .matching("$root.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun `no cycles on any level for production classes`() {
        slices()
            .matching("$root.(*)..")
            .should().beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun `controllers should have RestController-annotation`() {
        ArchRuleDefinition.classes().that()
            .haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(RestController::class.java)
            .check(allClasses)
    }
}

object CallCheckMethod : ArchCondition<JavaMethod>("call check()-method") {
    override fun check(method: JavaMethod?, events: ConditionEvents?) {
        requireNotNull(method); requireNotNull(events)
        if (method.methodCallsFromSelf!!.none { it.target.name == "check" && it.targetOwner.isAssignableTo(ArchRule::class.java) }) {
            events.add(SimpleConditionEvent.violated(method, "Method ${method.fullName} does not call check()"))
        }
    }

}
