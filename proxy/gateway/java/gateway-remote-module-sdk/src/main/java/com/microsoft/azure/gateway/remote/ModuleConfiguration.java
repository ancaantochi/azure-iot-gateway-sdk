/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import com.microsoft.azure.gateway.core.IGatewayModule;

public class ModuleConfiguration {
    private final String identifier;
    private final Class<? extends IGatewayModule> moduleClass;
    private final int version;

    private ModuleConfiguration(String identifier, Class<? extends IGatewayModule> moduleClass, int version) {
        this.identifier = identifier;
        this.moduleClass = moduleClass;
        this.version = version;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Class<? extends IGatewayModule> getModuleClass() {
        return moduleClass;
    }

    public int getVersion() {
        return version;
    }

    public static class Builder {
        private String identifier;
        private Class<? extends IGatewayModule> moduleClass;
        private int version;

        public Builder() {
        }

        public Builder setModuleClass(Class<? extends IGatewayModule> moduleClass) {
            this.moduleClass = moduleClass;
            return this;
        }

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setModuleVersion(int version) {
            this.version = version;
            return this;
        }

        public ModuleConfiguration build() {
            if (this.identifier == null || this.moduleClass == null)
                throw new IllegalArgumentException("Identifier and module class are required.");
            if (this.version <= 0)
                throw new IllegalArgumentException("Version can not have negative value or zero.");

            return new ModuleConfiguration(this.identifier, this.moduleClass, this.version);
        }
    }
}