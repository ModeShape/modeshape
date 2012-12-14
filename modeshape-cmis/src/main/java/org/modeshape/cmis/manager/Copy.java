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
package org.modeshape.cmis.manager;

import java.io.*;

/**
 *
 * @author kulikov
 */
public class Copy {
    public void copy(String fileName, File dest, String... params) throws IOException {
        System.out.println("Copying " + fileName + " to " + dest);
        String content;
        InputStream in = getClass().getResourceAsStream(fileName);

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int b = 0;
            while (b != -1) {
                b = in.read();
                if (b != -1) {
                    bout.write(b);
                }
            }
            content = new String(bout.toByteArray());
        } finally {
            if (in != null) {
                in.close();
            }
        }

        for (int i = 0; i < params.length; i++) {
            int pos = params[i].indexOf(":");
            if (pos == -1) {
                throw new IllegalArgumentException("Wrong format of parameter: " + params[i]);
            }

            String name = params[i].substring(0, pos).trim();
            String value = params[i].substring(pos + 1).trim();

            while (content.indexOf("${") != -1) {
                content = content.replace("${" + name +"}", value);
            }
        }

        FileOutputStream fout = new FileOutputStream(dest.getAbsoluteFile() + "/" + fileName);
        try {
            fout.write(content.getBytes());
            fout.flush();
        } finally {
            fout.close();
        }
    }

    public void copyBinary(String fileName, File dest) throws IOException {
        System.out.println("Copying " + fileName + " to " + dest);
        InputStream in = getClass().getResourceAsStream(fileName);
        FileOutputStream fout = new FileOutputStream(dest.getAbsoluteFile() + "/" + fileName);

        try {
            int b = 0;
            while (b != -1) {
                b = in.read();
                if (b != -1) {
                    fout.write(b);
                }
            }

            fout.flush();
        } finally {
            if (in != null) {
                in.close();
            }

            if (fout != null) {
                fout.close();
            }
        }

    }
}
