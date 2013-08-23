package org.modeshape.explorer.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import org.modeshape.explorer.client.callback.CRUDServiceCallback;
import org.modeshape.explorer.client.callback.ChangeNodeTypeIconAssociationCallback;
import org.modeshape.explorer.client.callback.DefaultLoginDetailsServiceCallback;
import org.modeshape.explorer.client.callback.GetBrowsableContentFilterRegexsServiceCallback;
import org.modeshape.explorer.client.callback.GetNodeServiceCallback;
import org.modeshape.explorer.client.callback.GetNodeTreeServiceCallback;
import org.modeshape.explorer.client.callback.GetNodeTypeIconsServiceCallback;
import org.modeshape.explorer.client.callback.LoginServiceCallback;
import org.modeshape.explorer.client.callback.NewBooleanCallback;
import org.modeshape.explorer.client.callback.RemoteFileServiceCallback;
import org.modeshape.explorer.client.domain.JcrTreeNode;
import org.modeshape.explorer.client.domain.LoginDetails;
import org.modeshape.explorer.client.handler.IconGridHandler;
import org.modeshape.explorer.client.ui.AddNewNode;
import org.modeshape.explorer.client.ui.Details;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.ImageStyle;
import com.smartgwt.client.types.LayoutPolicy;
import com.smartgwt.client.types.LayoutResizeBarPolicy;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.TreeModelType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.DropHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.grid.events.CellMouseDownHandler;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.layout.VStack;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 *
 * @author James Pickup
 *
 */
public class Explorer implements EntryPoint {

    private static VLayout mainForm = new VLayout();
    
    private Navigator navigator;
    
    private DetailsPanel nodeDetails;
    private RepositoryTabPanel repositoryDetails;
    private QueryTabPanel query;
    
    private ListGridRecord[] propertiesListGridRecords = new ListGridRecord[]{};
    
    public Explorer() {
        super();
    }
    
    public Navigator navigator() {
        return navigator;
    }
    
    public final static String NT_UNSTRUCTURED = "nt:unstructured";
    public static String browsableContentFilterRegex = "";
    public static final String defaultIcon = "icons/folder_open.png";
    public JcrTreeNode jcrRoot = new JcrTreeNode("", "", NT_UNSTRUCTURED, new HashMap<String, String>(),
            new JcrTreeNode("root", "/", NT_UNSTRUCTURED, new HashMap<String, String>(),
            defaultIcon));
    
    public static JcrServiceAsync service;
    public TabSet bottomRightTabSet = new TabSet();
    public ListGrid searchResultsListGrid = new ListGrid();
    private ListGrid remoteIconFilesListGrid = new ListGrid();
    private Window possibleIconsWindow = createPossibleIconsWindow(remoteIconFilesListGrid);
    
    private String sourcePath = "";
    private String destinationPath = "";
    private String copyCellPath = null;
    private String cutCellPath = null;
    private String changeIconCellType = null;
    private String deleteCellPath = null;
    
    public static TreeGrid cellMouseDownTreeGrid;
    public static Window loginWindow = new Window();
    public static String loadingImgPath = "loading/loading.gif";
    public static Img loadingImg = new Img(loadingImgPath);
    public static HLayout disabledHLayout = new HLayout();
    
    public LoginDetails loginDetails = null;
    protected final static String BINARY_SERVLET_PATH = "/jackrabbitexplorer/BinaryServlet?path=";
    public static List<Map<String, String>> customNodeList = null;
    
    private TabSet leftTabset = new TabSet();

    public static void setCustomTreeIcon(TreeNode treeNode, String primaryNodeType) {
        if (null != customNodeList) {//rep:system nt:folder
            for (Map<String, String> property : customNodeList) {
                if (property.containsKey(primaryNodeType)) {
                    treeNode.setAttribute("treeGridIcon", property.get(primaryNodeType));
                }
            }
        }

    }

    public ListGrid getRemoteIconFilesListGrid() {
        return remoteIconFilesListGrid;
    }

    public void onModuleLoad() {
        try {
            service = (JcrServiceAsync) GWT.create(JcrService.class);
            ServiceDefTarget endpoint = (ServiceDefTarget) service;
            endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "JcrService");
        } catch (Exception e) {
            SC.warn("There was an error: " + e.getMessage(), new NewBooleanCallback());
        }
        service.getDefaultLoginDetails(new DefaultLoginDetailsServiceCallback(this));
        getNodeTypeIcons();
        service.getBrowsableContentFilterRegex(new GetBrowsableContentFilterRegexsServiceCallback());
  
