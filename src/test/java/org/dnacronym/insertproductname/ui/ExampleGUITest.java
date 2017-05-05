package org.dnacronym.insertproductname.ui;

import org.dnacronym.insertproductname.ui.runnable.DNAApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ExampleGUITest extends FxRobot {
    @BeforeAll
    static void beforeAll() throws TimeoutException {
        FxToolkit.registerPrimaryStage();
        FxToolkit.showStage();
    }

    @BeforeEach
    final void beforeEach() throws IOException, TimeoutException {
        FxToolkit.setupApplication(DNAApplication.class);
    }

    @AfterEach
    final void afterEach() throws TimeoutException {
        FxToolkit.hideStage();
    }

    @AfterAll
    static void afterAll() {
        System.exit(0);
    }


    @Test
    public final void simpleFxRobotSample() {
        moveTo("#fileMenu");
    }
}