package no.nav.eessi.pensjon

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods
import com.tngtech.archunit.lang.syntax.elements.MethodsShouldConjunction
import com.tngtech.archunit.library.dependencies.SliceRule
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.Configurations.consideringOnlyDependenciesInAnyPackage
import com.tngtech.archunit.library.plantuml.PlantUmlArchCondition.adhereToPlantUmlDiagram
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
    val componentDiagramCheck: ArchRule =
            classes().should(
                    adhereToPlantUmlDiagram(this::class.java.getResource("/components.puml"),
                            consideringOnlyDependenciesInAnyPackage("no.nav.eessi.pensjon..")))

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
