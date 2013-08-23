package org.modeshape.explorer.client.ui;

import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.callback.SearchServiceCallback;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * 
 * @author James Pickup
 *
 */
public class SearchTab {
	private TextItem searchFullTxt = new TextItem();
	private TextAreaItem searchXpathTxt = new TextAreaItem();
	private TextAreaItem searchSqlTxt = new TextAreaItem();
	
	public Tab searchTab(Explorer jackrabbitExplorer) {
		Tab searchTab = new Tab();
		searchTab.setTitle("Search");
		TabSet searchTabSet = new TabSet();
		searchTabSet.setTitle("SearchSet");
		searchTabSet.setTabs(fullTextSearchTab(jackrabbitExplorer), 
				xpathSearchTab(jackrabbitExplorer), sqlSearchTab(jackrabbitExplorer));
		searchTabSet.setWidth100();
		searchTabSet.setHeight100();
		VLayout vlLayout = new VLayout();
		vlLayout.addChild(searchTabSet);
		searchTab.setPane(vlLayout);
		return searchTab;
	}
	
	private Tab fullTextSearchTab(Explorer jackrabbitExplorer) {
		Tab searchFullTextTab = new Tab();
		searchFullTextTab.setTitle("Full Text Search");
		final DynamicForm searchFullTextForm = new DynamicForm();
		searchFullTextForm.setID("searchFullTextForm");
		searchFullTextForm.setNumCols(3);
		searchFullTxt.setName("searchFullTxt");
		searchFullTxt.setTitle("Full Text Search");
		searchFullTxt.setWidth(250);
		searchFullTxt.setRequired(true);
//		RegExpValidator regExpValidator = new RegExpValidator();
//		regExpValidator.setExpression("^[\\w\\d\\_\\.]{1,}$");
//		searchFullTxt.setValidateOnChange(true);
//		searchFullTxt.setValidators(regExpValidator);
		SubmitItem searchFullTextSubmitItem = new SubmitItem("searchFullTextSubmitItem");
		searchFullTextSubmitItem.setTitle("Search");
		searchFullTextSubmitItem.setWidth(100);
	    class SearchFullTextSubmitValuesHandler implements SubmitValuesHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public SearchFullTextSubmitValuesHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	    	public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
					if (searchFullTextForm.validate()) {
						Explorer.showLoadingImg();
						Explorer.service.fullTextSearch(searchFullTxt.getValue().toString(),
								new SearchServiceCallback(jackrabbitExplorer));
					}
	      }  
	    };
	    searchFullTextForm.addSubmitValuesHandler(new SearchFullTextSubmitValuesHandler(jackrabbitExplorer));
	    searchFullTextForm.setSaveOnEnter(true);
		searchFullTxt.setStartRow(true);
		searchFullTxt.setEndRow(false);
		searchFullTextSubmitItem.setStartRow(false);
		searchFullTextSubmitItem.setEndRow(true);
		searchFullTextForm.setItems(searchFullTxt, searchFullTextSubmitItem);
		searchFullTextTab.setPane(searchFullTextForm);
		return searchFullTextTab;
	}
	
	private Tab xpathSearchTab(Explorer jackrabbitExplorer) {
		Tab searchXpathTab = new Tab();
		searchXpathTab.setTitle("Xpath Search");
		final DynamicForm searchXpathForm = new DynamicForm();
		searchXpathForm.setID("searchXpathForm");
		searchXpathForm.setNumCols(3);
		searchXpathTxt.setName("searchXpathTxt");
		searchXpathTxt.setTitle("Xpath Search");
		searchXpathTxt.setWidth(250);
		searchXpathTxt.setRequired(true);
		SubmitItem searchXpathSubmitItem = new SubmitItem("searchXpathSubmitItem");
		searchXpathSubmitItem.setTitle("Search");
		searchXpathSubmitItem.setWidth(100);
	    class SearchXpathSubmitValuesHandler implements SubmitValuesHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public SearchXpathSubmitValuesHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	    	public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
	    		if (searchXpathForm.validate()) {
	    			Explorer.showLoadingImg();
	    			Explorer.service.xpathSearch(searchXpathTxt.getValue().toString(),new SearchServiceCallback(jackrabbitExplorer));
	    		}
	      }  
	    };
	    searchXpathForm.addSubmitValuesHandler(new SearchXpathSubmitValuesHandler(jackrabbitExplorer));
	    searchXpathForm.setSaveOnEnter(true);
		searchXpathTxt.setStartRow(true);
		searchXpathTxt.setEndRow(false);
		searchXpathSubmitItem.setStartRow(false);
		searchXpathSubmitItem.setEndRow(true);
		searchXpathForm.setItems(searchXpathTxt, searchXpathSubmitItem);
		searchXpathTab.setPane(searchXpathForm);
		return searchXpathTab;
	}
	
	private Tab sqlSearchTab(Explorer jackrabbitExplorer) {
		Tab searchSqlTab = new Tab();
		searchSqlTab.setTitle("SQL Search");
		final DynamicForm searchSqlForm = new DynamicForm();
		searchSqlForm.setID("searchSqlForm");
		searchSqlForm.setNumCols(3);
		searchSqlTxt.setName("searchSqlTxt");
		searchSqlTxt.setTitle("SQL Search");
		searchSqlTxt.setWidth(250);
		searchSqlTxt.setRequired(true);
		SubmitItem searchSqlSubmitItem = new SubmitItem("searchSqlSubmitItem");
		searchSqlSubmitItem.setTitle("Search");
		searchSqlSubmitItem.setWidth(100);
	    class SearchSqlSubmitValuesHandler implements SubmitValuesHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public SearchSqlSubmitValuesHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	    	public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
	    		if (searchSqlForm.validate()) {
	    			Explorer.showLoadingImg();
	    			Explorer.service.sqlSearch(searchSqlTxt.getValue().toString(), new SearchServiceCallback(jackrabbitExplorer));
	    		}
	      }  
	    };
	    searchSqlForm.addSubmitValuesHandler(new SearchSqlSubmitValuesHandler(jackrabbitExplorer));
	    searchSqlForm.setSaveOnEnter(true);
		searchSqlTxt.setStartRow(true);
		searchSqlTxt.setEndRow(false);
		searchSqlSubmitItem.setStartRow(false);
		searchSqlSubmitItem.setEndRow(true);
		searchSqlForm.setItems(searchSqlTxt, searchSqlSubmitItem);
		searchSqlTab.setPane(searchSqlForm);
		return searchSqlTab;
	}
}
