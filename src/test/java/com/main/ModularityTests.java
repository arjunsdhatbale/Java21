package com.main;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    @Test
    void verifiesModularArchitecture() {
        ApplicationModules modules = ApplicationModules.of(DemoApplication.class);
        System.out.println("=== DISCOVERED APPLICATION MODULES ===");
        System.out.println(modules);
        modules.verify();
    }

    @Test
    void createModuleDocumentation() {
        ApplicationModules modules = ApplicationModules.of(DemoApplication.class);
        new Documenter(modules)
                .writeDocumentation()
                .writeModulesAsPlantUml();
    }
}
