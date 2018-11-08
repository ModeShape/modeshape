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
package org.modeshape.jcr;

import java.io.InputStream;

import org.modeshape.jcr.value.BinaryValue;

/**
 * This {@link org.modeshape.jcr.api.ValueFactory} implementation will use the hint supplied in the constructor for
 * creating binaries if no other hint is specified. The hint will *NOT* be used for calls to
 * {@link #createBinary(InputStream, String)}, only for calls to {@link #createBinary(InputStream)}.
 *
 * @author Wessel Nieboer
 */
public class JcrValueFactoryWithHint extends JcrValueFactory {

    private final String binaryStoreHint;

    protected JcrValueFactoryWithHint( ExecutionContext context, String binaryStoreHint ) {
        super(context);
        this.binaryStoreHint = binaryStoreHint;
    }

    @Override
    public BinaryValue createBinary( InputStream value ) {
        return super.createBinary(value, binaryStoreHint);
    }
}
