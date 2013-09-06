/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderClickEvent;
import com.smartgwt.client.widgets.tree.events.FolderClickHandler;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author kulikov
 */
public class Navigator extends Label {

    private Tree jcrTree = new Tree();
    private TreeGrid jcrTreeGrid = new TreeGrid();
    private HLayout layout = new HLayout();

    private static final TreeNode ROOT = new TreeNode();
    
    private Console console;
    
    static {
        ROOT.setTitle("root");
        ROOT.setAttribute("path", "/");
    }
    
    public Navigator(Console console) {
        super();
        this.console = console;
        
        setAlign(Alignment.CENTER);
        setOverflow(Overflow.HIDDEN);
        setWidth("20%");
        setShowResizeBar(true);

        //create tree tabset
        Tab treeTab = new Tab();
        
        JcrTreeNode root = new JcrTreeNode("", "", new JcrTreeNode("root", "/"));
        
        jcrTree.setModelType(TreeModelType.PARENT);
        jcrTree.setNameProperty("name");
        jcrTree.setRoot(root);
        
        jcrTreeGrid.setData(jcrTree);
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
//        jcrTreeGrid.setContextMenu(createRightClickMenu());
//        jcrTreeGrid.addCellMouseDownHandler(new JcrTreeCellMouseDownHandler());
//        jcrTreeGrid.addDropHandler(new JcrTreeDropHandler(Explorer.this));

        layout.setCanDragResize(true);
        layout.setMembersMargin(10);
        layout.addChild(jcrTreeGrid);

        treeTab.setPane(jcrTreeGrid);
        treeTab.setTitle("Repository");

        TabSet tabset = new TabSet();

        tabset.setTabs(treeTab);
        tabset.setWidth100();
        tabset.setHeight100();

        addChild(tabset);
        jcrTreeGrid.draw();
    }

//    public JcrTreeNode find(String path) {
//        return (JcrTreeNode) jcrTree.find(path);
//    }

    public void openFolder(TreeNode node) {
        jcrTree.openFolder(node);
    }

//    public TreeNode[] getChildren(JcrTreeNode node) {
//        return jcrTree.getChildren(node);
//    }

    public void remove(TreeNode node) {
        jcrTree.remove(node);
    }

    public void deselectAllRecords() {
        this.jcrTreeGrid.deselectAllRecords();;
    }

    public boolean hasChildren(TreeNode node) {
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
        JcrTreeNode node = (JcrTreeNode) jcrTreeGrid.getSelectedRecord();
        if (node == null) {
            node = (JcrTreeNode) ROOT;
        }
        console.jcrService.childNodes(node.getAttribute("path"), new ChildrenHandler());
        console.nodePanel.display(node);
    }
    
    private class NodeLoader implements CellClickHandler {
        @Override
        public void onCellClick(CellClickEvent event) {
            selectNode();
        }        
    }
    
    private class FolderClickHandlerImpl implements FolderClickHandler {

        @Override
        public void onFolderClick(FolderClickEvent event) {
            System.out.println("--------------- Folder has been clicked!-------");
        }
        
    }
    private class ChildrenHandler implements AsyncCallback<List<JcrNode>> {

        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(List<JcrNode> result) {
            //pick up selected record
            JcrTreeNode parent = (JcrTreeNode)jcrTreeGrid.getSelectedRecord();
            
            //clean up selected node
            TreeNode[] children = jcrTree.getChildren(parent);
            jcrTree.removeList(children);
            
            //reload child nodes
            for (JcrNode n : result) {
//                Collection<NodeObject> list = n.children();
                
//                ItemNode node = new ItemNode(n.getName(), n.getPath());
//                node.setName(n.getName());
//                node.setAttribute("path", n.getPath());
                JcrTreeNode node = new JcrTreeNode(n.getName(), n.getPath());
                node.setChildren(new JcrTreeNode[]{new JcrTreeNode("--", "--")});
                jcrTree.add(convert(n), parent);
                
            }
            
        }
        
        private JcrTreeNode convert(JcrNode node) {
            JcrTreeNode item = new JcrTreeNode(node.getName(), node.getPath(), node.getPrimaryType());
            item.setProperties(node.getProperties());
            
            Collection<JcrNode> children = node.children();
            JcrTreeNode[] childs = new JcrTreeNode[children.size()];
            int i = 0;
            for (JcrNode child : children){
                childs[i++] = convert(child);
            }
            item.setChildren(childs);
            return item;
        }
    }
}