        mainForm.setWidth("100%");
        mainForm.setHeight("100%");
        mainForm.setBackgroundColor("#F0F0F0");
        mainForm.draw();
    }

    public void getNodeTypeIcons() {
        service.getNodeTypeIcons(new GetNodeTypeIconsServiceCallback());
    }

    public void showMainForm() {
        nodeDetails = new DetailsPanel();
        repositoryDetails = new RepositoryTabPanel();
        query = new QueryTabPanel();
        
        mainForm.setLayoutMargin(15);

        //tool bar
        HLayout topPanel = new HLayout();        
        
        topPanel.setAlign(Alignment.LEFT);
        topPanel.setOverflow(Overflow.HIDDEN);
        topPanel.setHeight("5%");
        topPanel.setBackgroundColor("#d3d3d3");
        topPanel.addMember(new PathPanel());
        
        //main area
        HLayout bottomPanel = new HLayout();
        
        
        VLayout viewPortLayout = new VLayout();
        viewPortLayout.setWidth("80%");
        
        TabSet viewPort = new TabSet();
        viewPort.setTabs(nodeDetails, repositoryDetails, query);
        
        viewPortLayout.addMember(viewPort);
        
/*        Label topRightLabel = new Label();
        topRightLabel.setAlign(Alignment.CENTER);
        topRightLabel.setOverflow(Overflow.HIDDEN);
        topRightLabel.setHeight("185");
        topRightLabel.setShowResizeBar(true);
        
        Label bottonRightLabel = new Label();
        bottonRightLabel.setAlign(Alignment.CENTER);
        bottonRightLabel.setOverflow(Overflow.HIDDEN);
        bottonRightLabel.setHeight("100%");
        
        
        topRightLabel.addChild(new Details().createDetails(this));
        
        properties = new Properties();
        
        bottomRightTabSet.setWidth100();
        bottomRightTabSet.setHeight100();
        bottomRightTabSet.setTabs(properties, createSearchResultsTab());
        bottonRightLabel.addChild(bottomRightTabSet);
        
        viewPortLayout.addMember(topRightLabel);
        viewPortLayout.addMember(bottonRightLabel);
        viewPortLayout.setAlign(VerticalAlignment.CENTER);
  */      
        navigator = new Navigator();
        
        bottomPanel.addMember(navigator);
        bottomPanel.addMember(viewPortLayout);

        HLayout sp1 = new HLayout();
        sp1.setHeight("1%");

        HLayout sp2 = new HLayout();
        sp2.setHeight("1%");
        
        HLayout statusBar = new HLayout();        
        statusBar.setHeight("5%");
        statusBar.setBorder("1px solid black");
        
        mainForm.addMember(sp1);
        mainForm.addMember(topPanel);
        mainForm.addMember(sp2);
        mainForm.addMember(bottomPanel);        
        mainForm.addMember(statusBar);
    }

    public static void showLoadingImg() {
        showDisableLayer();
        loadingImg.setSize("100px", "100px");
        loadingImg.setTop(mainForm.getHeight() / 2 - 50); //loading image height is 50px
        loadingImg.setLeft(mainForm.getWidth() / 2 - 50); //loading image width is 50px
        loadingImg.show();
        loadingImg.bringToFront();
    }

    public static void showDisableLayer() {
        disabledHLayout.setSize("100%", "100%");
        disabledHLayout.setStyleName("disabledBackgroundStyle");
        disabledHLayout.show();
    }

    public static void hideDisableLayer() {
        disabledHLayout.hide();
    }

    public static void hideLoadingImg() {
        loadingImg.hide();
        hideDisableLayer();
    }

    public void refreshFromRoot() {
        treeRecordClick(null, true, "");
    }

    public void treeRecordClick(boolean refresh, String parentPath) {
        treeRecordClick(refresh, parentPath);
    }
    
    public void treeRecordClick(TreeGrid selectedTreeGrid, boolean refresh, String parentPath) {
        showLoadingImg();
        JcrTreeNode selectedAnimateTreeNode = jcrRoot;
        if (null != selectedTreeGrid && null == parentPath) {
            selectedAnimateTreeNode = (JcrTreeNode) selectedTreeGrid.getSelectedRecord();
        }
        if (null != parentPath) {
            selectedAnimateTreeNode = (JcrTreeNode) navigator.find("/root" + parentPath);
            if (null == selectedAnimateTreeNode) {
                if (null != parentPath && !parentPath.equals("/")) {
                    service.getNodeTree(parentPath, new GetNodeTreeServiceCallback(this));
                } else {
                    service.getNode(parentPath, new GetNodeServiceCallback(this, parentPath));
                }
                Details.nameTxt.setValue("");
                Details.pathTxt.setValue("");
                return;
            }
        }
        if (selectedAnimateTreeNode.getTitle().contains("[")) {
            Details.nameTxt.setValue(selectedAnimateTreeNode.getTitle().substring(0, selectedAnimateTreeNode.getTitle().lastIndexOf("[")));
        } else {
            Details.nameTxt.setValue(selectedAnimateTreeNode.getTitle());
        }
        Details.pathTxt.setValue(selectedAnimateTreeNode.getAttribute("path"));
        if (selectedAnimateTreeNode.getAttribute("path").equals("/")) {
            Details.nameTxt.setDisabled(true);
        } else {
            Details.nameTxt.setDisabled(false);
        }
        
        navigator.openFolder(selectedAnimateTreeNode);
        
        if ((null != selectedAnimateTreeNode
                && selectedAnimateTreeNode.getAttribute("children").length() < 1) || refresh) {
            if (refresh) {
                TreeNode[] selectedTreeNodeChildren = navigator.getChildren(selectedAnimateTreeNode);
                for (int j = 0; j < selectedTreeNodeChildren.length; j++) {
                    TreeNode treeNode = selectedTreeNodeChildren[j];
                    navigator.remove(treeNode);
                }
            }
            if (null != parentPath && !parentPath.equals("/")) {
                service.getNodeTree(parentPath, new GetNodeTreeServiceCallback(this));
            } else {
                service.getNode(selectedAnimateTreeNode.getAttribute("path"), new GetNodeServiceCallback(this, parentPath));
            }
        } else {
            hideLoadingImg();
        }
        Iterator<Map.Entry<String, String>> it = selectedAnimateTreeNode.getProperties().entrySet().iterator();
        ListGridRecord listGridRecord;
        propertiesListGridRecords = new ListGridRecord[selectedAnimateTreeNode.getProperties().size()];
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<String, String> pairs = (Map.Entry<String, String>) it.next();
            listGridRecord = new ListGridRecord();
            if (pairs.getKey().contains("jcr:data")) {
                listGridRecord.setAttribute("property", "<b>" + pairs.getKey() + "</b>");
                listGridRecord.setAttribute("value", "<b>" + pairs.getValue() + "</b>");
            } else {
                listGridRecord.setAttribute("property", pairs.getKey());
                listGridRecord.setAttribute("value", pairs.getValue());
            }
            propertiesListGridRecords[i] = listGridRecord;
            i++;
        }
        nodeDetails.refreshProperties();
        Details.addNodeSubmitItem.setDisabled(false);
        Details.addPropertySubmitItem.setDisabled(false);
    }

    public void treeDeleteUpdate(String parentPath) {
        showLoadingImg();
        JcrTreeNode selectedAnimateTreeNode = jcrRoot;

        if (null != parentPath) {
            selectedAnimateTreeNode = (JcrTreeNode) navigator.find("/root" + parentPath);
        }
        Details.nameTxt.setValue("");
        Details.pathTxt.setValue(parentPath);
        if (parentPath.equals("/")) {
            Details.nameTxt.setDisabled(true);
        } else {
            Details.nameTxt.setDisabled(false);
        }
        navigator.openFolder(selectedAnimateTreeNode);
        if (null != selectedAnimateTreeNode) {
            TreeNode[] selectedTreeNodeChildren = navigator.getChildren(selectedAnimateTreeNode);
            for (int j = 0; j < selectedTreeNodeChildren.length; j++) {
                TreeNode treeNode = selectedTreeNodeChildren[j];
                navigator.remove(treeNode);
            }
            //service.getNode(selectedAnimateTreeNode.getAttribute("path"), new GetNodeServiceCallback(this, parentPath));
        }
        propertiesListGridRecords = new ListGridRecord[0];
        nodeDetails.refreshProperties();
    }

    class JcrTreeGridCellClickHandler implements CellClickHandler {

        public void onCellClick(CellClickEvent event) {
            treeRecordClick((TreeGrid) event.getSource(), false, null);
        }
    };

    class JcrTreeDropHandler implements DropHandler {

        private Explorer jackrabbitExplorer;

        public JcrTreeDropHandler(Explorer jackrabbitExplorer) {
            this.jackrabbitExplorer = jackrabbitExplorer;
        }

        public void onDrop(com.smartgwt.client.widgets.events.DropEvent event) {
            TreeGrid dropToTreeGrid = (TreeGrid) event.getSource();
            TreeGrid sourceTreeGrid = cellMouseDownTreeGrid;
            sourcePath = sourceTreeGrid.getSelectedRecord().getAttribute("path");
            destinationPath = dropToTreeGrid.getRecord(dropToTreeGrid.getEventRow()).getAttribute("path");
            SC.confirm("Proceed with moving " + sourcePath + " to "
                    + destinationPath, new BooleanCallback() {
                public void execute(Boolean value) {
                    if (value != null && !value.equals("")) {
                        showLoadingImg();
                        service.moveNode(sourcePath, destinationPath, new CRUDServiceCallback(jackrabbitExplorer, destinationPath, sourcePath));
                    } else {
                        return;
                    }
                }
            });
            navigator.deselectAllRecords();
        }
    }

    class JcrTreeCellMouseDownHandler implements CellMouseDownHandler {

        public void onCellMouseDown(com.smartgwt.client.widgets.grid.events.CellMouseDownEvent event) {
            TreeGrid dropToTreeGrid = (TreeGrid) event.getSource();
            dropToTreeGrid.getTreeFieldTitle();
            cellMouseDownTreeGrid = dropToTreeGrid;
        }
    }

    private Menu createRightClickMenu() {
        Menu rightClickMenu = new Menu();
        MenuItem newMenuItem = new MenuItem("Add New Node", "icons/icon_add_files.png", "Ctrl+N");
        class NewClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            Explorer jackrabbitExplorer = null;

            NewClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                new AddNewNode().addNewNodeBox(jackrabbitExplorer);
            }
        };
        newMenuItem.addClickHandler(new NewClickHandler(this));

        MenuItem changeIconMenuItem = new MenuItem("Change Icon for Type", "icons/pencil.png", "Ctrl+I");
        class ChangeClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            Explorer jackrabbitExplorer = null;

            ChangeClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                changeIconCellType = selectedJcrTreeGrid.getSelectedRecord().getAttribute("primaryNodeType");
                showLoadingImg();
                jackrabbitExplorer.showPossibleIcons(null);
            }
        };
        changeIconMenuItem.addClickHandler(new ChangeClickHandler(this));

        MenuItem cutMenuItem = new MenuItem("Cut", "icons/cut.png", "Ctrl+X");
        class cutClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                cutCellPath = selectedJcrTreeGrid.getSelectedRecord().getAttribute("path");
                selectedJcrTreeGrid.removeSelectedData();
            }
        };
        cutMenuItem.addClickHandler(new cutClickHandler());
        MenuItem copyMenuItem = new MenuItem("Copy", "icons/copy.png", "Ctrl+C");
        class copyClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                copyCellPath = selectedJcrTreeGrid.getSelectedRecord().getAttribute("path");
            }
        };
        copyMenuItem.addClickHandler(new copyClickHandler());
        MenuItem pasteMenuItem = new MenuItem("Paste", "icons/paste.png", "Ctrl+P");
        class PasteClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            private Explorer jackrabbitExplorer;

            public PasteClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                String copyToPath = selectedJcrTreeGrid.getSelectedRecord().getAttribute("path");
                if (null != copyCellPath) {
                    showLoadingImg();
                    service.copyNode(copyCellPath, copyToPath, new CRUDServiceCallback(jackrabbitExplorer, copyToPath, null));
                    copyCellPath = null;
                } else if (null != cutCellPath) {
                    showLoadingImg();
                    service.cutAndPasteNode(cutCellPath, copyToPath, new CRUDServiceCallback(jackrabbitExplorer, copyToPath, cutCellPath));
                    cutCellPath = null;
                } else {
                    SC.say("Nothing to paste.");
                    return;
                }
            }
        };
        pasteMenuItem.addClickHandler(new PasteClickHandler(this));
        MenuItem refreshMenuItem = new MenuItem("Refresh", "icons/refresh.png", "Ctrl+R");
        class RefreshClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                treeRecordClick(selectedJcrTreeGrid, true, null);
            }
        };
        refreshMenuItem.addClickHandler(new RefreshClickHandler());
        MenuItem deleteMenuItem = new MenuItem("Delete", "icons/icon_remove_files.png");
        class DeleteClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            private Explorer jackrabbitExplorer;

            public DeleteClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                TreeGrid selectedJcrTreeGrid = (TreeGrid) event.getTarget();
                deleteCellPath = selectedJcrTreeGrid.getSelectedRecord().getAttribute("path");
                SC.confirm("Proceed with deleting " + deleteCellPath, new BooleanCallback() {
                    public void execute(Boolean value) {
                        if (value != null && value) {
                            showLoadingImg();
                            service.deleteNode(deleteCellPath, new CRUDServiceCallback(jackrabbitExplorer, null, deleteCellPath));
                        } else {
                            return;
                        }
                    }
                });
            }
        };
        deleteMenuItem.addClickHandler(new DeleteClickHandler(this));
        rightClickMenu.setItems(newMenuItem, new MenuItemSeparator(), refreshMenuItem, new MenuItemSeparator(),
                cutMenuItem, copyMenuItem, pasteMenuItem, new MenuItemSeparator(), changeIconMenuItem, new MenuItemSeparator(), deleteMenuItem);
        return rightClickMenu;
    }

    public void navigateTo(String path) {
        showLoadingImg();
        service.getNodeTree(path, new GetNodeTreeServiceCallback(this));
    }

    private Tab createSearchResultsTab() {
        Tab searchResultsTab = new Tab();
        searchResultsTab.setTitle("Search Results");
        searchResultsListGrid.setWidth(500);
        searchResultsListGrid.setHeight(224);
        searchResultsListGrid.setAlternateRecordStyles(true);
        searchResultsListGrid.setShowAllRecords(true);
        searchResultsListGrid.setCanEdit(false);
        searchResultsListGrid.setEditByCell(false);
        searchResultsListGrid.setShowHover(true);
        ListGridField pathField = new ListGridField("path", "Path");
        pathField.setShowHover(true);
        searchResultsListGrid.setFields(pathField);
        searchResultsListGrid.setCanResizeFields(true);
        searchResultsListGrid.setWidth100();
        searchResultsListGrid.setHeight100();
        ListGridRecord listGridRecord = new ListGridRecord();
        searchResultsListGrid.setData(new ListGridRecord[]{listGridRecord});
        searchResultsListGrid.addClickHandler(new SearchResultsClickClickHandler(this));
        searchResultsTab.setPane(searchResultsListGrid);
        return searchResultsTab;
    }

    private Window createPossibleIconsWindow(ListGrid listGrid) {
        Window changeNodeTypeWindow = new Window();
        changeNodeTypeWindow.setShowMinimizeButton(false);
        changeNodeTypeWindow.setIsModal(true);
        changeNodeTypeWindow.setShowModalMask(true);
        changeNodeTypeWindow.setTitle("Change Node Type Icon");
        changeNodeTypeWindow.setCanDragReposition(true);
        changeNodeTypeWindow.setCanDragResize(false);
        changeNodeTypeWindow.setAutoCenter(true);
        changeNodeTypeWindow.setWidth(500);
        changeNodeTypeWindow.setHeight(400);
        changeNodeTypeWindow.setAlign(VerticalAlignment.BOTTOM);

        Layout verticalStack = new VStack();
        verticalStack.setHeight100();
        verticalStack.setWidth100();
        verticalStack.setPadding(10);
        verticalStack.setAlign(VerticalAlignment.BOTTOM);

        listGrid.setHeight(300);
        listGrid.setAlternateRecordStyles(true);
        listGrid.setShowAllRecords(true);
        listGrid.setCanEdit(false);
        listGrid.setEditByCell(false);
        listGrid.setShowHover(true);
        ListGridField iconField = new ListGridField("imagePath", "Icon");
        iconField.setAlign(Alignment.CENTER);
        iconField.setType(ListGridFieldType.IMAGE);
        iconField.setImageURLPrefix("");
        iconField.setWidth("40");
        ListGridField pathField = new ListGridField("path", "Path");
        pathField.setShowHover(true);
        listGrid.setFields(iconField, pathField);
        listGrid.setCanResizeFields(false);
        listGrid.setWidth100();
        listGrid.addCellClickHandler(new IconGridHandler(this));

        HStack horizontalStack = new HStack();
        //horizontalStack.setHeight(50);
        horizontalStack.setAutoHeight();
        horizontalStack.setWidth100();
        horizontalStack.setPadding(10);
        horizontalStack.setAlign(Alignment.RIGHT);

        Button cancelButton = new Button("Cancel");

        class CancelClickHandler implements com.smartgwt.client.widgets.events.ClickHandler {

            Explorer jackrabbitExplorer = null;

            CancelClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                jackrabbitExplorer.hidePossibleIconsWindow();
            }
        };

        cancelButton.addClickHandler(new CancelClickHandler(this));
        horizontalStack.addMember(cancelButton);

        verticalStack.addMember(listGrid);
        verticalStack.addMember(horizontalStack);

        changeNodeTypeWindow.addItem(verticalStack);
        return changeNodeTypeWindow;
    }

    public void showPossibleIconsWindow() {
        possibleIconsWindow.show();
    }

    public void hidePossibleIconsWindow() {
        possibleIconsWindow.hide();
    }

    class SearchResultsClickClickHandler implements com.smartgwt.client.widgets.events.ClickHandler {

        private Explorer jackrabbitExplorer;

        public SearchResultsClickClickHandler(Explorer jackrabbitExplorer) {
            this.jackrabbitExplorer = jackrabbitExplorer;
        }

        @Override
        public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
            ListGrid selectedSearchResultTreeGrid = (ListGrid) event.getSource();
            showLoadingImg();
            service.getNodeTree(selectedSearchResultTreeGrid.getSelectedRecord().getAttribute("path"), new GetNodeTreeServiceCallback(jackrabbitExplorer));
        }
    }

