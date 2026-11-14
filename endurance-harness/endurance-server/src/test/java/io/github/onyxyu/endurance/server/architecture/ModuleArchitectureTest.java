package io.github.onyxyu.endurance.server.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 用真实生产字节码守住模块边界，新增实现同样受约束。
 */
class ModuleArchitectureTest {
    private static final String BASE = "io.github.onyxyu.endurance.";
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.onyxyu.endurance");

    @Test
    void foundationalModulesStayIndependentOfInfrastructure() {
        noClasses().that().resideInAnyPackage(BASE + "common..", BASE + "api..", BASE + "core..").should()
                .dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "com.baomidou..",
                        "org.apache.ibatis..",
                        "io.micrometer..",
                        "io.opentelemetry.."
                )
                .allowEmptyShould(true).check(CLASSES);
        noClasses().that().resideInAnyPackage(BASE + "api..", BASE + "core..").should().dependOnClassesThat()
                .resideInAnyPackage("tools.jackson..", "com.fasterxml.jackson..").allowEmptyShould(true).check(CLASSES);
        noClasses().that().resideInAPackage(BASE + "common..").and().resideOutsideOfPackage(BASE + "common.json..")
                .should().dependOnClassesThat().resideInAnyPackage("tools.jackson..", "com.fasterxml.jackson..")
                .allowEmptyShould(true).check(CLASSES);
        noClasses().that().resideInAPackage(BASE + "common..").should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + "api..",
                        BASE + "core..",
                        BASE + "models..",
                        BASE + "tools..",
                        BASE + "memory..",
                        BASE + "sandbox..",
                        BASE + "repository..",
                        BASE + "server.."
                )
                .allowEmptyShould(true).check(CLASSES);
        noClasses().that().resideInAnyPackage(BASE + "api..", BASE + "core..").should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + "models..",
                        BASE + "tools..",
                        BASE + "memory..",
                        BASE + "sandbox..",
                        BASE + "repository..",
                        BASE + "server.."
                )
                .allowEmptyShould(true).check(CLASSES);
    }

    @Test
    void apiCannotDependOnCore() {
        noClasses().that().resideInAPackage(BASE + "api..").should().dependOnClassesThat()
                .resideInAPackage(BASE + "core..").allowEmptyShould(true).check(CLASSES);
    }

    @Test
    void capabilityImplementationsCollaborateThroughApi() {
        String[] capabilities = {"models", "tools", "memory", "sandbox", "repository"};
        for (String module : capabilities) {
            for (String other : capabilities) {
                if (!module.equals(other)) {
                    noClasses().that().resideInAPackage(BASE + module + "..").should().dependOnClassesThat()
                            .resideInAPackage(BASE + other + "..").allowEmptyShould(true).check(CLASSES);
                }
            }
            noClasses().that().resideInAPackage(BASE + module + "..").should().dependOnClassesThat()
                    .resideInAnyPackage(BASE + "core..", BASE + "server..").allowEmptyShould(true).check(CLASSES);
        }
    }

    @Test
    void modulesHaveNoDependencyCycles() {
        slices().matching(BASE + "(*)..").should().beFreeOfCycles().check(CLASSES);
    }
}
