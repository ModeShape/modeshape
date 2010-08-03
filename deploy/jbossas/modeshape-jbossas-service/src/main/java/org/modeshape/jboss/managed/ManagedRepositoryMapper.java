/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jboss.managed;

import java.lang.reflect.Type;

import org.jboss.metatype.api.types.CompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.values.CompositeValue;
import org.jboss.metatype.api.values.CompositeValueSupport;
import org.jboss.metatype.api.values.MetaValue;
import org.jboss.metatype.api.values.MetaValueFactory;
import org.jboss.metatype.api.values.SimpleValueSupport;
import org.jboss.metatype.plugins.types.MutableCompositeMetaType;
import org.jboss.metatype.spi.values.MetaMapper;


public class ManagedRepositoryMapper extends MetaMapper<ManagedRepository> {
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String VERSION = "version"; //$NON-NLS-1$
	private static final MutableCompositeMetaType metaType;
	private static final MetaValueFactory metaValueFactory = MetaValueFactory.getInstance();
	
	static {
		metaType = new MutableCompositeMetaType(ManagedRepository.class.getName(), "The ModeShape repository instance"); //$NON-NLS-1$
		metaType.addItem(NAME, NAME, SimpleMetaType.STRING);
		metaType.addItem(VERSION, VERSION, SimpleMetaType.STRING);
		metaType.freeze();
	}
	
	@Override
	public Type mapToType() {
		return ManagedRepository.class;
	}
	
	@Override
	public MetaType getMetaType() {
		return metaType;
	}
	
	@Override
	public MetaValue createMetaValue(MetaType metaType, ManagedRepository object) {
		if (object == null)
			return null;
		if (metaType instanceof CompositeMetaType) {
			CompositeMetaType composite = (CompositeMetaType) metaType;
			CompositeValueSupport managedRepository = new CompositeValueSupport(composite);
			
			managedRepository.set(NAME, SimpleValueSupport.wrap(object.getName()));
			managedRepository.set(VERSION, SimpleValueSupport.wrap(object.getVersion()));
			return managedRepository;
		}
		throw new IllegalArgumentException("Cannot convert ManagedRepository " + object); //$NON-NLS-1$
	}

	@Override
	public ManagedRepository unwrapMetaValue(MetaValue metaValue) {
		if (metaValue == null)
			return null;

		if (metaValue instanceof CompositeValue) {
			CompositeValue compositeValue = (CompositeValue) metaValue;
			
			ManagedRepository managedRepository = new ManagedRepository();
			managedRepository.setName((String) metaValueFactory.unwrap(compositeValue.get(NAME)));
			managedRepository.setVersion((String) metaValueFactory.unwrap(compositeValue.get(VERSION)));
			return managedRepository;
		}
		throw new IllegalStateException("Unable to unwrap ManagedRepository " + metaValue); //$NON-NLS-1$
	}

}
