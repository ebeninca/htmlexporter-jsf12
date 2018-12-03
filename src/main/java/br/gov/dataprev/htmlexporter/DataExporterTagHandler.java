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

import javax.el.ELException;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.tag.TagAttribute;
import com.sun.facelets.tag.TagConfig;
import com.sun.facelets.tag.TagHandler;
import com.sun.facelets.tag.jsf.ComponentSupport;

public class DataExporterTagHandler extends TagHandler {

	private final TagAttribute target;
	private final TagAttribute type;
	private final TagAttribute fileName;
	private final TagAttribute tableTitle;
	private final TagAttribute pageOnly;
	private final TagAttribute preProcessor;
	private final TagAttribute postProcessor;
	private final TagAttribute encoding;
	private final TagAttribute facetBackground;
	private final TagAttribute facetFontSize;
	private final TagAttribute facetFontColor;
	private final TagAttribute facetFontStyle;
	private final TagAttribute fontName;
	private final TagAttribute cellFontSize;
	private final TagAttribute cellFontColor;
	private final TagAttribute cellFontStyle;
	private final TagAttribute datasetPadding;
	private final TagAttribute orientation;
	private final TagAttribute skipComponents;

	public DataExporterTagHandler(TagConfig tagConfig) {
		super(tagConfig);
		this.target = getRequiredAttribute("target");
		this.type = getRequiredAttribute("type");
		this.fileName = getAttribute("fileName");
		this.tableTitle = getAttribute("tableTitle");
		this.pageOnly = getAttribute("pageOnly");
		this.encoding = getAttribute("encoding");
		this.preProcessor = getAttribute("preProcessor");
		this.postProcessor = getAttribute("postProcessor");
		this.facetBackground = getAttribute("facetBackground");
		this.facetFontSize = getAttribute("facetFontSize");
		this.facetFontColor = getAttribute("facetFontColor");
		this.facetFontStyle = getAttribute("facetFontStyle");
		this.fontName = getAttribute("fontName");
		this.cellFontSize = getAttribute("cellFontSize");
		this.cellFontColor = getAttribute("cellFontColor");
		this.cellFontStyle = getAttribute("cellFontStyle");
		this.datasetPadding = getAttribute("datasetPadding");
		this.orientation = getAttribute("orientation");
		this.skipComponents = getAttribute("skipComponents");
	}

	public void apply(FaceletContext faceletContext, UIComponent parent)
			throws IOException, FacesException, FaceletException, ELException {
		if (ComponentSupport.isNew(parent)) {
			ValueExpression targetVE = target.getValueExpression(faceletContext, Object.class);
			ValueExpression typeVE = type.getValueExpression(faceletContext, Object.class);
			ValueExpression fileNameVE = null;
			ValueExpression tableTitleVE = null;
			ValueExpression pageOnlyVE = null;
			ValueExpression encodingVE = null;
			MethodExpression preProcessorME = null;
			MethodExpression postProcessorME = null;
			ValueExpression facetBackgroundVE = null;
			ValueExpression facetFontSizeVE = null;
			ValueExpression facetFontColorVE = null;
			ValueExpression facetFontStyleVE = null;
			ValueExpression fontNameVE = null;
			ValueExpression cellFontSizeVE = null;
			ValueExpression cellFontColorVE = null;
			ValueExpression cellFontStyleVE = null;
			ValueExpression datasetPaddingVE = null;
			ValueExpression orientationVE = null;
			ValueExpression skipComponentsVE = null;

			if (fileName != null) {
				fileNameVE = fileName.getValueExpression(faceletContext, Object.class);
			}
			if (tableTitle != null) {
				tableTitleVE = tableTitle.getValueExpression(faceletContext, Object.class);
			}
			if (encoding != null) {
				encodingVE = encoding.getValueExpression(faceletContext, Object.class);
			}
			if (pageOnly != null) {
				pageOnlyVE = pageOnly.getValueExpression(faceletContext, Object.class);
			}
			if (preProcessor != null) {
				preProcessorME = preProcessor.getMethodExpression(faceletContext, null,
						new Class[] { Object.class });
			}
			if (postProcessor != null) {
				postProcessorME = postProcessor.getMethodExpression(faceletContext, null,
						new Class[] { Object.class });
			}
			if (facetBackground != null) {
				facetBackgroundVE = facetBackground.getValueExpression(faceletContext, Object.class);
			}
			if (facetFontSize != null) {
				facetFontSizeVE = facetFontSize.getValueExpression(faceletContext, Object.class);
			}
			if (facetFontColor != null) {
				facetFontColorVE = facetFontColor.getValueExpression(faceletContext, Object.class);
			}
			if (facetFontStyle != null) {
				facetFontStyleVE = facetFontStyle.getValueExpression(faceletContext, Object.class);
			}
			if (fontName != null) {
				fontNameVE = fontName.getValueExpression(faceletContext, Object.class);
			}
			if (cellFontSize != null) {
				cellFontSizeVE = cellFontSize.getValueExpression(faceletContext, Object.class);
			}
			if (cellFontColor != null) {
				cellFontColorVE = cellFontColor.getValueExpression(faceletContext, Object.class);
			}
			if (cellFontStyle != null) {
				cellFontStyleVE = cellFontStyle.getValueExpression(faceletContext, Object.class);
			}
			if (datasetPadding != null) {
				datasetPaddingVE = datasetPadding.getValueExpression(faceletContext, Object.class);
			}
			if (orientation != null) {
				orientationVE = orientation.getValueExpression(faceletContext, Object.class);
			}
			if (skipComponents != null) {
				skipComponentsVE = skipComponents.getValueExpression(faceletContext, Object.class);
			}

			ActionSource actionSource = (ActionSource) parent;
			actionSource.addActionListener(new DataExporter(targetVE, typeVE, fileNameVE, tableTitleVE,
					pageOnlyVE, encodingVE, preProcessorME, postProcessorME, facetBackgroundVE,
					facetFontSizeVE, facetFontColorVE, facetFontStyleVE, fontNameVE, cellFontSizeVE,
					cellFontColorVE, cellFontStyleVE, datasetPaddingVE, orientationVE, skipComponentsVE));
		}
	}

}
