package com.microsoft.azure.gateway.remote;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

public class RemoteModuleProxy {

	private static final int DEFAULT_DELAY_MS = 200;
	private final ModuleConfiguration config;
	private boolean isAttached;
	private List<CommunicationEndpoint> endpoints;
	private IGatewayModule module;
	private ScheduledFuture<?> future;

	public RemoteModuleProxy(ModuleConfiguration configuration) {
		if (configuration == null)
			throw new IllegalArgumentException("Configuration can not be null.");

		this.config = configuration;
		this.endpoints = new ArrayList<CommunicationEndpoint>();
	}

	public void attach() throws ConnectionException {
		if (isAttached)
			return;

		CommunicationEndpoint controlEndpoint = new CommunicationControlEndpoint(this.config.getIdentifier());
		controlEndpoint.connect();
		endpoints.add(controlEndpoint);
		isAttached = true;
		this.startListening();
	}

	public void detach() throws ConnectionException {
		if (!isAttached)
			return;

		future.cancel(true);
		for (CommunicationEndpoint endpoint : endpoints) {
			endpoint.disconnect();
		}

		isAttached = false;
		future = null;
	}

	private void startListening() {
		if (!isAttached)
			throw new IllegalStateException("Please call attach before starting listening.");
		if (future != null)
			throw new IllegalStateException("Thread already listening");

		Runnable receiveMessage = new ProxyGateway();
		int delay = config.getReceiveMessageDelay() > 0 ? config.getReceiveMessageDelay() : DEFAULT_DELAY_MS;
		future = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(receiveMessage, 0, delay,
				TimeUnit.MICROSECONDS);
	}

	private class ProxyGateway implements Runnable {

		@Override
		public void run() {
			try {

				for (CommunicationEndpoint endpoint : endpoints) {
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ModuleInstantionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MessageDeserializationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void executeDataMessage(DataMessage dataMessage) {
			module.receive(dataMessage.getContent());
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
			if (module == null)
				throw new IllegalStateException("Module has to be initialized before calling destroy.");

			try {
				detach();
			} catch (ConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			module.destroy();
		}

		private void processStartMessage() {
			if (module == null)
				throw new IllegalStateException("Module has to be initialized before calling start.");

			module.start();
		}

		private void processCreateMessage(RemoteMessage message, CommunicationControlEndpoint endpoint)
				throws ModuleInstantionException, ConnectionException {
			CreateMessage controlMessage = (CreateMessage) message;
			this.createModuleInstanceWithArgsConstructor(controlMessage);

			if (module == null) {
				this.createModuleInstanceNoArgsConstructor(controlMessage);
			}

			this.createDataEndpoints(controlMessage.getDataEndpoints());
			endpoint.sendCreateReply();
		}

		private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage)
				throws ModuleInstantionException {
			try {
				final int emptyAddress = 0;
				module = config.getModuleClass().newInstance();
				module.create(emptyAddress, new BrokerProxy(), controlMessage.getArgs());
			} catch (InstantiationException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			} catch (IllegalAccessException e) {
				throw new ModuleInstantionException("Could not intantiate Gateway module", e);
			}
		}

		private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage)
				throws ModuleInstantionException {
			Class<? extends IGatewayModule> clazz = config.getModuleClass();
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
		}

		private void createDataEndpoints(List<DataEndpointConfig> dataEndpoints) throws ConnectionException {
			for (DataEndpointConfig endpointConfig : dataEndpoints) {
				CommunicationEndpoint endpoint = new CommunicationDataEndpoint(endpointConfig.getId(),
						endpointConfig.getType());
				endpoint.connect();
				endpoints.add(endpoint);
			}
		}
	}

}
