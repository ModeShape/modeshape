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
package org.modeshape.web.client.grid;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import org.modeshape.web.client.Contents;
import org.modeshape.web.client.grid.Children.NodeRecord;
import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 */
public class Children extends TabGrid<NodeRecord, JcrNode> {

    
    private JcrNode node;
    protected final Contents contents;
    
    public Children(Contents contents) {
        super("Child nodes");
        this.contents = contents;
    }

    public void show(JcrNode node) {
        this.node = node;
        setValues(node.children());
    }
    
    @Override
    protected HLayout tableHeader() {
        HLayout header = new HLayout();
        header.setHeight(30);
        header.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Name</b>");
        name.setWidth(150);
        name.setIcon("icons/view_tree_modernist.png");

        Label type = new Label("<b>Primary Type</b>");
        type.setWidth(150);
        type.setIcon("icons/documents.png");

        Label path = new Label("<b>Path</b>");
        path.setWidth100();
        path.setIcon("icons/view_table.png");


        header.addMember(name);
        header.addMember(type);
        header.addMember(path);
        
        return header;
    }

    @Override
    protected HLayout toolBar() {
        HLayout header = new HLayout();
//        header.setMargin(5);
        header.setAlign(Alignment.LEFT);
        header.setBackgroundColor("#ffffff");

        header.setDefaultLayoutAlign(Alignment.LEFT);
        header.setLayoutAlign(Alignment.LEFT);
        header.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        header.setLayoutAlign(VerticalAlignment.CENTER);
        header.setHeight(30);

        HLayout panel = new HLayout();

        panel.setAlign(Alignment.RIGHT);
        panel.setWidth100();
        panel.setDefaultLayoutAlign(Alignment.RIGHT);
        panel.setLayoutAlign(Alignment.RIGHT);
        panel.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        panel.setLayoutAlign(VerticalAlignment.CENTER);

        Button addButton = new Button();
        addButton.setTitle("New node");
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.addNode();
            }
        });

        Button remButton = new Button();
        remButton.setTitle("Delete node");
        remButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SC.ask("Remove node", "Do you want to remove node?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            contents.removeNode();
                        }
                    }
                });
            }
        });

        Button renameButton = new Button();
        renameButton.setTitle("Rename node");
        renameButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.renameNode();
            }
        });

        Button exportButton = new Button();
        exportButton.setTitle("Export");
        exportButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.export();
            }
        });

        Button importButton = new Button();
        importButton.setTitle("Import");
        importButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.importXML();
            }
        });
        
        HLayout strut = new HLayout();
        strut.setWidth(5);


        header.addMember(panel);

        panel.addMember(new Strut(10));
        panel.addMember(addButton);
        panel.addMember(new Strut(5));
        panel.addMember(remButton);
        panel.addMember(new Strut(5));
        panel.addMember(renameButton);
        panel.addMember(new Strut(5));
        panel.addMember(exportButton);
        panel.addMember(new Strut(5));
        panel.addMember(importButton);

        return header;
    }

    @Override
    protected NodeRecord[] records() {
        NodeRecord[] recs = new NodeRecord[100];
        for (int i = 0; i < recs.length; i++) {
            recs[i] = new NodeRecord();
        }
        return recs;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    protected void updateRecord(int pos, NodeRecord record, JcrNode value) {
        if (node.getPath().equals("/")) {
            record.setNode(value);
        } else if (pos == -1) {
            record.setNodeAsParent(node);
        } else if (pos == 0) {
            record.setNodeAsParent(node);
        } else {
            record.setNode(value);
        }
    }

    public class NodeRecord extends HLayout {

        private Label name = new Label();
        private Label path = new Label();
        private Label primaryType = new Label();
        
        private JcrNode node;

        public NodeRecord() {
            super();
            setStyleName("grid");
            setHeight(30);

            setDefaultLayoutAlign(VerticalAlignment.CENTER);
            setDefaultLayoutAlign(Alignment.LEFT);

            setLayoutAlign(VerticalAlignment.CENTER);
            setLayoutAlign(Alignment.CENTER);

            setAlign(VerticalAlignment.CENTER);
            setAlign(Alignment.LEFT);

            name.setStyleName("node-name");
            name.setIcon("icons/folder.png");
            name.addClickHandler(new ClickHandler() {
                @SuppressWarnings( "synthetic-access" )
                @Override
                public void onClick(ClickEvent event) {
                    contents.select(path(), true);
                }
            });

            name.setWidth(150);
            primaryType.setWidth(150);
            primaryType.setStyleName("text");
            
            path.setWidth100();
            path.setStyleName("text");
                    
            
            addMember(name);
            addMember(primaryType);
            addMember(path);
        }

        private String path() {
            return node == null ? "/" : path.getContents();
        }
        
        private void setNode(JcrNode node) {
            this.node = node;
            this.name.setContents(node.getName());
            this.path.setContents(node.getPath());
            this.path.setVisible(true);
            this.primaryType.setContents(node.getPrimaryType());
        }

        private void setNodeAsParent(JcrNode node) {
            this.node = node;
            this.name.setContents("<b>../</b>");
            this.path.setContents(parent(node.getPath()));
            this.path.setVisible(false);
            this.primaryType.setContents("");
        }
        
        private String parent(String path) {
            if (path == null) {
                return "/";
            }

            path = path.substring(0, path.lastIndexOf('/'));
            if (path.length() == 0) {
                return "/";
            }

            return path;
        }

    }
        
}

