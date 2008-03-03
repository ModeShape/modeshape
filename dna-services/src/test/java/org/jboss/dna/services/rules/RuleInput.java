/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.services.rules;

public final class RuleInput {

    String mimeType = "";
    String header = "";
    String fileName = "";

    /**
     * @return fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return header
     */
    public String getHeader() {
        return header;
    }

    /**
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param fileName Sets fileName to the specified value.
     */
    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }

    /**
     * @param header Sets header to the specified value.
     */
    public void setHeader( String header ) {
        this.header = header;
    }

    /**
     * @param mimeType Sets mimeType to the specified value.
     */
    public void setMimeType( String mimeType ) {
        this.mimeType = mimeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.fileName + " (" + this.mimeType + ") => " + this.header;
    }
}
