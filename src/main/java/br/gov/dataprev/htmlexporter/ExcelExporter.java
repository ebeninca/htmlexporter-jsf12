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
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class ExcelExporter extends Exporter {

	@Override
	public void export(final ActionEvent event, final String tableId, final FacesContext context,
			final String filename, final String tableTitle, final boolean pageOnly, final String encodingType,
			final MethodExpression preProcessor, final MethodExpression postProcessor) throws IOException {

		Workbook wb = createWorkBook();
		Sheet sheet = wb.createSheet();

		if (preProcessor != null) {
			preProcessor.invoke(context.getELContext(), new Object[] { wb });
		}

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

			List<UIColumn> columns = null;
			int first = 0;
			int size = 0;

			if (component instanceof HtmlDataList) {
				HtmlDataList list = (HtmlDataList) component;
				columns = getColumnsToExport(list, null);
				first = pageOnly ? list.getFirst() : 0;
				size = pageOnly ? (first + list.getRows()) : list.getRowCount();

				addColumnHeaders(sheet, columns);
				int sheetRowIndex = 1;
				int numberOfColumns = columns.size();

				for (int i = first; i < size; i++) {
					list.setRowIndex(i);
					Row row = sheet.createRow(sheetRowIndex++);

					for (int j = 0; j < numberOfColumns; j++) {
						UIColumn column = columns.get(j);

						if (column.isRendered())
							addColumnValue(row, column.getChildren(), j);
					}
				}
				list.setRowIndex(-1);

			} else {
				HtmlDataTable table = (HtmlDataTable) component;
				columns = getColumnsToExport(table, null);
				first = pageOnly ? table.getFirst() : 0;
				size = pageOnly ? (first + table.getRows()) : table.getRowCount();

				addColumnHeaders(sheet, columns);
				int sheetRowIndex = 1;
				int numberOfColumns = columns.size();

				for (int i = first; i < size; i++) {
					table.setRowIndex(i);
					Row row = sheet.createRow(sheetRowIndex++);

					for (int j = 0; j < numberOfColumns; j++) {
						UIColumn column = columns.get(j);

						if (column.isRendered())
							addColumnValue(row, column.getChildren(), j);
					}
				}
				table.setRowIndex(-1);
			}
		}

		if (postProcessor != null) {
			postProcessor.invoke(context.getELContext(), new Object[] { wb });
		}

		writeExcelToResponse(((HttpServletResponse) context.getExternalContext().getResponse()), wb,
				filename);
	}

	private void addColumnHeaders(Sheet sheet, List<UIColumn> columns) {
		Row rowHeader = sheet.createRow(0);

		for (int i = 0; i < columns.size(); i++) {
			UIColumn column = (UIColumn) columns.get(i);

			if (column.isRendered())
				addColumnValue(rowHeader, column.getHeader(), i);
		}
	}

	private void addColumnValue(Row rowHeader, UIComponent component, int index) {
		Cell cell = rowHeader.createCell(index);
		String value = component == null ? "" : exportValue(FacesContext.getCurrentInstance(), component);

		cell.setCellValue(createRichTextString(value));
	}

	private void addColumnValue(Row rowHeader, List<UIComponent> components, int index) {
		Cell cell = rowHeader.createCell(index);
		StringBuilder builder = new StringBuilder();

		for (UIComponent component : components) {
			if (component.isRendered()) {
				String value = exportValue(FacesContext.getCurrentInstance(), component);

				if (value != null)
					builder.append(value);
			}
		}

		cell.setCellValue(createRichTextString(builder.toString()));
	}

	private void writeExcelToResponse(HttpServletResponse response, Workbook generatedExcel, String filename)
			throws IOException {
		response.setContentType(getContentType());
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		response.setHeader("Content-disposition", getContentDisposition(filename));

		generatedExcel.write(response.getOutputStream());
	}

	protected RichTextString createRichTextString(String value) {
		return new HSSFRichTextString(value);
	}

	protected Workbook createWorkBook() {
		return new HSSFWorkbook();
	}

	protected String getContentType() {
		return "application/vnd.ms-excel";
	}

	protected String getContentDisposition(String filename) {
		return "attachment;filename=" + filename + ".xls";
	}

	@Override
	public void customFormat(String facetBackground, String facetFontSize, String facetFontColor,
			String facetFontStyle, String fontName, String cellFontSize, String cellFontColor,
			String cellFontStyle, String datasetPadding, String orientation) throws IOException {
		// TODO Auto-generated method stub

	}
}