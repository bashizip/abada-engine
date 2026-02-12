package com.abada.engine;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.persistence.PersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AbadaEngineApplicationTests {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private PersistenceService persistenceService;

    @Test
    void contextLoads() {
        assertThat(abadaEngine).isNotNull();
        assertThat(persistenceService).isNotNull();
    }
}
