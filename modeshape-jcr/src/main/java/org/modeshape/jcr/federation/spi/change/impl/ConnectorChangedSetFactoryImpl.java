package org.modeshape.jcr.federation.spi.change.impl;

import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.change.ConnectorChangedSet;
import org.modeshape.jcr.federation.spi.change.ConnectorChangedSetFactory;
import org.modeshape.jcr.value.ValueFactories;

public class ConnectorChangedSetFactoryImpl implements
		ConnectorChangedSetFactory {

	private ValueFactories factories;
	private String processKey;
	private String repositoryKey;
	private ChangeBus bus;

	public ConnectorChangedSetFactoryImpl(final String processKey,
			final String repositoryKey, final Connector connector,
			final ChangeBus bus) {
		this.bus = bus;
		this.factories = connector.getContext().getValueFactories();
		this.processKey = processKey;
		this.repositoryKey = repositoryKey;
	}

	@Override
	public ConnectorChangedSet newChangeSet() {
		return new ConnectorChangedSetImpl(processKey, repositoryKey,
				factories, bus);
	}
}
