package com.microsoft.azure.gateway.remote;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

public class GatewayProxy {

	private static final int DEFAULT_DELAY_MS = 200;
	private static final GatewayProxy INSTANCE = new GatewayProxy();

	private GatewayProxy() {
	}
	
	public static GatewayProxy getInstance() {
		return INSTANCE;
	}

	public RemoteModule attach(ModuleConfiguration config) throws ConnectionException {
		RemoteModuleImpl moduleProxy = new RemoteModuleImpl(config);
		CommunicationEndpoint controlEndpoint = new CommunicationControlEndpoint(config.getIdentifier());
		controlEndpoint.connect();
		moduleProxy.addEndpoint(controlEndpoint);
		Future<?> future = this.startListening(moduleProxy, config);
		moduleProxy.setFuture(future);

		return moduleProxy;
	}

	public void detach(RemoteModuleImpl moduleProxy) {
		moduleProxy.getFuture().cancel(true);
	}

	private Future<?> startListening(RemoteModuleImpl moduleProxy, ModuleConfiguration config) {
		Runnable receiveMessage = new ReceiveMessageTask(moduleProxy);
		int delay = config.getReceiveMessageDelay() > 0 ? config.getReceiveMessageDelay() : DEFAULT_DELAY_MS;
		Future<?> future = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(receiveMessage, 0, delay,
				TimeUnit.MICROSECONDS);

		return future;
	}

	private class ReceiveMessageTask implements Runnable {

		private final RemoteModuleImpl moduleProxy;

		public ReceiveMessageTask(RemoteModuleImpl moduleProxy) {
			this.moduleProxy = moduleProxy;
		}

		@Override
		public void run() {
			try {
				for (CommunicationEndpoint endpoint : moduleProxy.getEndpoints()) {
					RemoteMessage message = endpoint.receiveMessage();
					if (message != null) {
						if (endpoint instanceof CommunicationControlEndpoint) {
							executeControlMessage(message, (CommunicationControlEndpoint) endpoint);
						} else {
							executeDataMessage((DataMessage) message);
						}
					}
				}
			} catch (ConnectionException e) {
				e.printStackTrace();
			} catch (ModuleInstantionException e) {
				e.printStackTrace();
			} catch (MessageDeserializationException e) {
				e.printStackTrace();
			}
		}

		private void executeDataMessage(DataMessage dataMessage) {
			moduleProxy.getGatewayModule().receive(dataMessage.getContent());
		}

		private void executeControlMessage(RemoteMessage message, CommunicationControlEndpoint endpoint)
				throws ModuleInstantionException, ConnectionException {
			if (message instanceof CreateMessage) {
				this.processCreateMessage(message, endpoint);
				return;
			}

			if (message instanceof StartMessage) {
				this.processStartMessage();
				return;
			}

			if (message instanceof DestroyMessage) {
				this.processDestroMessage();
				return;
			}
		}

		private void processDestroMessage() {
			if (moduleProxy.getGatewayModule() == null)
				throw new IllegalStateException("Module has to be initialized before calling destroy.");

			try {
				moduleProxy.getFuture().cancel(true);
				moduleProxy.disconnectEndpoints();
			} catch (ConnectionException e) {
				e.printStackTrace();
			}
			moduleProxy.getGatewayModule().destroy();
		}

		private void processStartMessage() {
			if (moduleProxy.getGatewayModule() == null)
				throw new IllegalStateException("Module has to be initialized before calling start.");

			moduleProxy.getGatewayModule().start();
		}

		private void processCreateMessage(RemoteMessage message, CommunicationControlEndpoint endpoint)
				throws ModuleInstantionException, ConnectionException {
			CreateMessage controlMessage = (CreateMessage) message;
			IGatewayModule module = this.createModuleInstanceWithArgsConstructor(controlMessage);

			if (module == null) {
				module = this.createModuleInstanceNoArgsConstructor(controlMessage);
			}

			this.createDataEndpoints(controlMessage.getDataEndpoints());
			endpoint.sendCreateReply();
		}

		private IGatewayModule createModuleInstanceNoArgsConstructor(CreateMessage controlMessage)
				throws ModuleInstantionException {
			IGatewayModule module = null;
			try {
				final int emptyAddress = 0;
				module = this.moduleProxy.getConfig().getModuleClass().newInstance();
				module.create(emptyAddress, new BrokerProxy(), controlMessage.getArgs());
			} catch (InstantiationException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (IllegalAccessException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			}

			return module;
		}

		private IGatewayModule createModuleInstanceWithArgsConstructor(CreateMessage controlMessage)
				throws ModuleInstantionException {
			IGatewayModule module = null;
			Class<? extends IGatewayModule> clazz = moduleProxy.getConfig().getModuleClass();
			final int emptyAddress = 0;
			try {
				Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(Long.class, Broker.class,
						String.class);
				module = ctor.newInstance(emptyAddress, new BrokerProxy(), controlMessage.getArgs());
			} catch (NoSuchMethodException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (SecurityException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (InstantiationException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (IllegalAccessException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (IllegalArgumentException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (InvocationTargetException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			}

			return module;
		}

		private void createDataEndpoints(List<DataEndpointConfig> dataEndpoints) throws ConnectionException {
			for (DataEndpointConfig endpointConfig : dataEndpoints) {
				CommunicationEndpoint endpoint = new CommunicationDataEndpoint(endpointConfig.getId(),
						endpointConfig.getType());
				endpoint.connect();
				moduleProxy.addEndpoint(endpoint);
			}
		}
	}

}
