package com.algotalk.userservice.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.algotalk.userservice",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {
    // 패키지 의존성 규칙
    @ArchTest
    static final ArchRule service_should_not_depend_on_controller =
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..controller..")
                    .allowEmptyShould(true)
                    .because("Service는 Controller에 의존하면 안 됩니다.");

    @ArchTest
    static final ArchRule repository_should_not_depend_on_service =
            noClasses().that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..service..")
                    .allowEmptyShould(true)
                    .because("Repository(Mapper)는 Service에 의존하면 안 됩니다.");

    @ArchTest
    static final ArchRule dto_should_not_depend_on_entity =
            noClasses().that().resideInAPackage("..dto.request..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..domain.entity..")
                    .allowEmptyShould(true)
                    .because("RequestDTO는 Entity에 의존하면 안 됩니다.");

    // 네이밍 규칙
    @ArchTest
    static final ArchRule controller_naming =
            classes().that().resideInAPackage("..controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .allowEmptyShould(true)
                    .because("Controller 클래스는 Controller로 끝나야 합니다.");

    @ArchTest
    static final ArchRule service_naming =
            classes().that().resideInAPackage("..service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .allowEmptyShould(true)
                    .because("Service 클래스는 Service로 끝나야 합니다.");

    @ArchTest
    static final ArchRule request_dto_naming =
            classes().that().resideInAPackage("..dto.request..")
                    // @Builder가 생성하는 내부 Builder 클래스 제외
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .should().haveSimpleNameEndingWith("DTO")
                    .allowEmptyShould(true)
                    .because("RequestDTO 클래스는 DTO로 끝나야 합니다.");

    @ArchTest
    static final ArchRule response_dto_naming =
            classes().that().resideInAPackage("..dto.response..")
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .should().haveSimpleNameEndingWith("DTO")
                    .allowEmptyShould(true)
                    .because("ResponseDTO 클래스는 DTO로 끝나야 합니다.");

    @ArchTest
    static final ArchRule command_dto_naming =
            classes().that().resideInAPackage("..dto.command..")
                    .and().haveSimpleNameNotEndingWith("Builder")
                    .should().haveSimpleNameEndingWith("Command")
                    .allowEmptyShould(true)
                    .because("Command 클래스는 Command로 끝나야 합니다.");
}