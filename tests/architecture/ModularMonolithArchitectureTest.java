package architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Testes arquiteturais automatizados (Fitness Functions) com ArchUnit.
 * Validam o cumprimento das regras de Allowed-to-Use, ausência de ciclos
 * e encapsulamento estrito por fachadas públicas (ADR-005 e ARCHITECTURE.md).
 */
@AnalyzeClasses(packages = "pt.ulisboa.lisboapedala", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ModularMonolithArchitectureTest {

    private static final String BASE_PACKAGE = "pt.ulisboa.lisboapedala.";

    // 1. Ausência de ciclos entre os módulos de domínio
    @ArchTest
    public static final ArchRule no_cycles_between_modules =
            slices().matching("pt.ulisboa.lisboapedala.(*)..")
                    .should().beFreeOfCycles();

    // 2. Encapsulamento estrito: nenhum módulo pode aceder a classes internas (business ou persistence) de outro módulo
    @ArchTest
    public static final ArchRule no_access_to_internal_packages_of_other_modules =
            classes().that().resideInAPackage("pt.ulisboa.lisboapedala..")
                    .should(new ArchCondition<>("only access internal packages within their own module") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                            String sourceModule = extractModuleName(javaClass.getPackageName());
                            if (sourceModule == null || sourceModule.equals("common")) {
                                return;
                            }

                            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                                String targetPackage = dependency.getTargetClass().getPackageName();
                                String targetModule = extractModuleName(targetPackage);

                                if (targetModule != null && !targetModule.equals("common") && !targetModule.equals(sourceModule)) {
                                    if (targetPackage.contains(".business") || targetPackage.contains(".persistence")) {
                                        String message = String.format(
                                                "Violação de Encapsulamento (ADR-005): Classe %s no módulo [%s] acede à classe interna %s no módulo [%s]",
                                                javaClass.getName(), sourceModule, dependency.getTargetClass().getName(), targetModule
                                        );
                                        events.add(SimpleConditionEvent.violated(dependency, message));
                                    }
                                }
                            }
                        }
                    });

    // 3. Allowed-to-Use: tariff_billing é um módulo folha e não pode depender de nenhum outro módulo de domínio
    @ArchTest
    public static final ArchRule tariff_billing_should_not_depend_on_other_domain_modules =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.tariff_billing..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.trip_lifecycle..",
                            "pt.ulisboa.lisboapedala.station_availability..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("tariff_billing não pode usar nenhum outro módulo de domínio (ARCHITECTURE.md seção 3)");

    // 4. Allowed-to-Use: sensor_ingestion não pode depender de nenhum outro módulo de domínio
    @ArchTest
    public static final ArchRule sensor_ingestion_should_not_depend_on_other_domain_modules =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.sensor_ingestion..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.trip_lifecycle..",
                            "pt.ulisboa.lisboapedala.tariff_billing..",
                            "pt.ulisboa.lisboapedala.station_availability..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("sensor_ingestion não pode usar nenhum outro módulo de domínio (ARCHITECTURE.md seção 3)");

    // 5. Allowed-to-Use: dock_access só pode usar trip_lifecycle e station_availability
    @ArchTest
    public static final ArchRule dock_access_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.dock_access..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.tariff_billing..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("dock_access só pode usar trip_lifecycle e station_availability");

    // 6. Allowed-to-Use: trip_lifecycle só pode usar tariff_billing e station_availability
    @ArchTest
    public static final ArchRule trip_lifecycle_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.trip_lifecycle..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("trip_lifecycle só pode usar tariff_billing e station_availability");

    // 7. Allowed-to-Use: maintenance_orders só pode usar sensor_ingestion e station_availability
    @ArchTest
    public static final ArchRule maintenance_orders_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.maintenance_orders..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.trip_lifecycle..",
                            "pt.ulisboa.lisboapedala.tariff_billing..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("maintenance_orders só pode usar sensor_ingestion");

    // 8. Allowed-to-Use: reporting_audit só pode usar trip_lifecycle e station_availability
    @ArchTest
    public static final ArchRule reporting_audit_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.reporting_audit..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.tariff_billing..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("reporting_audit só pode usar trip_lifecycle e station_availability");

    // 9. Allowed-to-Use: history_reader só pode usar trip_lifecycle e tariff_billing
    @ArchTest
    public static final ArchRule history_reader_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.history_reader..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.station_availability..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.fleet_redistribution..",
                            "pt.ulisboa.lisboapedala.reporting_audit.."
                    )
                    .because("history_reader só pode usar trip_lifecycle e tariff_billing (ARCHITECTURE.md seção 3)");

    // 10. Allowed-to-Use: fleet_redistribution só pode usar station_availability
    @ArchTest
    public static final ArchRule fleet_redistribution_allowed_dependencies =
            noClasses().that().resideInAPackage("pt.ulisboa.lisboapedala.fleet_redistribution..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "pt.ulisboa.lisboapedala.dock_access..",
                            "pt.ulisboa.lisboapedala.trip_lifecycle..",
                            "pt.ulisboa.lisboapedala.tariff_billing..",
                            "pt.ulisboa.lisboapedala.sensor_ingestion..",
                            "pt.ulisboa.lisboapedala.maintenance_orders..",
                            "pt.ulisboa.lisboapedala.reporting_audit..",
                            "pt.ulisboa.lisboapedala.history_reader.."
                    )
                    .because("fleet_redistribution só pode usar station_availability (ARCHITECTURE.md seção 3)");

    private static String extractModuleName(String packageName) {
        if (!packageName.startsWith(BASE_PACKAGE)) {
            return null;
        }
        String rest = packageName.substring(BASE_PACKAGE.length());
        int dot = rest.indexOf('.');
        return dot == -1 ? rest : rest.substring(0, dot);
    }
}
