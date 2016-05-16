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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.layout.HLayout;
import java.util.ArrayList;
import org.modeshape.web.client.contents.PermissionsEditor.AclRecord;
import org.modeshape.web.client.grid.BooleanField;
import org.modeshape.web.client.grid.TabGrid;
import org.modeshape.web.shared.Align;
import org.modeshape.web.shared.Columns;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.Policy;

/**
 * Permissions editor.
 *
 * @author kulikov
 */
public class PermissionsEditor extends TabGrid<AclRecord, JcrPermission> {

    private final Policy ALL_PERMISSIONS_FOR_EVERYONE = new Policy();
    
    //header hight in pixels
    private final static int HEADER_HEIGHT = 30;
    
    protected final Contents contents;
    private JcrNode node;
    
    private ComboBoxItem principal;
    protected AddPolicyDialog addPolicyDialog;

    /**
     * Creates new Editor.
     * 
     * @param contents 
     */
    public PermissionsEditor(Contents contents) {
        super("");
        this.contents = contents;
        for (JcrPermission p : ALL_PERMISSIONS_FOR_EVERYONE.permissions()) {
            p.setStatus(true);
        }
        addPolicyDialog = new AddPolicyDialog(contents);        
    }

    /**
     * Displays permissions for the given node.
     * 
     * @param node 
     */
    public void show(JcrNode node) {
        this.node = node;
        if (node.getAcl() == null) {
            this.displayDisabledEditor();
        } else if (this.isAclDefined(node)) {
            this.selectFirstPrincipalAndDisplayPermissions(node);
        } else {
            this.displayEveryonePermissions();
        }
    }

    @Override
    protected AclRecord[] records() {
        int n = new Policy().permissions().size();
        AclRecord[] records = new AclRecord[n];
        for (int i = 0; i < n; i++) {
            records[i] = new AclRecord();
        }
        return records;
    }

    @Override
    protected HLayout tableHeader() {
        HLayout layout = new HLayout();

        layout.setHeight(30);
        layout.setWidth100();

        layout.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Permission</b>");
        name.setWidth100();

        Label type = new Label("<b>Status</b>");
        type.setWidth(50);


        layout.addMember(name);
        layout.addMember(type);

        return layout;
    }

    @Override
    protected HLayout toolBar() {
        Columns layout = new Columns(Align.LEFT, Align.CENTER);
        layout.setBackgroundColor("#ffffff");
        layout.setHeight(HEADER_HEIGHT);
        layout.setWidth100();

        //prepare form for the principal combobox
        DynamicForm form = new DynamicForm();
        form.setWidth100();

        //put into layout
        layout.addMember(form);

        //prepare combobox for principal selection
        principal = new ComboBoxItem("Principal");
        principal.setWidth("100%");

        //append on form
        form.setItems(principal);

        //buttons
        Label addPrincipalButton = new Label();
        addPrincipalButton.setStyleName("button-label");
        addPrincipalButton.setWidth(16);
        addPrincipalButton.setHeight(16);
        addPrincipalButton.setIcon("icons/group_blue_add.png");
        addPrincipalButton.setTooltip("Add new principal name");
        addPrincipalButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addPrincipal();
            }
        });
        
        layout.addMember(addPrincipalButton);

        Label delPrincipalButton = new Label();
        delPrincipalButton.setStyleName("button-label");
        delPrincipalButton.setWidth(16);
        delPrincipalButton.setHeight(16);
        delPrincipalButton.setIcon("icons/group_blue_remove.png");
        delPrincipalButton.setTooltip("Delete this principal");
        delPrincipalButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delPrincipal();
            }
        });
        
        layout.addMember(delPrincipalButton);

        return layout;
    }

    private void addPrincipal() {
        addPolicyDialog.showModal();
    }
    
    private void delPrincipal() {
        SC.ask("Do you want to delete principal " + principal.getValueAsString(), new BooleanCallback() {
            @Override
            public void execute(Boolean status) {
                if (status) {
                    contents.removeAccessList(principal.getValueAsString());
                }
            }
        });
    }
    
    @SuppressWarnings("synthetic-access")
    @Override
    protected void updateRecord(int pos, AclRecord record, JcrPermission value) {
        record.name.setContents(value.getDisplayName());
        record.value.setValue(value.getStatus());
        record.permission = value;
    }

    private boolean isAclDefined(JcrNode node) {
        return !(node.getAcl() == null || node.getAcl().principals().length == 0);
    }

    private void displayEveryonePermissions() {
        principal.setValueMap(new String[]{"Everyone"});
        principal.setValue("Everyone");
        principal.setDisabled(false);
        setValues(ALL_PERMISSIONS_FOR_EVERYONE.permissions());
    }

    private void displayDisabledEditor() {
        principal.setValueMap(new String[]{"Permission denied"});
        principal.setValue("Permission denied");
        principal.setDisabled(true);
        setValues(new ArrayList());
    }
    
    private void selectFirstPrincipalAndDisplayPermissions(JcrNode node) {
        String[] principals = node.getAcl().principals();
        principal.setValueMap(principals);
        principal.setValue(principals[0]);
        principal.setDisabled(false);
        setValues(node.getAcl().getPolicy(principals[0]).permissions());
    }

    @SuppressWarnings("synthetic-access")
    public class AclRecord extends HLayout {
        private JcrPermission permission;
        private Label name = new Label();
        private BooleanField value = new BooleanField();

        public AclRecord() {
            super();
            
            setStyleName("grid");
            setHeight(30);
            setWidth100();

            setDefaultLayoutAlign(VerticalAlignment.CENTER);
            setDefaultLayoutAlign(Alignment.LEFT);

            setLayoutAlign(VerticalAlignment.CENTER);
            setLayoutAlign(Alignment.CENTER);

            setAlign(VerticalAlignment.CENTER);
            setAlign(Alignment.LEFT);

            name.setIcon("icons/shield.png");
            name.setStyleName("text");
            name.setWidth100();

            value.setWidth(50);
            value.setStyleName("button-label");
            value.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    changePermission();
                }
            });

            addMember(name);
            addMember(value);
        }
        
        private void changePermission() {
            //this is everyone "principal"?
            if (node.getAcl() == null) {
                return;
            }            
            permission.setStatus(!permission.getStatus());
            contents.updateAccessList(principal.getValueAsString(), permission, permission.getStatus());
        }

    }
}
