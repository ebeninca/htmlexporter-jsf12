/**
 * Copyright 2011-2018 PrimeFaces Extensions
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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.component.UIColumn;
import javax.faces.component.UIComponent;
import javax.faces.component.UIPanel;
import javax.faces.component.html.HtmlCommandButton;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.myfaces.custom.datalist.HtmlDataList;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.html.HtmlTags;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.html.simpleparser.StyleSheet;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * <code>Exporter</code> component.
 *
 * 
 */
public class PDFExporter extends Exporter {

	private Font cellFont;
	private Font facetFont;
	private Color facetBackground;
	private Float facetFontSize;
	private Color facetFontColor;
	private String facetFontStyle;
	private String fontName;
	private Float cellFontSize;
	private Color cellFontColor;
	private String cellFontStyle;
	private int datasetPadding;
	private String orientation;

	@Override
	public void export(final ActionEvent event, final String tableId, final FacesContext context,
			final String filename, final String tableTitle, final boolean pageOnly, final String encodingType,
			final MethodExpression preProcessor, final MethodExpression postProcessor) throws IOException {
		try {
			final Document document = new Document();
			if (orientation.equalsIgnoreCase("Landscape")) {
				document.setPageSize(PageSize.A4.rotate());
			}
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PdfWriter.getInstance(document, baos);

			if (preProcessor != null) {
				preProcessor.invoke(context.getELContext(), new Object[] { document });
			}

			final StringTokenizer st = new StringTokenizer(tableId, ",");
			while (st.hasMoreElements()) {
				final String tableName = (String) st.nextElement();
				final UIComponent component = event.getComponent().findComponent(tableName);
				if (component == null) {
					throw new FacesException("Cannot find component \"" + tableName + "\" in view.");
				}
				if (!(component instanceof HtmlDataTable || component instanceof HtmlDataList)) {
					throw new FacesException(
							"Unsupported datasource target:\"" + component.getClass().getName()
									+ "\", exporter must target a PrimeFaces HtmlDataTable/HtmlDataList.");
				}

				if (!document.isOpen()) {
					document.open();
				}

				// CSS
/*
				StyleSheet css = new StyleSheet();
				css.loadTagStyle(HtmlTags.ROW, HtmlTags.BORDERCOLOR, "red");
				css.loadTagStyle(HtmlTags.ROW, HtmlTags.BORDERWIDTH, "1");
				css.loadTagStyle(HtmlTags.CELL, "style", "border: 1px solid #0000ff");
				css.loadTagStyle(HtmlTags.CELL, HtmlTags.BACKGROUNDCOLOR, "#f0f0f0");
				css.loadTagStyle(HtmlTags.CELL, "color", "#00ccff");

				css.loadTagStyle(HtmlTags.BODY, "size", "12pt");
				css.loadTagStyle(HtmlTags.BODY, "face", "Times");

				HTMLWorker htmlWorker = new HTMLWorker(document);
				htmlWorker.startDocument();
				
				htmlWorker.setStyleSheet(css);

				htmlWorker.endDocument();
*/
				if (tableTitle != null && !tableTitle.isEmpty() && !tableId.contains("" + ",")) {

					final Font tableTitleFont = FontFactory.getFont(FontFactory.TIMES, encodingType,
							Font.DEFAULTSIZE, Font.BOLD);
					final Paragraph title = new Paragraph(tableTitle, tableTitleFont);
					document.add(title);

					final Paragraph preface = new Paragraph();
					addEmptyLine(preface, 3);
					document.add(preface);
				}
				PdfPTable pdf;
				if (component instanceof HtmlDataList) {
					HtmlDataList list = (HtmlDataList) component;
					pdf = exportPDFTable(context, list, pageOnly, encodingType);
				} else {
					HtmlDataTable table = (HtmlDataTable) component;
					pdf = exportPDFTable(context, table, pageOnly, encodingType);
				}

				if (pdf != null) {
					document.add(pdf);
				}
				// add a couple of blank lines
				final Paragraph preface = new Paragraph();
				addEmptyLine(preface, datasetPadding);
				document.add(preface);
			}

			if (postProcessor != null) {
				postProcessor.invoke(context.getELContext(), new Object[] { document });
			}

			document.close();

			writePDFToResponse(context.getExternalContext(), baos, filename);

		} catch (final DocumentException e) {
			throw new IOException(e.getMessage());
		}
	}

