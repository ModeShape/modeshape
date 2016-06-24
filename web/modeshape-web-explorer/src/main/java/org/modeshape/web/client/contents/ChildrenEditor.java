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
package org.modeshape.web.client.contents;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import java.util.Collection;
import org.modeshape.web.client.contents.ChildrenEditor.NodeRecord;
import org.modeshape.web.client.grid.Grid;
import org.modeshape.web.client.grid.Pager;
import org.modeshape.web.shared.Align;
import org.modeshape.web.shared.Columns;
import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 */
public class ChildrenEditor extends Grid<NodeRecord, JcrNode> {

    private JcrNode node;    
    private final Contents contents;
    private final RenameNodeDialog renameNodeDialog;
    private final ChildNodesPager pager = new ChildNodesPager();
    
     
    private Label hint;
    
    public ChildrenEditor(Contents contents) {
        super("Child nodes");
        this.contents = contents;
        this.setFooterContent(pager);
        renameNodeDialog = new RenameNodeDialog(contents);
    }

    public void show(JcrNode node) {
        this.node = node;
        
        pager.setRecordsAmount((int)node.getChildCount());
        pager.fetch(0);
        
        String hintText = node.hasBinaryContent() ? 
                "This node has binary content attached. See bellow" :
                "This node has no binary content attached";
        hint.setContents(hintText);
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

    @SuppressWarnings("synthetic-access")
    @Override
    protected HLayout toolBar() {        
        hint = new Label();
        hint.setWidth100();
        
        Label hint2 = new Label();
        hint2.setContents("Enable/Disbale properties");
        hint2.setWidth(250);
        hint2.setAlign(Alignment.RIGHT);
        
        Label detailsButton = new Label();
        detailsButton.setTooltip("Show details");
        detailsButton.setStyleName("button-label");
        detailsButton.setWidth(16);
        detailsButton.setIcon("icons/view_table.png");
        detailsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.toggleDetails();
            }
        });
        
        Columns col = new Columns(Align.LEFT, Align.CENTER);        
        col.setBackgroundColor("#ffffff");
        
        col.setHeight(30);        
        col.addMember(hint);
        col.addMember(hint2);
        col.addMember(detailsButton);
        
        return col;
    }

    @Override
    protected NodeRecord[] records() {
        NodeRecord[] recs = new NodeRecord[100];
        for (int i = 0; i < recs.length; i++) {
            recs[i] = new NodeRecord();
        }
        return recs;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    protected void updateRecord(int pos, NodeRecord record, JcrNode value) {
        if (pos <= 0) {            
            record.setNodeAsParent(ChildrenEditor.this.node);
            record.setVisible(!node.getPath().equals("/"));
        } else {
            record.setNode(value);
        }        
    }

    public class NodeRecord extends HLayout {

        private Label name = new Label();
        private Label path = new Label();
        private Label primaryType = new Label();
        private Buttons buttons = new Buttons();
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
                @SuppressWarnings("synthetic-access")
                @Override
                public void onClick(ClickEvent event) {
                    contents.getAndDisplayNode(path(), true);
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
            addMember(buttons);
        }

        private String path() {
            return node == null ? "/" : path.getContents();
        }

        private void setNode(JcrNode node) {
            this.node = node;
            this.name.setContents(node.getName());
            this.path.setContents(node.getPath());
            this.path.setVisible(true);
            this.buttons.setVisible(true);
            this.buttons.setNode(node);
            this.primaryType.setContents(node.getPrimaryType());
        }

        private void setNodeAsParent(JcrNode node) {
            this.node = node;
            this.name.setContents("<b>../</b>");
            this.path.setContents(parent(node.getPath()));
            this.path.setVisible(false);
            this.buttons.setVisible(false);
            this.buttons.setNode(node);
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

    private class Buttons extends HLayout {

        private Label editButton = new Label();
        private Label remButton = new Label();
        private JcrNode node;
        
        public Buttons() {
            this.setWidth(36);

            this.setDefaultLayoutAlign(Alignment.RIGHT);
            this.setLayoutAlign(Alignment.RIGHT);
            this.setAlign(Alignment.RIGHT);
            this.setDefaultLayoutAlign(VerticalAlignment.CENTER);
            this.setLayoutAlign(VerticalAlignment.CENTER);
            this.setAlign(VerticalAlignment.CENTER);

            editButton.setWidth(16);
            editButton.setHeight(16);
            editButton.setIcon("icons/pencil.png");
            editButton.setStyleName("button-label");
            editButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    renameNodeDialog.showModal(node);
                }
            });

            remButton.setWidth(16);
            remButton.setHeight(16);
            remButton.setIcon("icons/cross.png");
            remButton.setStyleName("button-label");
            remButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    SC.ask("Remove node", "Do you want to remove node?", new BooleanCallback() {
                        @Override
                        public void execute(Boolean confirmed) {
                            if (confirmed) {
                                contents.removeNode(node);
                            }
                        }
                    });
                }
            });

            addMember(editButton);
            addMember(remButton);
        }
        
        protected void setNode(JcrNode node) {
            this.node = node;
        }
    }
    
    private class ChildNodesPager extends Pager {

        @Override
        public void fetch(int index) {
            contents.jcrService().childNodes(contents.repository(), 
                    contents.workspace(), contents.path(), 
                    index  * pager.getRecordsPerPage(), 
                    pager.getRecordsPerPage(), new AsyncCallback<Collection<JcrNode>>() {
                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(Collection<JcrNode> nodes) {
                    setValues(nodes);
                }
            });
        }
        
    }
}
