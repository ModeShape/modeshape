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
package org.modeshape.jcr.value.binary;

import java.util.Map;
import java.util.Properties;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 *
 * @author kulikov
 */
public abstract class CustomBinaryStore extends AbstractBinaryStore {

    /**
     * Instantiates custom binary store
     *
     * @param conf configuration parameters of the store
     * @return Binary store instance.
     * @throws Exception if problem with underlying resource occurs
     */
    public static CustomBinaryStore newInstance(Map conf) throws Exception {
        String className = (String) conf.get(RepositoryConfiguration.FieldName.CLASSNAME);
        if (className == null) {
            throw new BinaryStoreException(JcrI18n.missingVariableValue.text("classname"));
        }
        
        Class cls = classLoader(RepositoryConfiguration.FieldName.CLASSLOADER).loadClass(className);
        CustomBinaryStore store = (CustomBinaryStore) cls.newInstance();

        Properties props = new Properties();
        props.putAll(conf);

        store.configure(props);
        return store;
    }

    /**
     * Prepares class loaders
     * @param name
     * @return
     * @throws Exception
     */
    private static ClassLoader classLoader(String name) throws Exception {
        return StringUtil.isBlank(name) ? CustomBinaryStore.class.getClassLoader() :
                CustomBinaryStore.class.getClassLoader().loadClass(name).getClassLoader();
    }

    /**
     * Default constructor
     */
    public CustomBinaryStore() {
    }

    /**
     * Configures binary store.
     *
     * @param conf configuration parameters
     */
    public abstract void configure(Properties conf);

}
