/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jdbc.types;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.modeshape.jdbc.Transform;

/**
 *
 */
public class BooleanTransform  implements Transform {


	/**
	 * {@inheritDoc}
	 * @throws RepositoryException 
	 * @throws ValueFormatException 
	 *
	 * @see org.modeshape.jdbc.Transform#transform(javax.jcr.Value)
	 */
	@Override
	public Object transform(Value value) throws ValueFormatException, RepositoryException {
		return new Boolean(value.getBoolean());
	}

}
