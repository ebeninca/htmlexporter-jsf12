/*
 * Copyright 2009 Prime Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.gov.dataprev.htmlexporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

public class DataExporter implements ActionListener, StateHolder {

	private ValueExpression target;
	private ValueExpression type;
	private ValueExpression fileName;
	private ValueExpression tableTitle;
	private ValueExpression encoding;
	private ValueExpression pageOnly;
	private MethodExpression preProcessor;
	private MethodExpression postProcessor;
	private ValueExpression facetBackground;
	private ValueExpression facetFontSize;
	private ValueExpression facetFontColor;
	private ValueExpression facetFontStyle;
	private ValueExpression fontName;
	private ValueExpression cellFontSize;
	private ValueExpression cellFontColor;
	private ValueExpression cellFontStyle;
	private ValueExpression datasetPadding;
	private ValueExpression orientation;
	private ValueExpression skipComponents;

	public DataExporter() {
	}

	public DataExporter(ValueExpression target, ValueExpression type, ValueExpression fileName,
			ValueExpression tableTitle, ValueExpression pageOnly, ValueExpression encoding,
			MethodExpression preProcessor, MethodExpression postProcessor, ValueExpression facetBackground,
			ValueExpression facetFontSize, ValueExpression facetFontColor, ValueExpression facetFontStyle,
			ValueExpression fontName, ValueExpression cellFontSize, ValueExpression cellFontColor,
			ValueExpression cellFontStyle, ValueExpression datasetPadding, ValueExpression orientation,
			ValueExpression skipComponents) {
		this.target = target;
		this.type = type;
		this.fileName = fileName;
		this.tableTitle = tableTitle;
		this.pageOnly = pageOnly;
		this.preProcessor = preProcessor;
		this.postProcessor = postProcessor;
		this.encoding = encoding;
		this.facetBackground = facetBackground;
		this.facetFontSize = facetFontSize;
		this.facetFontColor = facetFontColor;
		this.facetFontStyle = facetFontStyle;
		this.fontName = fontName;
		this.cellFontSize = cellFontSize;
		this.cellFontColor = cellFontColor;
		this.cellFontStyle = cellFontStyle;
		this.datasetPadding = datasetPadding;
		this.orientation = orientation;
		this.skipComponents = skipComponents;
	}

	public void processAction(ActionEvent event) {
		FacesContext facesContext = FacesContext.getCurrentInstance();
		ELContext elContext = facesContext.getELContext();

		String tableId = (String) target.getValue(elContext);
		String exportAs = (String) type.getValue(elContext);
		String outputFileName = (String) fileName.getValue(elContext);

		String tableTitleValue = "";
		if (tableTitle != null) {
			tableTitleValue = (String) tableTitle.getValue(elContext);
		}

		String encodingType = "UTF-8";
		if (encoding != null) {
			encodingType = (String) encoding.getValue(elContext);
		}

		boolean isPageOnly = false;
		if (pageOnly != null) {
			isPageOnly = pageOnly.isLiteralText()
					? Boolean.valueOf(pageOnly.getValue(facesContext.getELContext()).toString())
					: (Boolean) pageOnly.getValue(facesContext.getELContext());
		}

		String facetBackgroundValue = null;
		if (facetBackground != null) {
			facetBackgroundValue = (String) facetBackground.getValue(elContext);
		}
		String facetFontSizeValue = "10";
		if (facetFontSize != null) {
			facetFontSizeValue = (String) facetFontSize.getValue(elContext);
		}
		String facetFontColorValue = null;
		if (facetFontColor != null) {
			facetFontColorValue = (String) facetFontColor.getValue(elContext);
		}
		String facetFontStyleValue = "BOLD";
		if (facetFontStyle != null) {
			facetFontStyleValue = (String) facetFontStyle.getValue(elContext);
		}
		String fontNameValue = null;
		if (fontName != null) {
			fontNameValue = (String) fontName.getValue(elContext);
		}
		String cellFontSizeValue = "8";
		if (cellFontSize != null) {
			cellFontSizeValue = (String) cellFontSize.getValue(elContext);
		}
		String cellFontColorValue = null;
		if (cellFontColor != null) {
			cellFontColorValue = (String) cellFontColor.getValue(elContext);
		}
		String cellFontStyleValue = "NORMAL";
		if (cellFontStyle != null) {
			cellFontStyleValue = (String) cellFontStyle.getValue(elContext);
		}
		String datasetPaddingValue = "5";
		if (datasetPadding != null) {
			datasetPaddingValue = (String) datasetPadding.getValue(elContext);
		}
		String orientationValue = "Portrait";
		if (orientation != null) {
			orientationValue = (String) orientation.getValue(elContext);
		}
		String skipComponentsValue = "";
		if (skipComponents != null) {
			skipComponentsValue = (String) skipComponents.getValue(elContext);
		}

		try {
			Exporter exporter = ExporterFactory.getExporterForType(exportAs);

			final StringTokenizer st = new StringTokenizer(tableId, ",");
			List<HtmlDataTable> tables = new ArrayList<HtmlDataTable>();
			while (st.hasMoreElements()) {

				final String tableName = (String) st.nextElement();
				UIComponent target = event.getComponent().findComponent(tableName);
				if (target == null)
					throw new FacesException("Cannot find component \"" + tableId + "\" in view.");
				if (!(target instanceof HtmlDataTable))
					throw new FacesException("Unsupported datasource target:\"" + target.getClass().getName()
							+ "\", exporter must target a PrimeFaces DataTable.");

				tables.add((HtmlDataTable) target);
			}

			exporter.setSkipComponents(skipComponentsValue);
			exporter.customFormat(facetBackgroundValue, facetFontSizeValue, facetFontColorValue,
					facetFontStyleValue, fontNameValue, cellFontSizeValue, cellFontColorValue,
					cellFontStyleValue, datasetPaddingValue, orientationValue);
			exporter.export(event, tableId, facesContext, outputFileName, tableTitleValue, isPageOnly,
					encodingType, preProcessor, postProcessor);

			facesContext.responseComplete();
		} catch (IOException e) {
			throw new FacesException(e);
		}
	}

	public boolean isTransient() {
		return false;
	}

	public void setTransient(boolean value) {
		// NoOp
	}

	public void restoreState(FacesContext context, Object state) {
		Object values[] = (Object[]) state;

		target = (ValueExpression) values[0];
		type = (ValueExpression) values[1];
		fileName = (ValueExpression) values[2];
		tableTitle = (ValueExpression) values[3];
		pageOnly = (ValueExpression) values[4];
		preProcessor = (MethodExpression) values[5];
		postProcessor = (MethodExpression) values[6];
		encoding = (ValueExpression) values[7];
		facetBackground = (ValueExpression) values[8];
		facetFontSize = (ValueExpression) values[9];
		facetFontColor = (ValueExpression) values[10];
		facetFontStyle = (ValueExpression) values[11];
		fontName = (ValueExpression) values[12];
		cellFontSize = (ValueExpression) values[13];
		cellFontColor = (ValueExpression) values[14];
		cellFontStyle = (ValueExpression) values[15];
		datasetPadding = (ValueExpression) values[16];
		orientation = (ValueExpression) values[17];
		skipComponents = (ValueExpression) values[18];
	}

	public Object saveState(FacesContext context) {
		Object values[] = new Object[19];

		values[0] = target;
		values[1] = type;
		values[2] = fileName;
		values[3] = tableTitle;
		values[4] = pageOnly;
		values[5] = preProcessor;
		values[6] = postProcessor;
		values[7] = encoding;
		values[8] = facetBackground;
		values[9] = facetFontSize;
		values[10] = facetFontColor;
		values[11] = facetFontStyle;
		values[12] = fontName;
		values[13] = cellFontSize;
		values[14] = cellFontColor;
		values[15] = cellFontStyle;
		values[16] = datasetPadding;
		values[17] = orientation;
		values[18] = skipComponents;

		return ((Object[]) values);
	}
}