	protected PdfPTable exportPDFTable(final FacesContext context, final HtmlDataTable table,
			final boolean pageOnly, final String encoding) {
		if (!"-".equalsIgnoreCase(encoding)) {
			createCustomFonts(encoding);
		}
		final int columnsCount = getColumnsCount(table);
		PdfPTable pdfTable = null;

		if (columnsCount == 0) {
			return null;
		}

		pdfTable = new PdfPTable(columnsCount);

		if (table.getHeader() != null) {
			tableFacet(context, pdfTable, table, columnsCount, "header");
		}
		if (hasHeaderColumn(table)) {
			addColumnFacets(table, pdfTable, ColumnType.HEADER);
		}

		if (pageOnly) {
			exportPageOnly(context, table, pdfTable);
		} else {
			exportAll(context, table, pdfTable);
		}

		if (hasFooterColumn(table)) {
			addColumnFacets(table, pdfTable, ColumnType.FOOTER);
		}
		if (table.getFooter() != null) {
			tableFacet(context, pdfTable, table, columnsCount, "footer");
		}

		table.setRowIndex(-1);

		return pdfTable;

	}

	protected PdfPTable exportPDFTable(final FacesContext context, final HtmlDataList list,
			final boolean pageOnly, final String encoding) {

		if (!"-".equalsIgnoreCase(encoding)) {
			createCustomFonts(encoding);
		}
		final int first = list.getFirst();
		final int rowCount = list.getRowCount();
		final int rowsToExport = first + list.getRows();

		final PdfPTable pdfTable = new PdfPTable(1);
		if (list.getHeader() != null) {
			final String value = exportValue(FacesContext.getCurrentInstance(), list.getHeader());
			final PdfPCell cell = new PdfPCell(new Paragraph(value, facetFont));
			if (facetBackground != null) {
				cell.setBackgroundColor(facetBackground);
			}
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			pdfTable.addCell(cell);
			pdfTable.completeRow();

		}

		final StringBuilder builder = new StringBuilder();
		String output = null;

		if (pageOnly) {
			output = exportPageOnly(first, list, rowsToExport, builder);
		} else {
			output = exportAll(list, rowCount, builder);
		}

		pdfTable.addCell(new Paragraph(output, cellFont));
		pdfTable.completeRow();

		if (list.getFooter() != null) {
			final String value = exportValue(FacesContext.getCurrentInstance(), list.getFooter());
			final PdfPCell cell = new PdfPCell(new Paragraph(value, facetFont));
			if (facetBackground != null) {
				cell.setBackgroundColor(facetBackground);
			}
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			pdfTable.addCell(cell);
			pdfTable.completeRow();

		}

		return pdfTable;
	}

	protected void exportPageOnly(final FacesContext context, final HtmlDataTable table,
			final PdfPTable pdfTable) {
		final int first = table.getFirst();
		final int rowsToExport = first + table.getRows();

		tableColumnGroup(pdfTable, table, "header");

		for (int rowIndex = first; rowIndex < rowsToExport; rowIndex++) {
			exportRow(table, pdfTable, rowIndex);
		}

		tableColumnGroup(pdfTable, table, "footer");
	}

	protected String exportPageOnly(final int first, final HtmlDataList list, final int rowsToExport,
			final StringBuilder input) {
		String output = "";
		for (int rowIndex = first; rowIndex < rowsToExport; rowIndex++) {
			output = addColumnValues(list, input);
		}
		return output;

	}

