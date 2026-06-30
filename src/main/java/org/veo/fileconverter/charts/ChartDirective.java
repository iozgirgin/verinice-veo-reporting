/*
 * verinice.veo reporting
 * Copyright (C) 2026  Jochen Kemnade
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.fileconverter.charts;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.batik.svggen.SVGIDGenerator;
import org.apache.batik.util.SVGConstants;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

@Component
public class ChartDirective implements TemplateDirectiveModel {

  public static final String CHART_DATA_CONTEXT = "chart_data_context";

  private static final double CSS_PX_PER_INCH = 96.0;
  private static final double CM_PER_INCH = 2.54;
  private static final double UNITS_PER_CM = CSS_PX_PER_INCH / CM_PER_INCH;

  @Override
  public void execute(
      Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException, IOException {

    String type =
        params.containsKey("type") ? params.get("type").toString().toLowerCase(Locale.ROOT) : "bar";
    String title = params.containsKey("title") ? params.get("title").toString() : "";
    String alt = params.containsKey("alt") ? params.get("title").toString() : "";
    String style = params.containsKey("style") ? params.get("style").toString() : "";
    double widthCM = cmParam(params, "width", 10);
    double heightCM = cmParam(params, "height", 8);

    List<DataDirective.ChartDataPoint> dataPoints = new ArrayList<>();
    env.setCustomAttribute(CHART_DATA_CONTEXT, dataPoints);

    if (body != null) {
      body.render(new StringWriter());
    }
    env.removeCustomAttribute(CHART_DATA_CONTEXT);

    JFreeChart chart;
    if ("pie".equals(type)) {
      final Map<String, String> urls = new HashMap<>();
      final Map<String, String> colors = new HashMap<>();

      DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
      for (DataDirective.ChartDataPoint pt : dataPoints) {
        dataset.setValue(pt.label(), pt.value());
        String url = pt.url();
        String color = pt.color();
        if (url != null && !url.isEmpty()) {
          urls.put(pt.label(), url);
        }
        if (color != null && !color.isEmpty()) {
          colors.put(pt.label(), color);
        }
      }
      chart = ChartUtils.createPieChart(title, dataset, urls, colors);
    } else {
      throw new IllegalArgumentException("Unsupported chart type: " + type);
    }

    @SuppressWarnings("PMD.CloseResource")
    var out = env.getOut();

    String objectTag =
        String.format(
            "<object type=\"jfreechart/veo-pie\" style=\"%s;width:%scm;height:%scm\" title=\"%s\" alt=\"%s\">",
            style, widthCM, heightCM, HtmlUtils.htmlEscape(title), HtmlUtils.htmlEscape(alt));

    out.write(objectTag);
    for (DataDirective.ChartDataPoint dp : dataPoints) {
      String dataTag =
          String.format(
              "<data name=\"%s\" color=\"%s\" value=\"%s\"></data>",
              dp.label(), dp.color(), dp.value());
      out.write(dataTag);
    }

    double uW = widthCM * UNITS_PER_CM;
    double uH = heightCM * UNITS_PER_CM;

    String svgElement = render(chart, uW, uH, widthCM + "cm", heightCM + "cm");

    out.write(svgElement);
    out.write("</object>");
  }

  public String render(JFreeChart chart, double uW, double uH, String cmW, String cmH)
      throws SVGGraphics2DIOException {

    Document doc =
        GenericDOMImplementation.getDOMImplementation()
            .createDocument(SVGConstants.SVG_NAMESPACE_URI, "svg", null);

    SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(doc);
    ctx.setPrecision(4);
    final String chartPrefix = "c" + System.nanoTime() + "-";
    ctx.setIDGenerator(
        new SVGIDGenerator() {
          @Override
          public String generateID(String prefix) {
            return chartPrefix + super.generateID(prefix);
          }
        });
    SVGGraphics2D g2 = new SVGGraphics2D(ctx, false);

    chart.draw(g2, new Rectangle2D.Double(0, 0, uW, uH));

    Element root = g2.getRoot();
    root.setAttributeNS(null, "viewBox", "0 0 " + uW + " " + uH);
    root.setAttributeNS(null, "width", cmW);
    root.setAttributeNS(null, "height", cmH);

    StringWriter sw = new StringWriter();
    g2.stream(root, sw, true, false);

    String out = sw.toString();
    return out.substring(out.indexOf("<svg"));
  }

  private static double cmParam(Map<?, ?> params, String name, double defaultValue)
      throws TemplateModelException {
    Object v = params.get(name);
    if (v == null) {
      return defaultValue;
    }
    if (v instanceof TemplateNumberModel n) {
      double d = n.getAsNumber().doubleValue();
      if (d <= 0) {
        throw new TemplateModelException(name + " must be positive");
      }
      return d;
    }
    throw new TemplateModelException(name + " must be a number in cm, e.g. width=10");
  }
}
