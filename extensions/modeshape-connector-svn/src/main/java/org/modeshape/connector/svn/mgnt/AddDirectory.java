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

import org.modeshape.connector.scm.ScmAction;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;

/**
 * root should be the last, previously created, parent folder. Each directory in the path will be created.
 * @author serge pagop
 */
public class AddDirectory implements ScmAction {
    private String rootDirPath;
    private String childDirPath;

    public AddDirectory( String rootPath,
                         String childDirPath ) {
        this.rootDirPath = rootPath;
        this.childDirPath = childDirPath;
    }

    public void applyAction( Object context ) throws SVNException {

        ISVNEditor editor = (ISVNEditor)context;

        ISVNEditorUtil.openDirectories(editor, this.rootDirPath);
        String[] paths = this.childDirPath.split("/");
        String newPath = this.rootDirPath;
        for (int i = 0, length = paths.length; i < length; i++) {
            newPath = (newPath.length() != 0) ? newPath + "/" + paths[i] : paths[i];

            editor.addDir(newPath, null, -1);
        }

        ISVNEditorUtil.closeDirectories(editor, childDirPath);
        ISVNEditorUtil.closeDirectories(editor, this.rootDirPath);
    }

    @Override
    public String toString() {
        return "AddDirectory {" + rootDirPath + "/" + childDirPath + "}";
    }
}
