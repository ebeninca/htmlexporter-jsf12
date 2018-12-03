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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.custom.datalist.HtmlDataList;

public class CSVExporter extends Exporter {

	@Override
	public void export(final ActionEvent event, final String tableId, final FacesContext context,
			final String filename, final String tableTitle, final boolean pageOnly, final String encodingType,
			final MethodExpression preProcessor, final MethodExpression postProcessor) throws IOException {

		HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
		OutputStream os = response.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os, encodingType);
		PrintWriter writer = new PrintWriter(osw);

		final StringTokenizer st = new StringTokenizer(tableId, ",");
		while (st.hasMoreElements()) {
			final String tableName = (String) st.nextElement();
			final UIComponent component = event.getComponent().findComponent(tableName);
			if (component == null) {
				throw new FacesException("Cannot find component \"" + tableName + "\" in view.");
			}
			if (!(component instanceof HtmlDataTable || component instanceof HtmlDataList)) {
				throw new FacesException("Unsupported datasource target:\"" + component.getClass().getName()
						+ "\", exporter must target a PrimeFaces HtmlDataTable/HtmlDataList.");
			}

			HtmlDataList list = null;
			HtmlDataTable table = null;
			List<UIColumn> columns = null;

			if (component instanceof HtmlDataList) {
				list = (HtmlDataList) component;
				columns = getColumnsToExport(list, null);
			} else {
				table = (HtmlDataTable) component;
				columns = getColumnsToExport(table, null);
			}

			addColumnHeaders(writer, columns);

			int first = pageOnly ? table.getFirst() : 0;
			int size = pageOnly ? (first + table.getRows()) : table.getRowCount();

			for (int i = first; i < size; i++) {
				table.setRowIndex(i);
				addColumnValues(writer, columns);
				writer.write("\n");
			}

			table.setRowIndex(-1);
		}

		response.setContentType("text/csv");
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		response.setHeader("Content-disposition", "attachment;filename=" + filename + ".csv");

		writer.flush();
		writer.close();

		response.getOutputStream().flush();
	}

	private void addColumnValues(PrintWriter writer, List<UIColumn> columns) throws IOException {
		for (Iterator<UIColumn> iterator = columns.iterator(); iterator.hasNext();) {
			UIColumn column = (UIColumn) iterator.next();

			if (column.isRendered()) {
				addColumnValue(writer, column.getChildren());

				if (iterator.hasNext())
					writer.write(",");
			}
		}
	}

	private void addColumnHeaders(PrintWriter writer, List<UIColumn> columns) throws IOException {
		for (Iterator<UIColumn> iterator = columns.iterator(); iterator.hasNext();) {
			UIColumn column = (UIColumn) iterator.next();

			if (column.isRendered()) {
				addColumnValue(writer, column.getHeader());

				if (iterator.hasNext())
					writer.write(",");
			}
		}

		writer.write("\n");
	}

	private void addColumnValue(PrintWriter writer, UIComponent component) throws IOException {
		String value = component == null ? "" : exportValue(FacesContext.getCurrentInstance(), component);

		writer.write("\"" + value + "\"");
	}

	private void addColumnValue(PrintWriter writer, List<UIComponent> components) throws IOException {
		StringBuilder builder = new StringBuilder();

		for (UIComponent component : components) {
			if (component.isRendered()) {
				String value = exportValue(FacesContext.getCurrentInstance(), component);

				builder.append(value);
			}
		}

		writer.write("\"" + builder.toString() + "\"");
	}

	@Override
	public void customFormat(String facetBackground, String facetFontSize, String facetFontColor,
			String facetFontStyle, String fontName, String cellFontSize, String cellFontColor,
			String cellFontStyle, String datasetPadding, String orientation) throws IOException {
		// TODO Auto-generated method stub

	}
}
