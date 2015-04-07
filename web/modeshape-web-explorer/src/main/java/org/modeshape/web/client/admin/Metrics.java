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
package org.modeshape.web.client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
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
 *
 * @author kulikov
 */
public class Metrics extends VLayout {

    private final Chart chart1 = new Chart("");
    private final Chart chart2 = new Chart("");
    
    private final DynamicForm f1 = new DynamicForm();
    private final DynamicForm f2 = new DynamicForm();
    
    private final ComboBoxItem cb1 = new ComboBoxItem("Select parameter");
    private final ComboBoxItem cb2 = new ComboBoxItem("Select parameter");
    
    private final ComboBoxItem tw1 = new ComboBoxItem("Time window");
    private final ComboBoxItem tw2 = new ComboBoxItem("Time window");

    private final AdminView adminView;
    
    public Metrics(AdminView adminView) {
        this.adminView = adminView;
        
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
        adminView.jcrService().getValueMetrics(new AsyncCallback<String[]>() {
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
        adminView.jcrService().getDurationMetrics(new AsyncCallback<String[]>() {
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
        adminView.jcrService().getTimeUnits(new AsyncCallback<String[]>() {
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
        adminView.jcrService().getValueStats(adminView.repository(), param, time, new AsyncCallback<Collection<Stats>>() {
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
        adminView.jcrService().getDurationStats(adminView.repository(), param, time, new AsyncCallback<Collection<Stats>>() {
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
        double[] x = new double[stats.size()];
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
            xl[j] = Integer.toString(j * dx);
        }

        chart.drawChart(xl, x, y1, y2, y3);
    }
}

