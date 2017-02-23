package com.microsoft.azure.gateway.remote;

import com.microsoft.azure.gateway.core.IGatewayModule;

public class ModuleConfiguration {
	private final String identifier;
	private final Class<? extends IGatewayModule> moduleClass;

	private ModuleConfiguration(String identifier, Class<? extends IGatewayModule> moduleClass) {
		this.identifier = identifier;
		this.moduleClass = moduleClass;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Class<? extends IGatewayModule> getModuleClass() {
		return moduleClass;
	}

	public static class Builder {
		private String identifier;
		private Class<? extends IGatewayModule> moduleClass;

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

		public ModuleConfiguration build() {
		    if (this.identifier == null || this.moduleClass == null)
		        throw new IllegalArgumentException("Identifier and module class are required.");
			return new ModuleConfiguration(this.identifier, this.moduleClass);
		}
	}
}