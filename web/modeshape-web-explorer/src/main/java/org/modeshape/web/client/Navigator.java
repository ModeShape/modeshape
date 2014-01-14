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
package org.modeshape.web.client;

import java.util.Collection;
import java.util.List;
import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderClickEvent;
import com.smartgwt.client.widgets.tree.events.FolderClickHandler;

/**
 * @author kulikov
 */
public class Navigator extends Label {

    protected Tree jcrTree = new Tree();
    protected TreeGrid jcrTreeGrid = new TreeGrid();
    // private HLayout layout = new HLayout();
    protected TreeNode ROOT = new TreeNode();
    protected Console console;

    // static {
    // ROOT.setTitle("root");
    // ROOT.setAttribute("path", "/");
    // }
    public Navigator( Console console ) {
        super();
        this.console = console;

        setAlign(Alignment.CENTER);
        setOverflow(Overflow.HIDDEN);
        setWidth("20%");
        setShowResizeBar(true);

        // create tree tabset
        Tab treeTab = new Tab();

        // JcrTreeNode root = new JcrTreeNode("", "", new JcrTreeNode("root", "/"));

        // jcrTree.setModelType(TreeModelType.PARENT);
        // jcrTree.setNameProperty("name");
        // jcrTree.setRoot(root);

        // jcrTreeGrid.setData(jcrTree);
        jcrTreeGrid.setWidth100();
        jcrTreeGrid.setHeight100();
        jcrTreeGrid.setCanDragResize(true);
        jcrTreeGrid.setAnimateFolders(true);
        jcrTreeGrid.setAnimateFolderSpeed(450);
        jcrTreeGrid.setCustomIconProperty("treeGridIcon");
        jcrTreeGrid.setTreeFieldTitle("Workspace: " + "default");
        jcrTreeGrid.setCanReorderRecords(true);
        jcrTreeGrid.setCanAcceptDroppedRecords(true);
        jcrTreeGrid.setCanDragRecordsOut(true);
        jcrTreeGrid.setShowConnectors(true);
        jcrTreeGrid.setSelectionType(SelectionStyle.SINGLE);
        jcrTreeGrid.addCellClickHandler(new NodeLoader());
        jcrTreeGrid.addFolderClickHandler(new FolderClickHandlerImpl());
        // jcrTreeGrid.setContextMenu(createRightClickMenu());
        // jcrTreeGrid.addCellMouseDownHandler(new JcrTreeCellMouseDownHandler());
        // jcrTreeGrid.addDropHandler(new JcrTreeDropHandler(Explorer.this));

        VLayout vlayout = new VLayout();
        vlayout.addMember(new TreeToolbar(console));
        vlayout.addMember(jcrTreeGrid);

        treeTab.setPane(vlayout);
        treeTab.setTitle("Repository");
        treeTab.setIcon("icons/view_tree_modernist.png");

        TabSet tabset = new TabSet();

        tabset.setTabs(treeTab);
        tabset.setWidth100();
        tabset.setHeight100();

        addChild(tabset);
        jcrTreeGrid.draw();
    }

    public String getSelectedPath() {
        return ((JcrTreeNode)jcrTreeGrid.getSelectedRecord()).getPath();
    }

    /**
     * Displays node navigator with initial root node.
     */
    public void showRoot() {
        console.jcrService.getRootNode(new RootAccessorHandler());
    }

    /**
     * Opens node in the tree with given path.
     * 
     * @param path the path to the node.
     */
    public void openFolder( String path ) {
        JcrTreeNode n = (JcrTreeNode)jcrTree.find("path", path);
        SC.say("Node is null " + (n == null));
        jcrTree.openFolder(n);
    }

    public void openFolder( TreeNode node ) {
        jcrTree.openFolder(node);
    }

    // public TreeNode[] getChildren(JcrTreeNode node) {
    // return jcrTree.getChildren(node);
    // }
    public void remove( TreeNode node ) {
        jcrTree.remove(node);
    }

    public void deselectAllRecords() {
        this.jcrTreeGrid.deselectAllRecords();
    }

    public boolean hasChildren( TreeNode node ) {
        return jcrTree.hasChildren(node);
    }

    public void refresh() {
        jcrTreeGrid.setData(jcrTree);
    }

    /*    public void addList(JcrTreeNode[] list, String path) {
     jcrTree.addList(list, path);
     }

     public void addList(JcrTreeNode[] list, TreeNode parent) {
     jcrTree.addList(list, parent);
     }

     public JcrTreeNode getSelectedRecord() {
     return (JcrTreeNode) jcrTreeGrid.getSelectedRecord();
     }
     */
    public void selectNode() {
        JcrTreeNode node = (JcrTreeNode)jcrTreeGrid.getSelectedRecord();
        if (node == null) {
            node = (JcrTreeNode)ROOT;
        }
        String path = node.getAttribute("path");
        if (path != null && path.trim().length() != 0) {
            console.jcrService.childNodes(path, new ChildrenHandler());
        }
        console.nodePanel.display(node);
    }

    /**
     * Gets access to the currently selected node.
     * 
     * @return selected node.
     */
    public JcrTreeNode getSelectedNode() {
        JcrTreeNode node = (JcrTreeNode)jcrTreeGrid.getSelectedRecord();
        if (node == null) {
            node = (JcrTreeNode)ROOT;
        }
        return node;
    }

    protected class NodeLoader implements CellClickHandler {

        @Override
        public void onCellClick( CellClickEvent event ) {
            selectNode();
        }
    }

    protected class FolderClickHandlerImpl implements FolderClickHandler {

        @Override
        public void onFolderClick( FolderClickEvent event ) {
        }
    }

    /**
     * Call back handler for the method jcrService.getRootNode().
     */
    protected class RootAccessorHandler implements AsyncCallback<JcrNode> {

        @Override
        public void onFailure( Throwable caught ) {
            throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                           // Templates.
        }

        @Override
        public void onSuccess( JcrNode node ) {
            // one more conversation of the value object into tree node object
            JcrTreeNode root = convert(node);
            ROOT = new JcrTreeNode("", "", root);

            jcrTree.setModelType(TreeModelType.PARENT);
            jcrTree.setNameProperty("name");
            jcrTree.setRoot(ROOT);

            jcrTreeGrid.setData(jcrTree);
            selectNode();
        }
    }

    protected class ChildrenHandler implements AsyncCallback<List<JcrNode>> {

        @Override
        public void onFailure( Throwable caught ) {
        }

        @Override
        public void onSuccess( List<JcrNode> result ) {
            // pick up selected record
            JcrTreeNode parent = (JcrTreeNode)jcrTreeGrid.getSelectedRecord();

            // clean up selected node
            TreeNode[] children = jcrTree.getChildren(parent);
            jcrTree.removeList(children);

            // reload child nodes
            for (JcrNode n : result) {
                jcrTree.add(convert(n), parent);
            }
        }
    }

    /**
     * Converts jcr node value object to tree node object.
     * 
     * @param node jcr node value object
     * @return tree node object
     */
    protected JcrTreeNode convert( JcrNode node ) {
        JcrTreeNode item = new JcrTreeNode(node.getName(), node.getPath(), node.getPrimaryType());
        item.setProperties(node.getProperties());

        Collection<JcrNode> children = node.children();
        JcrTreeNode[] childs = new JcrTreeNode[children.size()];
        int i = 0;
        for (JcrNode child : children) {
            childs[i++] = convert(child);
        }
        item.setChildren(childs);
        item.setAcessControlList(node.getAccessList());
        item.setMixins(node.getMixins());
        item.setPropertyDefs(node.getPropertyDefs());
        return item;
    }
}