/*    private Tab createPropertiesTab() {
        Tab propertiesTab = new Tab();
        propertiesTab.setTitle("Node Properties");
        propertiesListGrid.setWidth(500);
        propertiesListGrid.setHeight(224);
        propertiesListGrid.setAlternateRecordStyles(true);
        propertiesListGrid.setShowAllRecords(true);
        propertiesListGrid.setCanEdit(true);
        propertiesListGrid.setEditEvent(ListGridEditEvent.CLICK);
        propertiesListGrid.setEditByCell(true);
        ListGridField propertyField = new ListGridField("property", "Property");
        propertyField.setCanEdit(false);
        propertyField.setShowHover(true);
        ListGridField valueField = new ListGridField("value", "Value");
        valueField.setShowHover(true);
        propertiesListGrid.setFields(propertyField, valueField);
        propertiesListGrid.setCanResizeFields(true);
        propertiesListGrid.setWidth100();
        propertiesListGrid.setHeight100();
        ListGridRecord listGridRecord = new ListGridRecord();
        propertiesListGrid.setData(new ListGridRecord[]{listGridRecord});
        propertiesListGrid.addCellSavedHandler(new PropertiesCellSavedHandler(this));
        propertiesListGrid.addCellClickHandler(new PropertiesCellClickHandler());
        propertiesListGrid.setContextMenu(createPropertyRightClickMenu());
        propertiesTab.setPane(propertiesListGrid);
        return propertiesTab;
    }
*/
    class PropertiesCellSavedHandler implements CellSavedHandler {

        private Explorer jackrabbitExplorer;

        public PropertiesCellSavedHandler(Explorer jackrabbitExplorer) {
            this.jackrabbitExplorer = jackrabbitExplorer;
        }

        public void onCellSaved(com.smartgwt.client.widgets.grid.events.CellSavedEvent event) {
            String selectedNodePath = cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
            String propertyName = event.getRecord().getAttribute("property");
            if (event.getColNum() == 0) {
                //update property name
                //service.savePropertyStringValue(selectedNodePath, propertyName, event.getNewValue().toString(), SimpleStringServiceCallback);
            } else if (event.getColNum() == 1) {
                //update value
                showLoadingImg();
                service.savePropertyStringValue(selectedNodePath, propertyName, event.getNewValue().toString(),
                        new CRUDServiceCallback(jackrabbitExplorer, selectedNodePath, null));
            }
        }
    };
    ListGridRecord selectedPropertyListGridRecord = null;

    class PropertiesCellClickHandler implements CellClickHandler {

        public void onCellClick(com.smartgwt.client.widgets.grid.events.CellClickEvent event) {
            selectedPropertyListGridRecord = event.getRecord();
            String propertyName = selectedPropertyListGridRecord.getAttribute("property");
            if (propertyName.contains("jcr:data")) {
                nodeDetails.setCanEditProperties(false);

                String selectedNodePath = cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
                String mimeType = "";
                for (int i = 0; i < propertiesListGridRecords.length; i++) {
                    if ("jcr:mimeType".equalsIgnoreCase(propertiesListGridRecords[i].getAttribute("property"))) {
                        mimeType = propertiesListGridRecords[i].getAttribute("value");
                    }
                }
                if (mimeType.startsWith("image")) {
                    createBinaryImgWindow(selectedNodePath, selectedNodePath + "/" + "jcr:data", mimeType);
                } else {
                    com.google.gwt.user.client.Window.open(BINARY_SERVLET_PATH + selectedNodePath + "/" + "jcr:data"
                            + "&rmiUrl=" + "" /*rmiUrlTxt.getValue().toString() */
                            + "&workSpace=" + workspaceTxt.getValue().toString() + "&mimeType=" + mimeType,
                            "_blank", "toolbar=no,menubar=no,location=no,resizable=yes,scrollbars=yes,status=no");
                }
                return;
            } else {
                nodeDetails.setCanEditProperties(true);
            }
        }
    }

    private Window createRemoteWindow(String title, String url) {
        Window remoteWindow = new Window();
        HTMLPane htmlPane = new HTMLPane();
        htmlPane.setContentsURL(url);
        htmlPane.setContentsType(ContentsType.PAGE);
        remoteWindow.addItem(htmlPane);
        remoteWindow.setTitle(title);
        remoteWindow.setShowMaximizeButton(true);
        remoteWindow.setCanDragReposition(true);
        remoteWindow.setCanDragResize(true);
        remoteWindow.setHeight("40%");
        remoteWindow.setWidth("40%");
        remoteWindow.setAutoCenter(true);
        remoteWindow.setShowResizeBar(true);
        remoteWindow.setDefaultResizeBars(LayoutResizeBarPolicy.MARKED);
        remoteWindow.show();
        return remoteWindow;
    }

    private Window createBinaryImgWindow(String title, String path, String mimeType) {
        Window binaryWindow = new Window();
        Img img = new Img(BINARY_SERVLET_PATH + path + "&rmiUrl=" /*rmiUrlTxt.getValue().toString()*/
                + "&workSpace=" + workspaceTxt.getValue().toString() + "&mimeType=" + mimeType);
        img.setImageType(ImageStyle.STRETCH);
        img.setHeight100();
        img.setWidth100();
        binaryWindow.addItem(img);
        binaryWindow.setTitle(title);
        binaryWindow.setShowMaximizeButton(true);
        binaryWindow.setCanDragReposition(true);
        binaryWindow.setCanDragResize(true);
        binaryWindow.setHeight("40%");
        binaryWindow.setWidth("40%");
        binaryWindow.setAutoCenter(true);
        binaryWindow.setShowResizeBar(true);
        //binaryWindow.setDefaultResizeBars(LayoutResizeBarPolicy.MARKED);
        binaryWindow.show();
        return binaryWindow;
    }
    String deletePropertyString;

    private Menu createPropertyRightClickMenu() {
        Menu rightClickMenu = new Menu();
        MenuItem deleteMenuItem = new MenuItem("Delete", "icons/icon_remove_files.png");
        class DeleteClickHandler implements com.smartgwt.client.widgets.menu.events.ClickHandler {

            private Explorer jackrabbitExplorer;

            public DeleteClickHandler(Explorer jackrabbitExplorer) {
                this.jackrabbitExplorer = jackrabbitExplorer;
            }

            public void onClick(com.smartgwt.client.widgets.menu.events.MenuItemClickEvent event) {
                sourcePath = cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
                ListGrid selectedPropListGrid = (ListGrid) event.getTarget();
                deletePropertyString = selectedPropListGrid.getSelectedRecord().getAttribute("property");
                SC.confirm("Proceed with deleting property " + sourcePath + "?", new BooleanCallback() {
                    public void execute(Boolean value) {
                        if (value != null && value) {
                            showLoadingImg();
                            service.deleteProperty(sourcePath, deletePropertyString,
                                    new CRUDServiceCallback(jackrabbitExplorer, null, sourcePath));
                        } else {
                            return;
                        }
                    }
                });
            }
        };
        deleteMenuItem.addClickHandler(new DeleteClickHandler(this));
        rightClickMenu.setItems(deleteMenuItem);
        return rightClickMenu;
    }
