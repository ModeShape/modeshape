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
package org.modeshape.connector.svn.mgnt;

import java.io.ByteArrayInputStream;
import org.modeshape.connector.scm.ScmAction;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;

public class UpdateFile implements ScmAction {
    private String path;
    private String file;
    private byte[] oldData;
    private byte[] newData;

    public UpdateFile( String path,
                      String file,
                      byte[] oldData,
                      byte[] newData ) {
        this.path = path;
        this.file = file;
        this.oldData = oldData;
        this.newData = newData;
    }

    public void applyAction( Object context ) throws Exception {
        ISVNEditor editor = (ISVNEditor)context;
        ISVNEditorUtil.openDirectories(editor, this.path);
        
        editor.openFile(this.path + "/" + this.file, -1);        
        editor.applyTextDelta(this.path + "/" + this.file, null);
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
        String checksum = deltaGenerator.sendDelta(this.path + "/" + this.file,
                                                   new ByteArrayInputStream(this.oldData),
                                                   0,
                                                   new ByteArrayInputStream(this.newData),
                                                   editor,
                                                   true);
        editor.closeFile(this.path + "/" + this.file, checksum);
        ISVNEditorUtil.closeDirectories(editor, path);
    }

    @Override
    public String toString() {
        return "UpdateFile {" + path + "/" + file + "}";
    }

}
