package com.microsoft.azure.gateway.remote;

import com.microsoft.azure.gateway.core.IGatewayModule;

public class ModuleConfiguration {
	private final String identifier;
	private final Class<? extends IGatewayModule> moduleClass;
	private final int receiveMessageDelay;

	private ModuleConfiguration(String identifier, Class<? extends IGatewayModule> moduleClass,
			int receiveMessageDelay) {
		this.identifier = identifier;
		this.moduleClass = moduleClass;
		this.receiveMessageDelay = receiveMessageDelay;
	}

	public String getIdentifier() {
		return identifier;
	}

	public Class<? extends IGatewayModule> getModuleClass() {
		return moduleClass;
	}

	public int getReceiveMessageDelay() {
		return receiveMessageDelay;
	}

	public static class Builder {
		private String identifier;
		private Class<? extends IGatewayModule> moduleClass;
		private int receiveMessageDelay;

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

		public Builder withReceiveMessageDelay(int delay) {
			this.receiveMessageDelay = delay;
			return this;
		}

		public ModuleConfiguration build() {
			return new ModuleConfiguration(this.identifier, this.moduleClass, this.receiveMessageDelay);
		}
	}
}