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
package org.modeshape.web.client.chart;

import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.Format;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class Chart extends VLayout {

    private final static int LABEL_SIZE = 30;
    private final static int Y_SCALE = 10;
    
    private YAxis yAxis = new YAxis();
    private XAxis xAxis = new XAxis();
    
    private int X, Y;
    
    private ViewPort viewPort = new ViewPort();
    private GWTCanvas canvas;

    /**
     * Creates chart with given title.
     * 
     * @param title chart's title.
     */
    public Chart(String title) {
        setWidth100();
        setHeight100();
        
        init();
    }

    public final void init() {
        Title title = new Title("");
        title.setWidth100();
        title.setHeight(30);
        addMember(title);

        HLayout layout = new HLayout();
        layout.setWidth100();
        layout.setHeight100();

        layout.addMember(yAxis);
        layout.addMember(viewPort);

        addMember(layout);
        addMember(xAxis);        
        addMember(new Footer());
    }

    public void drawChart(String[] xl, double[] x, double[] y1, double[] y2, double[] y3) {
        viewPort.drawChart(xl, x, y1, y2, y3);
    }
    
    
    private class Title extends VLayout {
        public Title(String title) {
            setContents(title);
        }
    }

    private class Footer extends VLayout {
        public Footer() {
            HLayout layout = new HLayout();
            addMember(layout);
            
            Label strut = new Label();
            strut.setWidth(LABEL_SIZE);
            
            Label min = new Label("<span style=\"color:red\">Min</span>");
            Label max = new Label("<span style=\"color:blue\">Max</span>");
            Label avg = new Label("<span style=\"color:green\">Average</span>");
            
            layout.addMember(strut);
            layout.addMember(min);
            layout.addMember(max);
            layout.addMember(avg);
        }
    }
    
    
    /**
     * YAxis.
     */
    private class YAxis extends VLayout {

        public YAxis() {
            setWidth(LABEL_SIZE);
            setHeight100();
        }
        
        private void clean() {
            Canvas[] members = this.getMembers();
            for (int i = 0; i < members.length; i++) {
                this.removeMember(members[i]);
            }
        }
        
        public void drawLabels(String[] x) {
            clean();
            int j = x.length - 1;
            for (int i = 0; i < x.length; i++) {
                addMember(new YLabel(x[j--]));
            }
        }
    }

    /**
     * Label for Y axis.
     * 
     */
    private class YLabel extends Label {
        
        public YLabel(String v) {
            setWidth100();
            setHeight100();
            setContents(v);
            setAlign(Alignment.CENTER);
            setLayoutAlign(Alignment.CENTER);
            setLayoutAlign(VerticalAlignment.BOTTOM);
            setValign(VerticalAlignment.BOTTOM);
        }
    }
    
    private class XAxis extends HLayout {

        public XAxis() {
            setHeight(LABEL_SIZE);
            setWidth100();
        }

        private void clean() {
            Canvas[] members = this.getMembers();
            for (int i = 0; i < members.length; i++) {
                this.removeMember(members[i]);
            }
        }
        
        public void drawLabels(String[] x) {
            clean();
            
            Label strut = new Label();
            strut.setWidth(LABEL_SIZE);
            strut.setHeight(LABEL_SIZE);
            
            addMember(strut);
            
            for (int i = 0; i < x.length; i++) {
                addMember(new XLabel(x[i]));
            }
        }
    }

    private class XLabel extends Label {
        public XLabel(String v) {
            setWidth100();
            setHeight100();
            setContents(v);
            setAlign(Alignment.LEFT);
            setLayoutAlign(Alignment.LEFT);
        }
    }
    
    private class ViewPort extends HLayout {
        
        private final Color COLOR[] = new Color[]{Color.RED, Color.BLUE, Color.GREEN};
                
        public ViewPort() {
            setWidth100();
            setHeight100();
            setBorder("2px solid black");
        }

        private void drawGrid(int N, int M) {
            if (canvas == null) {
                canvas = new GWTCanvas(getWidth(), getHeight());
                addMember(canvas);
            } else {
                canvas.clear();
            }
            
            //draw horizontal lines
            
            X = canvas.getCoordWidth();
            Y = canvas.getCoordHeight();
            
            double dx = X / N;
            double dy = Y / M;
            
            canvas.beginPath();

            canvas.setLineWidth(1); 
            canvas.setStrokeStyle(Color.BLACK);
              
            for (int i = 0; i < N; i++) {
                double x = dx * (i + 1);
                
                canvas.moveTo(x, 0);
                canvas.lineTo(x, Y);
            }

            //draw vertical lines
            int j = M -1;
            for (int i = 0; i < M; i++) {
                double y = Y - dy * (j--);
                
                canvas.moveTo(0, y);
                canvas.lineTo(X, y);
            }
            
            canvas.stroke();
            
        }
        
        private void drawLine(double[] y, double min, double max, Color color) {
            canvas.beginPath();

            canvas.setLineWidth(2); 
            canvas.setStrokeStyle(color);
                       
            double xScale = X / y.length;
            double yScale = Y / (max - min);
            
            
            for (int i = 0; i < y.length -1; i++) {
                canvas.moveTo(i * xScale, Y - (y[i] - min) * yScale);
                canvas.lineTo((i + 1)* xScale, Y - (y[i + 1] - min) * yScale);
            }
            
            canvas.stroke();
        }
        
        private String[] yLabels(double[]... y) {
            String[] labels = new String[Y_SCALE];
            
            double min = min(y);
            double max = max(y);
            
            double s = (max - min)/Y_SCALE;
            for (int i = 0; i < Y_SCALE; i++) {
                labels[i] = Format.toUSString((min + i * s), 2);
            }
            
            return labels;
        }
        
        public void drawChart(String[] xLabels, double[] x, double[]... y) {
            String[] yLabels = yLabels(y);
            
            yAxis.drawLabels(yLabels);
            xAxis.drawLabels(xLabels);
            
            double min = min(y);
            double max = max(y);
            
            drawGrid(xLabels.length, yLabels.length);
                        
            for (int i = 0; i < y.length; i++) {
                drawLine(y[i], min, max, COLOR[i]);
            }
        }
        
        private double min(double[]... x) {
            double min = x[0][0];
            for (int j = 0; j < x.length; j++ ) {
                for (int i = 0; i < x[j].length; i++) {
                    if (x[j][i] < min) min = x[j][i];
                }
            }
            return min;
        }

        private double max(double[]... x) {
            double max = x[0][0];
            for (int j = 0; j < x.length; j++) {
                for (int i = 0; i < x[j].length; i++) {
                    if (x[j][i] > max) max = x[j][i];
                }
            }
            return max;
        }
        
    }

}
