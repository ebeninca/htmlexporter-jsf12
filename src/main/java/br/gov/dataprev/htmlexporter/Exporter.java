/*
 * Copyright 2009-2011 Prime Technology.
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.ValueHolder;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlOutputLabel;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ActionEvent;

import org.apache.myfaces.custom.datalist.HtmlDataList;

public abstract class Exporter {

	protected String skipComponents;

	public abstract void export(ActionEvent event, String tableId, FacesContext facesContext,
			String outputFileName, String tableTitleValue, boolean pageOnly, String encodingType,
			MethodExpression preProcessor, MethodExpression postProcessor) throws IOException;

	public abstract void customFormat(String facetBackground, String facetFontSize, String facetFontColor,
			String facetFontStyle, String fontName, String cellFontSize, String cellFontColor,
			String cellFontStyle, String datasetPadding, String orientation) throws IOException;

	protected List<UIColumn> getColumnsToExport(UIData table, int[] excludedColumns) {
		List<UIColumn> allColumns = new ArrayList<UIColumn>();
		List<UIColumn> columnsToExport = new ArrayList<UIColumn>();

		for (UIComponent component : table.getChildren()) {
			if (component instanceof UIColumn)
				allColumns.add((UIColumn) component);
		}

		if (excludedColumns == null) {
			return allColumns;
		} else {
			for (int i = 0; i < allColumns.size(); i++) {
				if (Arrays.binarySearch(excludedColumns, i) < 0)
					columnsToExport.add(allColumns.get(i));
			}

			allColumns = null;

			return columnsToExport;
		}
	}

	protected String exportValue(FacesContext context, UIComponent component) {

		if (component instanceof HtmlCommandLink) { // support for PrimeFaces
													// and standard
													// HtmlCommandLink
			HtmlCommandLink link = (HtmlCommandLink) component;
			Object value = link.getValue();

			if (value != null) {
				return String.valueOf(value);
			} else {
				// export first value holder
				for (UIComponent child : link.getChildren()) {
					if (child instanceof ValueHolder) {
						return exportValue(context, child);
					}
				}

				return null;
			}
		} else if (component instanceof ValueHolder) {

			if (component instanceof EditableValueHolder) {
				Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
				if (submittedValue != null) {
					return submittedValue.toString();
				}
			}

			ValueHolder valueHolder = (ValueHolder) component;
			Object value = valueHolder.getValue();
			if (value == null)
				return "";

			// first ask the converter
			if (valueHolder.getConverter() != null) {
				return valueHolder.getConverter().getAsString(context, component, value);
			}
			// Try to guess
			else {
				ValueExpression expr = component.getValueExpression("value");
				if (expr != null) {
					Class<?> valueType = expr.getType(context.getELContext());
					if (valueType != null) {
						Converter converterForType = context.getApplication().createConverter(valueType);

						if (converterForType != null)
							return converterForType.getAsString(context, component, value);
					}
				}
			}

			// No converter found just return the value as string
			return value.toString();
		} else {
			// This would get the plain texts on UIInstructions when using
			// Facelets
			String value = component.toString();

			if (value != null)
				return value.trim();
			else
				return "";
		}
	}

	/**
	 * Version 2.0
	 */

	protected enum ColumnType {
		HEADER("header"), FOOTER("footer");

		private final String facet;

		ColumnType(String facet) {
			this.facet = facet;
		}

		public String facet() {
			return facet;
		}

		@Override
		public String toString() {
			return facet;
		}
	}

	protected String exportFacetValue(FacesContext context, UIComponent component) {
		if (component instanceof ValueHolder) {

			if (component instanceof EditableValueHolder) {
				Object submittedValue = ((EditableValueHolder) component).getSubmittedValue();
				if (submittedValue != null) {
					return submittedValue.toString();
				}
			}

			ValueHolder valueHolder = (ValueHolder) component;
			Object value = valueHolder.getValue();
			if (value == null) {
				return "";
			}

			// first ask the converter
			if (valueHolder.getConverter() != null) {
				return valueHolder.getConverter().getAsString(context, component, value);
			}
			return value.toString();
		} else {
			// This would get the plain texts on UIInstructions when using
			// Facelets
			String value = component.toString();

			return value.trim();
		}

	}

	protected List<UIColumn> getColumnsToExport(UIData table) {
		List<UIColumn> columns = new ArrayList<UIColumn>();

		for (UIComponent child : table.getChildren()) {
			if (child instanceof UIColumn) {
				UIColumn column = (UIColumn) child;

				columns.add(column);
			}
		}

		return columns;
	}

	protected String addColumnValues(HtmlDataList dataList, StringBuilder input) {
		for (UIComponent component : dataList.getChildren()) {
			if (component instanceof UIColumn) {
				UIColumn column = (UIColumn) component;
				for (UIComponent childComponent : column.getChildren()) {
					if (component.isRendered()) {
						String value = exportValue(FacesContext.getCurrentInstance(), childComponent);

						if (value != null) {
							input.append(value + "\n \n");
						}
					}
				}
				return input.toString();
			} else {
				if (component.isRendered()) {
					String value = exportValue(FacesContext.getCurrentInstance(), component);

					if (value != null) {
						input.append(value + "\n \n");
					}
				}
				return input.toString();
			}
		}
		return null;
	}

	protected int getColumnsCount(HtmlDataTable table) {
		int count = 0;

		for (UIComponent comp : table.getChildren()) {
			if (comp instanceof UIColumn) {
				if (!((UIColumn) comp).isRendered()) {
					continue;
				}
				count++;
			}
		}
		return count;
	}

	public boolean hasHeaderColumn(HtmlDataTable table) {
		for (UIComponent comp : table.getChildren()) {
			if (comp instanceof UIColumn) {
				UIColumn col = (UIColumn) comp;
				if (col.isRendered() && (col.getFacet("header") != null)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasFooterColumn(HtmlDataTable table) {
		for (UIComponent comp : table.getChildren()) {
			if (comp instanceof UIColumn) {
				UIColumn col = (UIColumn) comp;
				if (col.isRendered() && (col.getFacet("footer") != null)) {
					return true;
				}
			}
		}
		return false;
	}

	public String getFacetText(Iterator<UIComponent> comps) {
		while (comps.hasNext()) {
			UIComponent component = comps.next();
			if (component instanceof HtmlOutputText) {
				HtmlOutputText text = (HtmlOutputText) component;
				return text.getValue().toString();

			} else if (component instanceof HtmlOutputLabel) {
				HtmlOutputLabel label = (HtmlOutputLabel) component;
				return label.getValue().toString();
			}
		}
		return "";
	}

	public void setSkipComponents(String skipComponentsValue) {
		skipComponents = skipComponentsValue;
	}

}