//    private RadioGroupItem connectionType = new RadioGroupItem();
//    private TextItem configFilePathTxt = new TextItem();
//    private TextItem homeDirPathTxt = new TextItem();
    private TextItem jndiNameTxt = new TextItem();
//    private TextItem jndiContextTxt = new TextItem();
//    private TextItem rmiUrlTxt = new TextItem();
    private TextItem workspaceTxt = new TextItem();
    private TextItem usernameTxt = new TextItem();
    private PasswordItem passwordTxt = new PasswordItem();

    public void showLoginBox() {
        final DynamicForm loginForm = new DynamicForm();
        loginForm.setID("loginForm");
        loginForm.setNumCols(2);
        loginForm.setPadding(25);

        LinkedHashMap<String, String> connectionMap = new LinkedHashMap<String, String>();

        String defaultValue = "3";

        // Examine logindetails here to find what to show. . . 
        if (loginDetails.isSupportsLocalRepository()) {
            connectionMap.put("1", "Local");
            defaultValue = "1";
        }
        if (loginDetails.isSupportsJndiRepository()) {
            connectionMap.put("2", "JNDI");
            defaultValue = "2";
        }
        if (loginDetails.isSupportsRmiRepository()) {
            connectionMap.put("3", "RMI");
            defaultValue = "3";
        }

/*        connectionType.setDefaultValue(defaultValue);

        connectionType.setTitle("Connection");
        connectionType.setValueMap(connectionMap);
        connectionType.setRedrawOnChange(true);
        connectionType.setRequired(true);
        connectionType.setVertical(false);

        configFilePathTxt.setName("configFilePathTxt");
        configFilePathTxt.setTitle("Config file");
        configFilePathTxt.setDefaultValue(loginDetails.getConfigFilePath());
        configFilePathTxt.setWidth(250);
        configFilePathTxt.setRequired(false);
        configFilePathTxt.setVisible(false);
        
        homeDirPathTxt.setName("homeDirPathTxt");
        homeDirPathTxt.setTitle("Home directory");
        homeDirPathTxt.setDefaultValue(loginDetails.getHomeDirPath());
        homeDirPathTxt.setWidth(250);
        homeDirPathTxt.setRequired(false);
        homeDirPathTxt.setVisible(false);
*/        
        jndiNameTxt.setName("jndiNameTxt");
        jndiNameTxt.setTitle("JNDI name");
        jndiNameTxt.setDefaultValue(loginDetails.getJndiName());
        jndiNameTxt.setWidth(250);
        jndiNameTxt.setRequired(false);
        jndiNameTxt.setVisible(false);
        
//        jndiContextTxt.setName("jndiContextTxt");
//        jndiContextTxt.setTitle("JNDI context");
//        jndiContextTxt.setDefaultValue(loginDetails.getJndiContext());
//        jndiContextTxt.setWidth(250);
//        jndiContextTxt.setRequired(false);
//        jndiContextTxt.setVisible(false);
        
//        rmiUrlTxt.setName("rmiUrlTxt");
//        rmiUrlTxt.setTitle("RMI URL");
//        rmiUrlTxt.setDefaultValue(loginDetails.getRmiUrl());
//        rmiUrlTxt.setWidth(250);
//        rmiUrlTxt.setRequired(false);
//        rmiUrlTxt.setVisible(false);
        
        final SpacerItem rmiSpacer = new SpacerItem();
        rmiSpacer.setStartRow(true);
        rmiSpacer.setEndRow(true);
        rmiSpacer.setVisible(false);

  /*      connectionType.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                if (connectionType.getValue().equals("1")) {
                    jndiNameTxt.setRequired(false);
                    jndiNameTxt.setVisible(false);
                    jndiContextTxt.setRequired(false);
                    jndiContextTxt.setVisible(false);
                    rmiUrlTxt.setRequired(false);
                    rmiUrlTxt.setVisible(false);
                    rmiSpacer.setVisible(false);

                    configFilePathTxt.setVisible(true);
                    configFilePathTxt.setRequired(true);
                    homeDirPathTxt.setVisible(true);
                    homeDirPathTxt.setRequired(true);
                } else if (connectionType.getValue().equals("2")) {
                    configFilePathTxt.setRequired(false);
                    configFilePathTxt.setVisible(false);
                    homeDirPathTxt.setRequired(false);
                    homeDirPathTxt.setVisible(false);
                    rmiUrlTxt.setRequired(false);
                    rmiUrlTxt.setVisible(false);
                    rmiSpacer.setVisible(false);

                    jndiNameTxt.setVisible(true);
                    jndiNameTxt.setRequired(true);
                    jndiContextTxt.setVisible(true);
                    jndiContextTxt.setRequired(true);
                } else {
                    configFilePathTxt.setRequired(false);
                    configFilePathTxt.setVisible(false);
                    homeDirPathTxt.setRequired(false);
                    homeDirPathTxt.setVisible(false);
                    jndiNameTxt.setRequired(false);
                    jndiNameTxt.setVisible(false);
                    jndiContextTxt.setRequired(false);
                    jndiContextTxt.setVisible(false);

                    rmiUrlTxt.setVisible(true);
                    rmiUrlTxt.setRequired(true);
                    rmiSpacer.setVisible(true);
                }
            }
        });
        */ 

//		//Set up view of login panel as appropriate
        if (defaultValue.equals("1")) {
            jndiNameTxt.setRequired(false);
            jndiNameTxt.setVisible(false);
//            jndiContextTxt.setRequired(false);
//            jndiContextTxt.setVisible(false);
//            rmiUrlTxt.setRequired(false);
//            rmiUrlTxt.setVisible(false);
            rmiSpacer.setVisible(false);

//            configFilePathTxt.setVisible(true);
//            configFilePathTxt.setRequired(true);
//            homeDirPathTxt.setVisible(true);
//            homeDirPathTxt.setRequired(true);
        } else if (defaultValue.equals("2")) {
//            configFilePathTxt.setRequired(false);
//            configFilePathTxt.setVisible(false);
//            homeDirPathTxt.setRequired(false);
//            homeDirPathTxt.setVisible(false);
//            rmiUrlTxt.setRequired(false);
//            rmiUrlTxt.setVisible(false);
            rmiSpacer.setVisible(false);

            jndiNameTxt.setVisible(true);
            jndiNameTxt.setRequired(true);
//            jndiContextTxt.setVisible(true);
//            jndiContextTxt.setRequired(true);
        } else {
//            configFilePathTxt.setRequired(false);
//            configFilePathTxt.setVisible(false);
//            homeDirPathTxt.setRequired(false);
//            homeDirPathTxt.setVisible(false);
            jndiNameTxt.setRequired(false);
            jndiNameTxt.setVisible(false);
//            jndiContextTxt.setRequired(false);
//            jndiContextTxt.setVisible(false);

//            rmiUrlTxt.setVisible(true);
//            rmiUrlTxt.setRequired(true);
            rmiSpacer.setVisible(true);
        }

        jndiNameTxt.setRequired(true);
        jndiNameTxt.setVisible(true);
        rmiSpacer.setVisible(true);
        
        workspaceTxt.setName("workspaceTxt");
        workspaceTxt.setTitle("Workspace");
        workspaceTxt.setDefaultValue(loginDetails.getWorkSpace());
        workspaceTxt.setWidth(250);
        workspaceTxt.setRequired(true);
        usernameTxt.setName("usernameTxt");
        usernameTxt.setTitle("Username");
        usernameTxt.setDefaultValue(loginDetails.getUserName());
        usernameTxt.setWidth(250);
        usernameTxt.setRequired(true);
        passwordTxt.setName("passwordTxt");
        passwordTxt.setTitle("Password");
        passwordTxt.setDefaultValue(loginDetails.getPassword());
        passwordTxt.setWidth(250);
        SubmitItem loginSubmitItem = new SubmitItem("loginSubmitItem");
        loginSubmitItem.setTitle("Login");
        loginSubmitItem.setWidth(100);
        VStack vStack = new VStack();
        SpacerItem spacerItem1 = new SpacerItem();
        SpacerItem spacerItem2 = new SpacerItem();
        spacerItem1.setStartRow(true);
        spacerItem1.setEndRow(true);
        spacerItem2.setStartRow(true);
        spacerItem2.setEndRow(false);
        loginSubmitItem.setStartRow(false);
        loginSubmitItem.setEndRow(true);
        
        class LoginSubmitValuesHandler implements SubmitValuesHandler {

            private Explorer jcrExplorer;

            public LoginSubmitValuesHandler(Explorer jcrExplorer) {
                this.jcrExplorer = jcrExplorer;
            }

            @Override
            public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
                if (loginForm.validate()) {
                    loginWindow.hide();
                    showLoadingImg();

                   service.loginViaJndi(jndiNameTxt.getValue().toString(),"jndi/context",
                                workspaceTxt.getValue().toString(), usernameTxt.getValue()
                                .toString(), passwordTxt.getValue().toString(),
                                new LoginServiceCallback(jcrExplorer));
                    
//					drawMainLayout();



//		        	String rmiUrlText = rmiUrlTxt.getValue().toString(); 
//		        	String workspaceText = workspaceTxt.getValue().toString();
//		        	String usernameText = usernameTxt.getValue().toString();
//		        	String passwordText = passwordTxt.getValue() != null ? passwordTxt.getValue().toString() : "";
//		        	
//		        	service.login(rmiUrlText, workspaceText,usernameText, passwordText, new LoginServiceCallback(jackrabbitExplorer));
                }
            }
        };
        loginForm.addSubmitValuesHandler(new LoginSubmitValuesHandler(this));
        loginForm.setSaveOnEnter(true);
        //loginForm.setItems(rmiUrlTxt, workspaceTxt, spacerItem1, usernameTxt, passwordTxt, spacerItem1, spacerItem2, loginSubmitItem);

        loginForm.setItems( spacerItem1, jndiNameTxt, rmiSpacer, spacerItem1, usernameTxt,
                passwordTxt, workspaceTxt, spacerItem1, spacerItem2, loginSubmitItem);

        vStack.setTop(30);
        vStack.addMember(loginForm);
        loginWindow.addChild(vStack);
        loginWindow.setTitle("Login");
        loginWindow.setCanDragReposition(true);
        loginWindow.setCanDragResize(false);
        loginWindow.setShowMinimizeButton(false);
        loginWindow.setShowCloseButton(false);
        loginWindow.setHeight(350);
        loginWindow.setWidth(400);
        loginWindow.setAutoCenter(true);
        loginWindow.show();
        usernameTxt.focusInItem();
    }

    private void showPossibleIcons(String path) {
        showLoadingImg();
        service.getPossibleIconPaths(path, new RemoteFileServiceCallback(this));
    }

    public void changeCurrentNodeTypeAssociation(String iconPath) {
        changeNodeTypeIconAssociation(changeIconCellType, iconPath);
    }

    private void changeNodeTypeIconAssociation(String nodeType, String iconPath) {
        service.changeNodeTypeIconAssociation(nodeType, iconPath, new ChangeNodeTypeIconAssociationCallback(this));
    }
    
    /**
     * Form for query path to go.
     */
    private class PathPanel extends DynamicForm {
        private TextItem pathEditor = new TextItem();
        private SubmitItem goButton = new SubmitItem();
        
        public PathPanel() {
            super();
            setID("pathPanel");
            setMargin(0);

            pathEditor.setTitle("Path");
            pathEditor.setWidth(500);
            pathEditor.setStartRow(true);
            pathEditor.setEndRow(false);
            pathEditor.setRequired(true);
        
            goButton.setTitle("Go");
            goButton.setWidth(100);
            goButton.setEndRow(true);
            goButton.setStartRow(false);
            
            this.setNumCols(3);
            this.setWidth(700);
            setItems(pathEditor, goButton);            
        }
      
    }
    
    /**
     * Node navigator.
     */
    public class Navigator extends Label {
        private Tree jcrTree = new Tree();
        private TreeGrid jcrTreeGrid = new TreeGrid();
        private HLayout layout = new HLayout();
        
        public Navigator() {
            super();            
            setAlign(Alignment.CENTER);
            setOverflow(Overflow.HIDDEN);
            setWidth("20%");
            setShowResizeBar(true);

            //create tree tabset
            Tab treeTab = new Tab();
            
            jcrTree.setModelType(TreeModelType.PARENT);
            jcrTree.setNameProperty("title");
            jcrTree.setRoot(jcrRoot);
            jcrTreeGrid.setData(jcrTree);
            jcrTreeGrid.setWidth100();
            jcrTreeGrid.setHeight100();
            jcrTreeGrid.setCanDragResize(true);
            jcrTreeGrid.setAnimateFolders(true);
            jcrTreeGrid.setAnimateFolderSpeed(450);
            jcrTreeGrid.setCustomIconProperty("treeGridIcon");
            jcrTreeGrid.setTreeFieldTitle("Workspace: " + workspaceTxt.getValue().toString());
            jcrTreeGrid.setCanReorderRecords(true);
            jcrTreeGrid.setCanAcceptDroppedRecords(true);
            jcrTreeGrid.setCanDragRecordsOut(true);
            jcrTreeGrid.setShowConnectors(true);
            jcrTreeGrid.setSelectionType(SelectionStyle.SINGLE);
            jcrTreeGrid.setContextMenu(createRightClickMenu());
            jcrTreeGrid.addCellMouseDownHandler(new JcrTreeCellMouseDownHandler());
            jcrTreeGrid.addDropHandler(new JcrTreeDropHandler(Explorer.this));
            
            layout.setCanDragResize(true);
            layout.setMembersMargin(10);
            layout.addChild(jcrTreeGrid);
            jcrTreeGrid.addCellClickHandler(new JcrTreeGridCellClickHandler());
            
  //          treeTab.setPane(createNavigation());
            treeTab.setPane(jcrTreeGrid);
            treeTab.setTitle("Repository");

            TabSet tabset = new TabSet();
            
            tabset.setTabs(treeTab);        
            tabset.setWidth100();
            tabset.setHeight100();
                
            addChild(tabset);
            
        }
        
        public JcrTreeNode find(String path) {
            return (JcrTreeNode) jcrTree.find(path);
        }
        
        public void openFolder(TreeNode node) {
            jcrTree.openFolder(node);
        }
        
        public TreeNode[] getChildren(JcrTreeNode node) {
            return jcrTree.getChildren(node);
        }
        
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
        
        public void addList(JcrTreeNode[] list, String path) {
            jcrTree.addList(list, path);
        }
        
        public void addList(JcrTreeNode[] list, TreeNode parent) {
            jcrTree.addList(list, parent);
        }
        
        public JcrTreeNode getSelectedRecord() {
            return (JcrTreeNode) jcrTreeGrid.getSelectedRecord();
        }
    }
    
    private class DetailsPanel extends Tab {
        private GeneralNodeInformationPanel generalInfo = new GeneralNodeInformationPanel();
        private PropertiesPanel properties;
        private AccessControlPanel accessControl;
                
        public DetailsPanel() {
            super();
            setTitle("Node Properties");
            
            properties = new PropertiesPanel();
            accessControl = new AccessControlPanel();
            
            VLayout vLayout = new VLayout();

            vLayout.addMember(new Strut(15));
            vLayout.addMember(generalInfo);
            vLayout.addMember(new Strut(15));
            
            SectionStack stack = new SectionStack();
            stack.setWidth100();
            stack.setHeight100();
            stack.setVisibilityMode(VisibilityMode.MULTIPLE);
            
            SectionStackSection psection = new SectionStackSection("Properties");
            psection.setExpanded(true);
            
            
            psection.addItem(properties);
            stack.addSection(psection);
            
            SectionStackSection acl = new SectionStackSection("Access Control");
            acl.addItem(accessControl);
            acl.setExpanded(true);
            
            stack.addSection(acl);
            
            vLayout.addMember(stack);
            setPane(vLayout);
        }
        
        public void setCanEditProperties(boolean flag) {
            this.properties.setCanEdit(flag);
        }
        
        public void refreshProperties() {
            properties.refresh();
        }
    
    }
    
    private class PropertiesPanel extends VLayout {
        private ListGrid grid = new ListGrid();
        private PropertiesToolBar toolBar = new PropertiesToolBar();
        
        public PropertiesPanel() {
            super();
            this.setBackgroundColor("#d3d3d3");
            
            grid.setWidth(500);
            grid.setHeight(224);
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            grid.setEditByCell(true);
            
            ListGridField propertyField = new ListGridField("property", "Property");
            propertyField.setCanEdit(false);
            propertyField.setShowHover(true);
            
            ListGridField valueField = new ListGridField("value", "Value");
            valueField.setShowHover(true);
            
            grid.setFields(propertyField, valueField);
            
            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();
            
            ListGridRecord listGridRecord = new ListGridRecord();
            grid.setData(new ListGridRecord[]{listGridRecord});
            
            grid.addCellSavedHandler(new PropertiesCellSavedHandler(Explorer.this));
            grid.addCellClickHandler(new PropertiesCellClickHandler());
            
            grid.setContextMenu(createPropertyRightClickMenu());
                        
            addMember(toolBar);
            addMember(grid);
        }        
        
        public void setCanEdit(boolean flag) {
            this.grid.setCanEdit(flag);
        }
        
        public void refresh() {
            grid.setData(propertiesListGridRecords);
        }
    }
    
    private class PropertiesToolBar extends HLayout {
        public PropertiesToolBar() {
            super();
            setMargin(3);
            
            Menu nodeMenu = new Menu();  
            nodeMenu.setShowShadow(true);  
            nodeMenu.setShadowDepth(10);
        
            MenuItem newNode = new MenuItem("New", "icons/26.png", "Ctrl+N");
            MenuItem renameNode = new MenuItem("Rename node", "icons/69.png", "Ctrl+R");
            MenuItem removeNode = new MenuItem("Remove node", "icons/33.png", "Ctrl+D");
            
            MenuItemSeparator separator = new MenuItemSeparator();
            
            nodeMenu.setItems(newNode, renameNode, separator, removeNode);
            
            IMenuButton nodeButton = new IMenuButton("Node", nodeMenu);
            this.addMember(nodeButton);
            
            Menu propsMenu = new Menu();
            propsMenu.setShowShadow(true);  
            propsMenu.setShadowDepth(10);
        
            MenuItem newProperty = new MenuItem("New");
            MenuItem renameProperty = new MenuItem("Rename node");
            MenuItem removeProperty = new MenuItem("Remove node");
            
            propsMenu.setItems(newProperty, renameProperty, separator, removeProperty);

            IMenuButton propertyButton = new IMenuButton("Property", propsMenu);
            
//            this.addMember(new Strut(5));            
            this.addMember(propertyButton);
        }
    }
    
    private class AccessControlPanel extends VLayout {
        public AccessControlPanel() {
            super();            
            setHeight(250);
        }
    }
    
    private class GeneralNodeInformationPanel extends VLayout {
        private RecordPane currentNodeName = new RecordPane("Current Node");
        private RecordPane primaryType = new RecordPane("Primary Type");
        private RecordPane versions = new RecordPane("Number of Versions");
        
        public GeneralNodeInformationPanel() {
            super();            
            setWidth100();
            addMember(currentNodeName);
            addMember(primaryType);
            addMember(versions);
        }
    }
    
    private class RecordPane extends HLayout {
        private Label valueLabel;
        
        public RecordPane(String title) {
            Label titleLabel = new Label();
            titleLabel.setContents(title + ":");
            titleLabel.setWidth(200);
            
            valueLabel = new Label();
            valueLabel.setContents("N/A");
            
            addMember(titleLabel);
            addMember(valueLabel);
        }
        
        public void setValue(String value) {
            valueLabel.setContents(value);
        }
    }
    
    private class Strut extends HLayout {
        public Strut(int size) {
            super();
            setHeight(size);
        }
    }
    
    private class RepositoryTabPanel extends Tab {
        private final RepositoryInfoPanel repositoryInfo;
        private final NodeTypesPanel nodeTypes;
        
        public RepositoryTabPanel() {
            super();
            
            setTitle("Repository");
            
            repositoryInfo = new RepositoryInfoPanel();
            nodeTypes = new NodeTypesPanel();
            
            VLayout layout = new VLayout();
            SectionStack stack = new SectionStack();
            
            SectionStackSection section1 = new SectionStackSection("Repository descriptor");
            section1.setExpanded(true);
            section1.addItem(repositoryInfo);
            stack.addSection(section1);
            
            
            SectionStackSection section2 = new SectionStackSection("Node type management");
            section2.setExpanded(true);
            section2.addItem(nodeTypes);
            stack.addSection(section2);
            
            layout.addMember(stack);
            setPane(layout);
        }
    }
    
    private class RepositoryInfoPanel extends VLayout {
        public RepositoryInfoPanel() {
            super();
        }
    }
    
    private class NodeTypesPanel extends VLayout {
        public NodeTypesPanel() {
            super();
        }
    }
    
    private class QueryTabPanel extends Tab {
        public QueryTabPanel() {
            super();
            setTitle("Query");
        } 
    }
}