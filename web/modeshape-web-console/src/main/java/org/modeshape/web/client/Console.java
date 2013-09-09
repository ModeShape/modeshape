package org.modeshape.web.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point classes define
 * <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint {

    /**
     * The message displayed to the user when the server cannot be reached or
     * returns an error.
     */
    private static final String SERVER_ERROR = "An error occurred while "
            + "attempting to contact the server. Please check your network "
            + "connection and try again.";
    /**
     * Create a remote service proxy to talk to the server-side Greeting
     * service.
     */
    protected final JcrServiceAsync jcrService = GWT.create(JcrService.class);

    private VLayout mainForm = new VLayout();
    private ToolBar toolBar = new ToolBar(this);
    private Navigator navigator;
    protected final NodePanel nodePanel = new NodePanel();
    private RepositoryPanel repositoryPanel = new RepositoryPanel(this);
    private QueryPanel queryPanel = new QueryPanel(this);
    
    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        new LoginDialog(this).showDialog();
        /*        final Button sendButton = new Button("Send");
         final TextBox nameField = new TextBox();
         nameField.setText("GWT User");
         final Label errorLabel = new Label();

         // We can add style names to widgets
         sendButton.addStyleName("sendButton");

         // Add the nameField and sendButton to the RootPanel
         // Use RootPanel.get() to get the entire body element
         RootPanel.get("nameFieldContainer").add(nameField);
         RootPanel.get("sendButtonContainer").add(sendButton);
         RootPanel.get("errorLabelContainer").add(errorLabel);

         // Focus the cursor on the name field when the app loads
         nameField.setFocus(true);
         nameField.selectAll();

         // Create the popup dialog box
         final DialogBox dialogBox = new DialogBox();
         dialogBox.setText("Remote Procedure Call");
         dialogBox.setAnimationEnabled(true);
         final Button closeButton = new Button("Close");
         // We can set the id of a widget by accessing its Element
         closeButton.getElement().setId("closeButton");
         final Label textToServerLabel = new Label();
         final HTML serverResponseLabel = new HTML();
         VerticalPanel dialogVPanel = new VerticalPanel();
         dialogVPanel.addStyleName("dialogVPanel");
         dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
         dialogVPanel.add(textToServerLabel);
         dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
         dialogVPanel.add(serverResponseLabel);
         dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
         dialogVPanel.add(closeButton);
         dialogBox.setWidget(dialogVPanel);

         // Add a handler to close the DialogBox
         closeButton.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event) {
         dialogBox.hide();
         sendButton.setEnabled(true);
         sendButton.setFocus(true);
         }
         });

         // Create a handler for the sendButton and nameField
         class MyHandler implements ClickHandler, KeyUpHandler {

         public void onClick(ClickEvent event) {
         sendNameToServer();
         }

         public void onKeyUp(KeyUpEvent event) {
         if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
         sendNameToServer();
         }
         }

         private void sendNameToServer() {
         // First, we validate the input.
         errorLabel.setText("");
         String textToServer = nameField.getText();
         if (!FieldVerifier.isValidName(textToServer)) {
         errorLabel.setText("Please enter at least four characters");
         return;
         }

         // Then, we send the input to the server.
         sendButton.setEnabled(false);
         textToServerLabel.setText(textToServer);
         serverResponseLabel.setText("");
         greetingService.greetServer(textToServer, new AsyncCallback<String>() {
         public void onFailure(Throwable caught) {
         // Show the RPC error message to the user
         dialogBox.setText("Remote Procedure Call - Failure");
         serverResponseLabel.addStyleName("serverResponseLabelError");
         serverResponseLabel.setHTML(SERVER_ERROR);
         dialogBox.center();
         closeButton.setFocus(true);
         }

         public void onSuccess(String result) {
         dialogBox.setText("Remote Procedure Call");
         serverResponseLabel.removeStyleName("serverResponseLabelError");
         serverResponseLabel.setHTML(result);
         dialogBox.center();
         closeButton.setFocus(true);
         }
         });
         }
         }

         // Add a handler to send the name to the server
         MyHandler handler = new MyHandler();
         sendButton.addClickHandler(handler);
         nameField.addKeyUpHandler(handler);
         */
    }

    public void showMainForm() {
        mainForm.setLayoutMargin(5);
        mainForm.setWidth100();
        mainForm.setHeight100();
        mainForm.setBackgroundColor("#F0F0F0");
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
        viewPort.setTabs(nodePanel, repositoryPanel, queryPanel);

        viewPortLayout.addMember(viewPort);

        navigator = new Navigator(this);

        bottomPanel.addMember(navigator);
        bottomPanel.addMember(viewPortLayout);

        HLayout sp1 = new HLayout();
        sp1.setHeight("1%");

        HLayout sp2 = new HLayout();
        sp2.setHeight("1%");

        HLayout statusBar = new HLayout();
        statusBar.setHeight("5%");
        statusBar.setBorder("1px solid black");
        
        mainForm.addMember(toolBar);
//        mainForm.addMember(sp1);
        mainForm.addMember(topPanel);
        mainForm.addMember(sp2);
        mainForm.addMember(bottomPanel);
        mainForm.addMember(statusBar);
        
        mainForm.draw();
        
        repositoryPanel.display();
        queryPanel.init();
        //navigator.select();
    }
}
