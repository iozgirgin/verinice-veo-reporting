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

import java.awt.Color;
import java.awt.Font;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jspecify.annotations.NonNull;

import com.helger.collection.commons.ICommonsOrderedSet;
import com.helger.font.api.FontResourceManager;
import com.helger.font.api.IFontResource;

import org.veo.reporting.exception.VeoReportingException;

public final class ChartUtils {

  private static final Pattern PATTERN_RGB =
      Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");

  private static final Pattern PATTERN_HTML =
      Pattern.compile("#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})", Pattern.CASE_INSENSITIVE);

  public static final Color DEFAULT_FONT_COLOR = Color.decode("#767676");
  public static final Color DEFAULT_LABEL_BACKGROUND = new Color(255, 255, 255, 130);

  private static final Font OPEN_SANS_REGULAR_12;
  private static final Font OPEN_SANS_BOLD_20;

  static {
    var openSansFontResources = FontResourceManager.getAllResourcesOfFontType("Open Sans");
    var openSansRegular = loadFont(openSansFontResources, 400);
    var openSansBold = loadFont(openSansFontResources, 700);
    OPEN_SANS_REGULAR_12 = openSansRegular.deriveFont(Font.PLAIN, 12f);
    OPEN_SANS_BOLD_20 = openSansBold.deriveFont(Font.BOLD, 20f);
  }

  private static Font loadFont(
      ICommonsOrderedSet<IFontResource> openSansFontResources, int weight) {
    IFontResource resource =
        openSansFontResources.findFirst(
            f -> f.getFontWeight().getWeight() == weight && f.getFontStyle().isRegular());
    try (InputStream is =
        Objects.requireNonNull(resource, "Failed to resolve font").getBufferedInputStream()) {
      return Font.createFont(Font.TRUETYPE_FONT, is);
    } catch (Exception e1) {
      throw new VeoReportingException("Error initializing chart font", e1);
    }
  }

  public static Color parseColor(String input) {
    Matcher m = PATTERN_RGB.matcher(input);
    if (m.matches()) {
      return new Color(
          Integer.parseInt(m.group(1)), // r
          Integer.parseInt(m.group(2)), // g
          Integer.parseInt(m.group(3))); // b
    }
    m = PATTERN_HTML.matcher(input);
    if (m.matches()) {
      return new Color(
          Integer.parseInt(m.group(1), 16), // r
          Integer.parseInt(m.group(2), 16), // g
          Integer.parseInt(m.group(3), 16)); // b
    }
    return null;
  }

  public static @NonNull JFreeChart createPieChart(
      String title,
      DefaultPieDataset<String> dataset,
      Map<String, String> urls,
      Map<String, String> colors) {
    JFreeChart chart;
    chart = ChartFactory.createPieChart(title, dataset, true, false, true);
    PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
    plot.setBackgroundPaint(null);
    plot.setURLGenerator((_, key, _) -> urls.get(key.toString()));
    plot.setShadowPaint(null);
    plot.setShadowGenerator(null);
    plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}\n{1} ({2})"));
    plot.setLegendLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {2}"));

    plot.setLabelOutlinePaint(null);
    plot.setLabelBackgroundPaint(DEFAULT_LABEL_BACKGROUND);
    plot.setLabelLinkStyle(PieLabelLinkStyle.QUAD_CURVE);
    plot.setLabelShadowPaint(null);

    plot.setOutlinePaint(DEFAULT_FONT_COLOR);
    plot.setLabelPaint(DEFAULT_FONT_COLOR);
    chart.getLegend().setItemPaint(DEFAULT_FONT_COLOR);

    chart.getTitle().setPaint(DEFAULT_FONT_COLOR);
    chart.getTitle().setFont(OPEN_SANS_BOLD_20);
    chart.getLegend().setItemFont(OPEN_SANS_REGULAR_12);
    plot.setLabelFont(OPEN_SANS_REGULAR_12);

    colors.forEach((key, value) -> plot.setSectionPaint(key, ChartUtils.parseColor(value)));
    return chart;
  }

  private ChartUtils() {}
}
