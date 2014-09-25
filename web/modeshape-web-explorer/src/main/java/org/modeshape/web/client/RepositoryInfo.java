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
package org.modeshape.web.client;

import java.util.Collection;
import org.modeshape.web.client.grid.Descriptors;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Param;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author kulikov
 */
public class RepositoryInfo extends View {

    // private final RepositoryInfoPanel repositoryInfo;

    private Descriptors descriptors;

    private final NodeTypesPanel nodeTypes;
    private VLayout[] pages;

    public RepositoryInfo( Console console,
                           ViewPort viewPort,
                           View parent ) {
        super(viewPort, parent);

        Canvas text = new Canvas();
        text.setContents("<p>Repository descriptors are used to test support "
                         + "for repository features that have a behavioral (as opposed " + "to a data-model) aspect.</br>"
                         + "The full set of valid keys (both standard and implementation-specific) " + "for this repository</p>");
        text.setWidth100();
        text.setAutoHeight();
        text.setStyleName("caption");

        descriptors = new Descriptors(console);

        // setTitle("Repository");

        nodeTypes = new NodeTypesPanel();

        HLayout vstrut = new HLayout();
        vstrut.setHeight(15);

        HLayout bottomStrut = new HLayout();
        bottomStrut.setHeight(15);

        addMember(text);
        addMember(vstrut);
        addMember(descriptors);
        // addMember(new SwitchPanel());
        // addMember(repositoryInfo);
        // addMember(nodeTypes);
        // addMember(bottomStrut);

        // pages = new VLayout[]{repositoryInfo, nodeTypes};
        // showPage(0);
        // this.display();
    }

    public void show( JcrRepositoryDescriptor descriptor ) {
        descriptors.show(descriptor);
    }

    public void showRepositoryInfo() {
    }

    public final void display() {
        // viewPort().jcrService().repositoryInfo(viewPort().getRepositoryName(), new RepositoryInfoCallbackHandler());
        nodeTypes.display();
    }

    private void showPage( int k ) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisible(false);
        }
        pages[k].setVisible(true);
        pages[k].show();
    }

    @SuppressWarnings( "synthetic-access" )
    public class SwitchPanel extends HLayout {
        public SwitchPanel() {
            super();
            setHeight(30);
            setAlign(Alignment.RIGHT);

            Button repoButton = new Button();
            repoButton.setTitle("Repository descriptors");
            repoButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick( ClickEvent event ) {
                    showPage(0);
                }
            });
            repoButton.setHeight(25);

            Button nodeTypeButton = new Button();
            nodeTypeButton.setTitle("Node types");
            nodeTypeButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick( ClickEvent event ) {
                    showPage(1);
                }
            });
            nodeTypeButton.setHeight(25);

            HLayout strut = new HLayout();
            strut.setWidth(5);

            addMember(repoButton);
            addMember(strut);
            addMember(nodeTypeButton);
        }
    }

    private class NodeTypesPanel extends VLayout {
        private ListGrid grid = new ListGrid();

        public NodeTypesPanel() {
            super();

            grid.setWidth(500);
            grid.setHeight(224);
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(false);

            ListGridField iconField = new ListGridField("icon", " ");
            iconField.setType(ListGridFieldType.IMAGE);
            iconField.setImageURLPrefix("icons/bullet_");
            iconField.setImageURLSuffix(".png");
            iconField.setWidth(20);

            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField isPrimaryField = new ListGridField("isPrimary", "Primary");
            isPrimaryField.setCanEdit(false);
            isPrimaryField.setShowHover(true);

            ListGridField isMixinField = new ListGridField("isMixin", "Mixin");
            isMixinField.setCanEdit(false);
            isMixinField.setShowHover(true);

            ListGridField isAbstractField = new ListGridField("isAbstract", "Abstract");
            isAbstractField.setCanEdit(false);
            isAbstractField.setShowHover(true);

            grid.setFields(iconField, nameField, isPrimaryField, isMixinField, isAbstractField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();

            Label header = new Label();
            header.setContents("Node types");
            header.setHeight(25);
            header.setStyleName("header-label");
            header.setIcon("icons/sprocket.png");

            addMember(header);
            addMember(grid);

        }

        public void display() {
            /*            viewPort().jcrService().nodeTypes(viewPort().getRepositoryName(), 
                                viewPort().getWorkspaceName(), 
                                new AsyncCallback<Collection<JcrNodeType>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                SC.say(caught.getMessage());
                            }


                            @Override
                            public void onSuccess(Collection<JcrNodeType> types) {
                                ListGridRecord[] data = new ListGridRecord[types.size()];
                                int i = 0;
                                for (JcrNodeType type: types) {
                                    ListGridRecord record = new ListGridRecord();
                                    record.setAttribute("icon", "blue");
                                    record.setAttribute("name", type.getName());
                                    record.setAttribute("isPrimary", type.isPrimary());
                                    record.setAttribute("isMixin", type.isMixin());
                                    record.setAttribute("isAbstract", type.isAbstract());
                                    data[i++] = record;                        
                                }
                                grid.setData(data);
                            }
                        });*/
        }

    }

    public class RepositoryInfoCallbackHandler implements AsyncCallback<JcrRepositoryDescriptor> {

        @Override
        public void onFailure( Throwable caught ) {
            throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                           // Templates.
        }

        @Override
        public void onSuccess( JcrRepositoryDescriptor descriptor ) {
            Collection<Param> params = descriptor.info();
            ListGridRecord[] data = new ListGridRecord[params.size()];
            int i = 0;
            for (Param p : params) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("icon", "blue");
                record.setAttribute("name", p.getName());
                record.setAttribute("value", p.getValue());
                data[i++] = record;
            }
            // RepositoryInfo.this.repositoryInfo.grid.setData(data);
            // RepositoryInfo.this.repositoryInfo.draw();
        }
    }
}
