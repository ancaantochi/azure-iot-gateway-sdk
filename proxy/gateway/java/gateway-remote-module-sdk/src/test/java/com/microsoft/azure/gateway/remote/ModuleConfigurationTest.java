/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModuleConfigurationTest {

    private static final int INVALID_VERSION = -1;
    private static final int VERSION = 1;
    private static final Class<TestModuleImplementsInterface> MODULE_CLASS = TestModuleImplementsInterface.class;
    private static final String IDENTIFIER = "Test";

    @Test
    public void buildSuccess() {
        ModuleConfiguration.Builder builder = new ModuleConfiguration.Builder();
        builder.setIdentifier(IDENTIFIER).setModuleClass(MODULE_CLASS).setModuleVersion(VERSION);

        ModuleConfiguration config = builder.build();

        assertEquals(config.getIdentifier(), IDENTIFIER);
        assertEquals(config.getModuleClass(), MODULE_CLASS);
        assertEquals(config.getVersion(), VERSION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFailWhenIdentifierNotSet() {
        ModuleConfiguration.Builder builder = new ModuleConfiguration.Builder();
        builder.setModuleClass(MODULE_CLASS).setModuleVersion(VERSION);

        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFailWhenModuleClassNotSet() {
        ModuleConfiguration.Builder builder = new ModuleConfiguration.Builder();
        builder.setIdentifier(IDENTIFIER).setModuleVersion(1);

        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFailWhenVersionNotSet() {
        ModuleConfiguration.Builder builder = new ModuleConfiguration.Builder();
        builder.setIdentifier(IDENTIFIER).setModuleClass(MODULE_CLASS);

        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFailWhenVersionIsNegative() {
        ModuleConfiguration.Builder builder = new ModuleConfiguration.Builder();
        builder.setIdentifier(IDENTIFIER).setModuleClass(MODULE_CLASS).setModuleVersion(INVALID_VERSION);

        builder.build();
    }
}
