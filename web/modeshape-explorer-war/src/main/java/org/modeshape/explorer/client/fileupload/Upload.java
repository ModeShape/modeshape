/**
Copyright (c) <2009> <Pete Boysen>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package org.modeshape.explorer.client.fileupload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.NamedFrame;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * 
 *
 * @author Pete Boysen (pboysen@iastate.edu)
 * @since May 9, 2009
 * @version May 9, 2009
 */
public class Upload extends Canvas {
	public static enum Mode {DEFAULT, CONVERSIONS};
	public static final String TARGET="uploadTarget";

	private DynamicForm uploadForm;
	private UploadItem fileItem;
	private UploadListener listener;
	private List<HiddenItem> hiddenItems;
	/**
	 * 
	 */
	public Upload() {
		this(null,Mode.DEFAULT);
	}
	
	/**
	 * @param args
	 */
	public Upload(Map<String,String> args,Mode mode) {
		initComplete(this);
		initFailed(this);
		List<FormItem> items = new ArrayList<FormItem>();
		if (args != null) {
			hiddenItems = new ArrayList<HiddenItem>();
			for(String key: args.keySet()) {
				HiddenItem item = new HiddenItem(key);
				item.setValue(args.get(key));
				items.add(item);
				hiddenItems.add(item);
			};
		}
		ValuesManager vm = new ValuesManager();
		uploadForm = new DynamicForm();
		uploadForm.setValuesManager(vm);
		uploadForm.setEncoding(Encoding.MULTIPART);
		uploadForm.setTarget(TARGET);
		uploadForm.setCanSubmit(true);

		fileItem = new UploadItem("file");
		fileItem.setTitle("File");
		fileItem.setWidth(500);
		items.add(fileItem);
		fileItem.addChangedHandler(new ChangedHandler() {
			public void onChanged(ChangedEvent e) {
				System.out.println("change");
			}
		});
		Button uploadButton = new Button("Upload");
		uploadButton.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent e) {
				Object obj = fileItem.getDisplayValue();
				if (obj != null) {
					uploadForm.submitForm();
				} else
					SC.say("Please select a file.");
			}
		});
		VStack stack = new VStack();
		stack.setWidth100();
		stack.setMembersMargin(10);
		stack.setDefaultLayoutAlign(Alignment.CENTER);

		NamedFrame frame = new NamedFrame(TARGET);
		frame.setWidth("1");
		frame.setHeight("1");
		frame.setVisible(false);

		VStack mainLayout = new VStack();
		mainLayout.setWidth(300);
		mainLayout.setHeight(100);
		
		if (mode == Mode.CONVERSIONS) {		
			CheckboxItem unzip = new CheckboxItem("unzip");
			unzip.setDefaultValue(true);
			unzip.setTitle("Unzip .zip file");
			items.add(unzip);
			CheckboxItem overwrite = new CheckboxItem("overwrite");
			overwrite.setDefaultValue(false);
			overwrite.setTitle("Overwrite existing file"); 
			items.add(overwrite);
			CheckboxItem convertpdf = new CheckboxItem("convertpdf");
			convertpdf.setDefaultValue(true);
			convertpdf.setTitle("Convert Word document to PDF"); 
			items.add(convertpdf);
			CheckboxItem streaming = new CheckboxItem("streaming");
			streaming.setDefaultValue(true);
			streaming.setTitle("Convert video file to streaming format(flv)"); 
			items.add(streaming);
			CheckboxItem thumbnail = new CheckboxItem("thumbnail");
			thumbnail.setDefaultValue(true);
			thumbnail.setTitle("Make thumbnail(48x48) from image");
			items.add(thumbnail);
		}
		FormItem[] fitems = new FormItem[items.size()];
		items.toArray(fitems);
		uploadForm.setItems(fitems);
		stack.addMember(uploadForm);
		stack.addMember(uploadButton);
		mainLayout.addMember(stack);
		mainLayout.addMember(frame);
		addChild(mainLayout);
	}

	public String getFile() {
		Object obj = fileItem.getValue();
		if (obj == null)
			return null;
		else
			return obj.toString();
	}

	public void setHiddenItem(String name, String value) {
		for (HiddenItem item: hiddenItems)
			if (item.getName().equals(name)) {
				item.setValue(value);
				return;
			}
	}
	
	public void setAction(String url) {
		uploadForm.setAction(url);
	}
	
	public void setUploadListener(UploadListener listener) {
		this.listener = listener;
	}
	
	public void uploadComplete(String fileName) {
		if (listener != null)
			listener.uploadComplete(fileName);
	}
	
	public void uploadFailed(String msg) {
		if (listener != null)
			listener.uploadFailed(msg);
	}
	
	
	private native void initComplete(Upload upload) /*-{
	   $wnd.uploadComplete = function (fileName) {
	       upload.@org.modeshape.explorer.client.fileupload.Upload::uploadComplete(Ljava/lang/String;)(fileName);
	   };
	}-*/;
	
	private native void initFailed(Upload upload) /*-{
	   $wnd.uploadFailed = function (msg) {
	       upload.@org.modeshape.explorer.client.fileupload.Upload::uploadFailed(Ljava/lang/String;)(msg);
	   };
	}-*/;
}
