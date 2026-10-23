package com.orderhub.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Fitness Functions Arquiteturais (ARQ-01 a ARQ-05).
 * Validação rigorosa dos limites arquiteturais usando ArchUnit.
 */
class ArchitectureFitnessTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.orderhub");
    }

    @Test
    @DisplayName("ARQ-01: domain não importa nada fora de si mesmo (nem frameworks, nem application, nem adapters)")
    void arq01_domainNaoImportaNadaForaDeSi() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application..",
                "..adapters..",
                "..bootstrap..",
                "org.springframework..",
                "jakarta.."
            )
            .check(classes);
    }

    @Test
    @DisplayName("ARQ-02: application importa apenas domain e suas próprias portas")
    void arq02_applicationNaoImportaAdaptersNemBootstrap() {
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapters..",
                "..bootstrap..",
                "org.springframework.."
            )
            .check(classes);
    }

    @Test
    @DisplayName("ARQ-03: adapters in não importam adapters out")
    void arq03_adaptersInNaoImportamAdaptersOut() {
        noClasses().that().resideInAPackage("..adapters.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapters.out..")
            .check(classes);
    }

    @Test
    @DisplayName("ARQ-03b: adapters não importam bootstrap")
    void arq03b_adaptersNaoImportamBootstrap() {
        noClasses().that().resideInAPackage("..adapters..")
            .should().dependOnClassesThat().resideInAPackage("..bootstrap..")
            .check(classes);
    }

    @Test
    @DisplayName("ARQ-04: Somente bootstrap conhece classes concretas de todos os adaptadores")
    void arq04_apenasBootstrapConheceTodosAdaptadoresConcretos() {
        noClasses().that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapters..")
            .check(classes);
    }

    @Test
    @DisplayName("ARQ-05: Não há ciclos entre pacotes")
    void arq05_naoHaCiclosEntrePacotes() {
        slices().matching("com.orderhub.(*)..")
            .should().beFreeOfCycles()
            .check(classes);
    }
}

