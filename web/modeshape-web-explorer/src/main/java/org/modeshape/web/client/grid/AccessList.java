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
package org.modeshape.web.client.grid;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.layout.HLayout;
import org.modeshape.web.client.AddPrincipalDialog;
import org.modeshape.web.client.Contents;
import org.modeshape.web.client.grid.AccessList.AclRecord;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.Policy;

/**
 *
 * @author kulikov
 */
public class AccessList extends TabGrid<AclRecord, Policy> {
    
    private final AclRecord[] records = new AclRecord[]{
        new AclRecord(JcrPermission.ALL),
        new AclRecord(JcrPermission.LIFECYCLE_MANAGEMENT),
        new AclRecord(JcrPermission.LOCK_MANAGEMENT),
        new AclRecord(JcrPermission.MODIFY_ACCESS_CONTROL),
        new AclRecord(JcrPermission.MODIFY_PROPERTIES),
        new AclRecord(JcrPermission.NODE_TYPE_MANAGEMENT),
        new AclRecord(JcrPermission.READ),
        new AclRecord(JcrPermission.READ_ACCESS_CONTROL),
        new AclRecord(JcrPermission.REMOVE_CHILD_NODES),
        new AclRecord(JcrPermission.RETENTION_MANAGEMENT),
        new AclRecord(JcrPermission.VERSION_MANAGEMENT),
        new AclRecord(JcrPermission.WRITE)
    };
    
    private JcrNode node;
    protected final Contents contents;
    private ComboBoxItem principal;
    private final AddPrincipalDialog addAccessListDialog;
    
    public AccessList(Contents contents) {
        super("Access list");
        this.contents = contents;
        addAccessListDialog = new AddPrincipalDialog(contents);        
    }

    public void show(JcrNode node) {
/*        this.node = node;
        if (node.getAcl() == null || node.getAcl().principals().length == 0) {
            principal.setValueMap(new String[]{});
            principal.setValue("");
//            hideRecords();
            return;
        }
        
        if (node.getAcl() != null) {
            String[] principals = node.getAcl().principals();
            principal.setValueMap(principals);
            principal.setValue(principals[0]);
           
            ArrayList<Policy> values = new ArrayList();
            values.add(node.getAcl().getPolicy(principals[0]));
            setValues(values);
//            showPermissions(node.getAcl().getPolicy(principals[0]));
        }
        
//        showPermissions();
*/ 
    }
    
//    private void hideRecords() {
//        for (int i = 0; i < records.length; i++) {
//            records[i].setVisible(false);
//            viewPort.removeMember(records[i]);
//        }
//        emptyRecord.setVisible(true);
//        viewPort.addMember(emptyRecord);
//    }
//
//    private void displayRecords() {
//        emptyRecord.setVisible(false);
//        viewPort.removeChild(emptyRecord);
//        for (int i = 0; i < records.length; i++) {
//            records[i].setVisible(true);
//            viewPort.addMember(records[i]);
//        }
//    }
//    
//    private void showPermissions(Policy policy) {
//        displayRecords();
//        for (int i = 0; i < records.length; i++) {            
//            records[i].test(policy);
//        }
//    }

    @Override
    protected AclRecord[] records() {
        return records;
    }

    @Override
    protected HLayout tableHeader() {
        HLayout layout = new HLayout();
        layout.setHeight(30);
        layout.setWidth100();
        layout.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Permission</b>");
        name.setWidth(450);

        Label type = new Label("<b>Status</b>");
        type.setWidth(150);


        layout.addMember(name);
        layout.addMember(type);
        return layout;
    }

    @Override
    protected HLayout toolBar() {
        HLayout layout = new HLayout();
        layout.setBackgroundColor("#ffffff");
        layout.setAlign(Alignment.LEFT);
        layout.setDefaultLayoutAlign(Alignment.LEFT);
        layout.setLayoutAlign(Alignment.LEFT);
        layout.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        layout.setLayoutAlign(VerticalAlignment.CENTER);

        layout.setHeight(30);
        layout.setWidth100();

        principal = new ComboBoxItem("Principal");
        
        DynamicForm form = new DynamicForm();
        form.setItems(principal);

        layout.addMember(form);
        
        HLayout panel = new HLayout();

        panel.setAlign(Alignment.RIGHT);
        panel.setWidth100();
        panel.setDefaultLayoutAlign(Alignment.RIGHT);
        panel.setLayoutAlign(Alignment.RIGHT);
        panel.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        panel.setLayoutAlign(VerticalAlignment.CENTER);

        Label hint = new Label();
        hint.setWidth(200);
        hint.setAlign(Alignment.RIGHT);
        hint.setLayoutAlign(VerticalAlignment.BOTTOM);

        hint.setContents("Click button to switch view");

        Button addButton = new Button();
        addButton.setTitle("Add access list");
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addAccessListDialog.showModal();
            }
        });

        Button remButton = new Button();
        remButton.setTitle("Delete access list");
        remButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                @SuppressWarnings( "synthetic-access" )
                final String name = principal.getValueAsString();
                SC.ask("Remove acl for " + name + "?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        if (value) {
                            contents.removeAccessList(name);
                        }
                    }
                });

            }
        });


        Button applyButton = new Button();
        applyButton.setTitle("Apply access list");
        applyButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SC.ask("Apply ACL changes?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        if (value) {
                            contents.applyAccessList();
                        }
                    }
                });
            }
        });

        HLayout strut = new HLayout();
        strut.setWidth(5);


        layout.addMember(panel);

//            panel.addMember(hint);
        panel.addMember(new Strut(10));
        panel.addMember(addButton);
        panel.addMember(new Strut(5));
        panel.addMember(remButton);
        panel.addMember(strut);
        panel.addMember(applyButton);

        return layout;
        
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    protected void updateRecord(int pos, AclRecord record, Policy value) {
        record.test(value);
    }
    
    
    @SuppressWarnings( "synthetic-access" )
    public class AclRecord extends HLayout {

        private final JcrPermission permission;
        
        private Label name = new Label();
        private Label description = new Label();
        private BooleanField value = new BooleanField();
        
        public AclRecord(final JcrPermission permission) {
            super();
            this.permission = permission;
            
            setStyleName("grid");
            setHeight(30);
            setWidth100();
            setDefaultLayoutAlign(VerticalAlignment.CENTER);
            setDefaultLayoutAlign(Alignment.LEFT);

            setLayoutAlign(VerticalAlignment.CENTER);
            setLayoutAlign(Alignment.CENTER);

            setAlign(VerticalAlignment.CENTER);
            setAlign(Alignment.LEFT);

            name.setContents(permission.getDisplayName());
            name.setIcon("icons/shield.png");
            name.setStyleName("text");

            name.setWidth(350);
//            description.setWidth100();
            value.setWidth(50);
            value.setStyleName("text");
            value.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    node.getAcl().modify(principal.getValueAsString(), permission, !value.getValue());
                }
            });
            
            addMember(name);
            addMember(description);
            addMember(value);
        }

        
        private boolean test(Policy policy) {
            boolean val = policy.hasPermission(permission);
            value.setValue(val);
            return val;
        }
        
    }
        
}