	protected void exportAll(final FacesContext context, final HtmlDataTable table,
			final PdfPTable pdfTable) {
		final int first = table.getFirst();
		final int rowCount = table.getRowCount();

		tableColumnGroup(pdfTable, table, "header");
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			exportRow(table, pdfTable, rowIndex);
		}
		tableColumnGroup(pdfTable, table, "footer");
		// restore
		table.setFirst(first);
	}

	protected String exportAll(final HtmlDataList list, final int rowCount, final StringBuilder input) {
		String output = "";
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
			list.setRowIndex(rowIndex);
			output = addColumnValues(list, input);
		}

		return output;
	}

	protected void tableFacet(final FacesContext context, final PdfPTable pdfTable, final HtmlDataTable table,
			final int columnCount, final String facetType) {
		final Map<String, UIComponent> map = table.getFacets();
		final UIComponent component = map.get(facetType);
		if (component != null) {
			String headerValue = null;
			if (component instanceof HtmlCommandButton) {
				headerValue = exportValue(context, component);
			} else if (component instanceof HtmlCommandLink) {
				headerValue = exportValue(context, component);
			} else if (component instanceof UIPanel) {
				final StringBuilder header = new StringBuilder("");
				for (final UIComponent child : component.getChildren()) {
					headerValue = exportValue(context, child);
					header.append(headerValue);
				}
				final PdfPCell cell = new PdfPCell(new Paragraph(header.toString(), facetFont));
				if (facetBackground != null) {
					cell.setBackgroundColor(facetBackground);
				}
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				// addColumnAlignments(component,cell);
				cell.setColspan(columnCount);
				pdfTable.addCell(cell);
				pdfTable.completeRow();
				return;
			} else {
				headerValue = exportFacetValue(context, component);
			}
			final PdfPCell cell = new PdfPCell(new Paragraph(headerValue, facetFont));
			if (facetBackground != null) {
				cell.setBackgroundColor(facetBackground);
			}
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			// addColumnAlignments(component,cell);
			cell.setColspan(columnCount);
			pdfTable.addCell(cell);
			pdfTable.completeRow();

		}
	}

	protected void tableColumnGroup(final PdfPTable pdfTable, final HtmlDataTable table,
			final String facetType) {
		/*
		 * final ColumnGroup cg = table.getColumnGroup(facetType);
		 * List<UIComponent> headerComponentList = null; if (cg != null) {
		 * headerComponentList = cg.getChildren(); } if (headerComponentList !=
		 * null) { for (final UIComponent component : headerComponentList) { if
		 * (component instanceof Row) { final Row row = (Row) component; for
		 * (final UIComponent rowComponent : row.getChildren()) { final UIColumn
		 * column = (UIColumn) rowComponent; String value = null; if
		 * (column.isRendered() && column.isExportable()) { if
		 * (facetType.equalsIgnoreCase("header")) { value =
		 * column.getHeaderText(); } else { value = column.getFooterText(); }
		 * final int rowSpan = column.getRowspan(); final int colSpan =
		 * column.getColspan(); final PdfPCell cell = new PdfPCell(new
		 * Paragraph(value, facetFont)); if (facetBackground != null) {
		 * cell.setBackgroundColor(facetBackground); } if (rowSpan > 1) {
		 * cell.setVerticalAlignment(Element.ALIGN_CENTER);
		 * cell.setRowspan(rowSpan);
		 * 
		 * } if (colSpan > 1) {
		 * cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		 * cell.setColspan(colSpan);
		 * 
		 * } // addColumnAlignments(component,cell); if
		 * (facetType.equalsIgnoreCase("header")) {
		 * cell.setHorizontalAlignment(Element.ALIGN_CENTER); }
		 * pdfTable.addCell(cell); } } }
		 * 
		 * } } pdfTable.completeRow();
		 */

	}

	protected void exportRow(final HtmlDataTable table, final PdfPTable pdfTable, final int rowIndex) {
		table.setRowIndex(rowIndex);

		if (!table.isRowAvailable()) {
			return;
		}

		exportCells(table, pdfTable);
	}

	protected void exportCells(final HtmlDataTable table, final PdfPTable pdfTable) {
		List<UIComponent> comp = table.getChildren();
		for (UIComponent uiComponent : comp) {

			if (uiComponent instanceof UIColumn) {

				UIColumn col = (UIColumn) uiComponent;
				if (col.isRendered()) {
					/*
					 * if (col.getSelectionMode() != null) {
					 * pdfTable.addCell(new Paragraph(col.getSelectionMode(),
					 * cellFont)); continue; }
					 */
					addColumnValue(pdfTable, col.getChildren(), cellFont, "data", col);
				}
			}
		}
		pdfTable.completeRow();
	}

	protected void addColumnFacets(final HtmlDataTable table, final PdfPTable pdfTable,
			final ColumnType columnType) {

		List<UIComponent> comp = table.getChildren();
		for (UIComponent uiComponent : comp) {

			if (uiComponent instanceof UIColumn) {

				UIColumn col = (UIColumn) uiComponent;
				PdfPCell cell = null;
				if (col.isRendered()) {
					if (col.getFacet("header") != null && columnType.name().equalsIgnoreCase("header")) {
						cell = new PdfPCell(
								new Paragraph(getFacetText(col.getFacetsAndChildren()), facetFont));
						if (facetBackground != null) {
							cell.setBackgroundColor(facetBackground);
						}
						cell.setHorizontalAlignment(Element.ALIGN_CENTER);
						pdfTable.addCell(cell);
					} else if (col.getFacet("footer") != null
							&& columnType.name().equalsIgnoreCase("footer")) {
						cell = new PdfPCell(
								new Paragraph(getFacetText(col.getFacetsAndChildren()), facetFont));
						if (facetBackground != null) {
							cell.setBackgroundColor(facetBackground);
						}
						pdfTable.addCell(cell);
					} else {
						addColumnValue(pdfTable, col.getFacet(columnType.facet()), facetFont,
								columnType.name());
					}
				}
			}
		}
	}

	protected void addColumnValue(final PdfPTable pdfTable, final UIComponent component, final Font font,
			final String columnType) {
		final String value = component == null ? ""
				: exportValue(FacesContext.getCurrentInstance(), component);
		PdfPCell cell = new PdfPCell(new Paragraph(value, font));

		if (facetBackground != null) {
			cell.setBackgroundColor(facetBackground);
		}
		if (columnType.equalsIgnoreCase("header")) {
			cell = addFacetAlignments(component, cell);
		} else {
			cell = addColumnAlignments(component, cell);
		}
		pdfTable.addCell(cell);
	}

	protected void addColumnValue(final PdfPTable pdfTable, final List<UIComponent> components,
			final Font font, final String columnType, final UIColumn column) {
		final FacesContext context = FacesContext.getCurrentInstance();
		PdfPCell cell = null;

		final StringBuilder builder = new StringBuilder();
		for (final UIComponent component : components) {
			if (component.isRendered()) {
				final String value = exportValue(context, component);

				if (value != null) {
					builder.append(value);
				}
			}
		}
		cell = new PdfPCell(new Paragraph(builder.toString(), font));
		for (final UIComponent component : components) {
			cell = addColumnAlignments(component, cell);
		}
		if (columnType.equalsIgnoreCase("header")) {
			for (final UIComponent component : components) {
				cell = addFacetAlignments(component, cell);
			}
		}

		if (cell != null) {
			pdfTable.addCell(cell);
		}
	}

	protected PdfPCell addColumnAlignments(final UIComponent component, final PdfPCell cell) {
		if (component instanceof HtmlOutputText) {
			final HtmlOutputText output = (HtmlOutputText) component;
			if (output.getStyle() != null && output.getStyle().contains("left")) {
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
			}
			if (output.getStyle() != null && output.getStyle().contains("right")) {
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			}
			if (output.getStyle() != null && output.getStyle().contains("center")) {
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			}
		}
		return cell;
	}

	protected PdfPCell addFacetAlignments(final UIComponent component, final PdfPCell cell) {
		if (component instanceof HtmlOutputText) {
			final HtmlOutputText output = (HtmlOutputText) component;
			if (output.getStyle() != null && output.getStyle().contains("left")) {
				cell.setHorizontalAlignment(Element.ALIGN_LEFT);
			} else if (output.getStyle() != null && output.getStyle().contains("right")) {
				cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			} else {
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			}
		}
		return cell;
	}

	@Override
	public void customFormat(final String facetBackground, final String facetFontSize,
			final String facetFontColor, final String facetFontStyle, final String fontName,
			final String cellFontSize, final String cellFontColor, final String cellFontStyle,
			final String datasetPadding, final String orientation) {

		this.facetFontSize = new Float(facetFontSize);
		this.cellFontSize = new Float(cellFontSize);
		this.datasetPadding = Integer.parseInt(datasetPadding);
		this.orientation = orientation;

		if (facetBackground != null) {
			this.facetBackground = Color.decode(facetBackground);
		}
		if (facetFontColor != null) {
			this.facetFontColor = Color.decode(facetFontColor);
		}
		if (cellFontColor != null) {
			this.cellFontColor = Color.decode(cellFontColor);
		}
		if (fontName != null) {
			this.fontName = fontName;
		}
		if (facetFontStyle.equalsIgnoreCase("NORMAL")) {
			this.facetFontStyle = "" + Font.NORMAL;
		}
		if (facetFontStyle.equalsIgnoreCase("BOLD")) {
			this.facetFontStyle = "" + Font.BOLD;
		}
		if (facetFontStyle.equalsIgnoreCase("ITALIC")) {
			this.facetFontStyle = "" + Font.ITALIC;
		}

		if (cellFontStyle.equalsIgnoreCase("NORMAL")) {
			this.cellFontStyle = "" + Font.NORMAL;
		}
		if (cellFontStyle.equalsIgnoreCase("BOLD")) {
			this.cellFontStyle = "" + Font.BOLD;
		}
		if (cellFontStyle.equalsIgnoreCase("ITALIC")) {
			this.cellFontStyle = "" + Font.ITALIC;
		}

	}

	protected void createCustomFonts(final String encoding) {

		if (fontName != null && FontFactory.getFont(fontName).getBaseFont() != null) {
			cellFont = FontFactory.getFont(fontName, encoding);
			facetFont = FontFactory.getFont(fontName, encoding, Font.DEFAULTSIZE, Font.BOLD);
		} else {
			cellFont = FontFactory.getFont(FontFactory.TIMES, encoding);
			facetFont = FontFactory.getFont(FontFactory.TIMES, encoding, Font.DEFAULTSIZE, Font.BOLD);
		}
		if (facetFontColor != null) {
			facetFont.setColor(facetFontColor);
		}
		if (facetFontSize != null) {
			facetFont.setSize(facetFontSize);
		}
		if (facetFontStyle != null) {
			facetFont.setStyle(facetFontStyle);
		}
		if (cellFontColor != null) {
			cellFont.setColor(cellFontColor);
		}
		if (cellFontSize != null) {
			cellFont.setSize(cellFontSize);
		}
		if (cellFontStyle != null) {
			cellFont.setStyle(cellFontStyle);
		}
	}

	private static void addEmptyLine(final Paragraph paragraph, final int number) {
		for (int i = 0; i < number; i++) {
			paragraph.add(new Paragraph(" "));
		}
	}

	protected void writePDFToResponse(final ExternalContext externalContext, final ByteArrayOutputStream baos,
			final String fileName) throws IOException, DocumentException {

		HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

		response.setContentType("application/pdf");
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".pdf");
		// externalContext.addResponseCookie(Constants.DOWNLOAD_COOKIE, "true",
		// Collections.<String, Object> emptyMap());
		response.setContentLength(baos.size());

		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();

	}

}
