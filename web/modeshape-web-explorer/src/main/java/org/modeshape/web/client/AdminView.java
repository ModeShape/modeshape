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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.client.chart.Chart;
import org.modeshape.web.shared.Stats;

/**
 * @author kulikov
 */
public class AdminView extends View {

    private Console console;
    private BackupDialog backupDialog = new BackupDialog(this);
    private RestoreDialog restoreDialog = new RestoreDialog(this);
    private DynamicForm form = new DynamicForm();
    private MetricControl mc;
    
    public AdminView( Console console,
                      final JcrServiceAsync jcrService,
                      ViewPort viewPort ) {
        super(viewPort, null);
        this.console = console;

        addMember(new BackupControl());
        addMember(new RestoreControl());
        addMember(new DownloadControl());
        addMember(mc = new MetricControl());
        addMember(form);
    }

    @Override
    public void show() {
        super.show();
//        mc.init();
    }
    
    public void backup( String name ) {
        console.jcrService.backup(console.repository(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess( Object result ) {
                SC.say("Complete");
            }
        });
    }

    public void restore( String name ) {
        console.jcrService.restore(console.repository(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess( Object result ) {
                SC.say("Complete");
            }
        });
    }

    private void backupAndDownload() {
        console.jcrService.backup(console.contents().repository(), "zzz", new AsyncCallback<Object>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess( Object result ) {
                form.setAction(GWT.getModuleBaseForStaticFiles() + "backup/do?file=zzz");
                form.setMethod(FormMethod.GET);
                form.submitForm();
            }
        });
    }

    @SuppressWarnings( "synthetic-access" )
    private class BackupControl extends VLayout {

        public BackupControl() {
            super();
            setStyleName("admin-control");

            Label label = new Label("Backup");
            label.setIcon("icons/data.png");
            label.setStyleName("button-label");
            label.setHeight(25);
            label.addClickHandler(new ClickHandler() {
                @Override
                public void onClick( ClickEvent event ) {
                    backupDialog.showModal();
                }
            });

            Canvas text = new Canvas();
            text.setAutoHeight();
            text.setContents("Create backups of an entire repository (even when " + "the repository is in use)"
                             + "This works regardless of where the repository content " + "is persisted.");

            addMember(label);
            addMember(text);
        }
    }

    @SuppressWarnings( "synthetic-access" )
    private class RestoreControl extends VLayout {

        public RestoreControl() {
            super();
            setStyleName("admin-control");

            Label label = new Label("Restore");
            label.setIcon("icons/documents.png");
            label.setStyleName("button-label");
            label.setHeight(25);
            label.addClickHandler(new ClickHandler() {
                @Override
                public void onClick( ClickEvent event ) {
                    restoreDialog.showModal();
                }
            });

            Canvas text = new Canvas();
            text.setAutoHeight();
            text.setContents("Once you have a complete backup on disk, you can "
                             + "then restore a repository back to the state captured "
                             + "within the backup. To do that, simply start a repository "
                             + "(or perhaps a new instance of a repository with a "
                             + "different configuration) and, before it’s used by "
                             + "any applications, load into the new repository all of " + "the content in the backup. ");

            addMember(label);
            addMember(text);
        }
    }

    @SuppressWarnings( "synthetic-access" )
    private class DownloadControl extends VLayout {

        public DownloadControl() {
            super();
            setStyleName("admin-control");

            Label label = new Label("Backup & Download");
            label.setStyleName("button-label");
            label.setHeight(25);
            label.setIcon("icons/data.png");
            label.addClickHandler(new ClickHandler() {
                @Override
                public void onClick( ClickEvent event ) {
                    backupAndDownload();
                }
            });

            Canvas text = new Canvas();
            text.setAutoHeight();
            text.setContents("Create backups of an entire repository (even when "
                             + "the repository is in use) and download zip archive "
                             + "This works regardless of where the repository content " + "is persisted.");

            addMember(label);
            addMember(text);
        }
    }
    
    private class MetricControl extends VLayout {
        private final Chart chart1 = new Chart("");
        private final Chart chart2 = new Chart("");
        
        private DynamicForm f1 = new DynamicForm();
        private DynamicForm f2 = new DynamicForm();
        
        private ComboBoxItem cb1 = new ComboBoxItem("Select parameter");
        private ComboBoxItem cb2 = new ComboBoxItem("Select parameter");

        private ComboBoxItem tw1 = new ComboBoxItem("Time window");
        private ComboBoxItem tw2 = new ComboBoxItem("Time window");
        
        public MetricControl() {           
            setHeight(500);
            setWidth100();

            VLayout strut = new VLayout();
            strut.setHeight(30);
            
            addMember(strut);
            
            loadValueMetrics();
            loadDurationMetrics();
            loadTimeUnits();
            
            cb1.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    drawChart1();
                }
            });

            tw1.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    drawChart1();
                }
            });
            
            cb2.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    drawChart2();
                }
            });
            
            tw2.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    drawChart2();
                }
            });
            
            f1.setItems(cb1, tw1);            
            f2.setItems(cb2, tw2);
            
            HLayout layout = new HLayout();
            
            layout.setWidth100();
            layout.setHeight100();
            
            VLayout p1 = new VLayout();
            VLayout p2 = new VLayout();
            
            addMember(layout);
            
            layout.addMember(p1);
            layout.addMember(p2);
            
            chart1.setHeight100();
            chart2.setHeight100();

            chart1.setWidth100();
            chart2.setWidth100();
            
            p1.addMember(f1);
            p1.addMember(chart1);
            
            p2.addMember(f2);
            p2.addMember(chart2);
            
        }
        
        private void loadValueMetrics() {
            console.jcrService.getValueMetrics(new AsyncCallback<String[]>() {

                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(String[] result) {
                    cb1.setValueMap(result);
                }
            });
        }

        private void loadDurationMetrics() {
            console.jcrService.getDurationMetrics(new AsyncCallback<String[]>() {

                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(String[] result) {
                    cb2.setValueMap(result);
                }
            });
        }

        private void loadTimeUnits() {
            console.jcrService.getTimeUnits(new AsyncCallback<String[]>() {

                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(String[] result) {
                    tw1.setValueMap(result);
                    tw2.setValueMap(result);
                    
                    tw1.setValue(result[0]);
                    tw2.setValue(result[0]);
                }
            });
        }
        
        private void drawChart1() {
            drawValueHistory(chart1, cb1.getValueAsString(), tw1.getValueAsString());
        }
        
        private void drawChart2() {
            drawDurationHistory(chart2, cb2.getValueAsString(), tw2.getValueAsString());
        }
        
        private void drawValueHistory(final Chart chart, String param, String time) {
            console.jcrService.getValueStats(console.repository(), param, time, new AsyncCallback<Collection<Stats>>() {
                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(Collection<Stats> stats) {
                    drawHistory(chart, stats);
                }
            });
        }

        private void drawDurationHistory(final Chart chart, String param, String time) {
            console.jcrService.getDurationStats(console.repository(), param, time, new AsyncCallback<Collection<Stats>>() {
                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }

                @Override
                public void onSuccess(Collection<Stats> stats) {
                    drawHistory(chart, stats);
                }
            });
        }
        
        private void drawHistory(Chart chart, Collection<Stats> stats) {
            double[] x  = new double[stats.size()];
            double[] y1 = new double[stats.size()];
            double[] y2 = new double[stats.size()];
            double[] y3 = new double[stats.size()];
            
            int c = stats.size() > 24 ? 24 : stats.size();
            int dx = stats.size() / c;
            
            String[] xl = new String[c];
            
            int i = 0;
            for (Stats s : stats) {
                x[i] = i;
                y1[i] = s.min();
                y2[i] = s.max();
                y3[i] = s.avg();
                i++;
            }

            for (int j = 0; j < xl.length; j++) {                
                xl[j] = Integer.toString(j*dx);
            }
            
            chart.drawChart(xl, x, y1, y2, y3);
        }
        
    }
}

