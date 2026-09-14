/*
 * verinice.veo reporting
 * Copyright (C) 2022  Jochen Kemnade
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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.data.general.DefaultPieDataset;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.openhtmltopdf.extend.FSObjectDrawer;
import com.openhtmltopdf.extend.OutputDevice;
import com.openhtmltopdf.render.RenderingContext;

public class VeoJFreeChartPieDiagramObjectDrawer implements FSObjectDrawer {

  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  private static Map<Shape, String> buildShapeLinkMap(
      ChartRenderingInfo renderingInfo, int dotsPerPixel) {
    Map<Shape, String> linkShapes = null;
    AffineTransform scaleTransform = new AffineTransform();
    scaleTransform.scale(dotsPerPixel, dotsPerPixel);
    for (Object entity : renderingInfo.getEntityCollection().getEntities()) {
      if (!(entity instanceof ChartEntity)) {
        continue;
      }
      ChartEntity chartEntity = (ChartEntity) entity;
      Shape shape = chartEntity.getArea();
      String url = chartEntity.getURLText();
      if (url != null) {
        if (linkShapes == null) {
          linkShapes = new HashMap<>();
        }
        linkShapes.put(scaleTransform.createTransformedShape(shape), url);
      }
    }
    return linkShapes;
  }

  @Override
  public Map<Shape, String> drawObject(
      Element e,
      final double x,
      final double y,
      final double width,
      final double height,
      OutputDevice outputDevice,
      RenderingContext ctx,
      final int dotsPerPixel) {
    DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
    NodeList childNodes = e.getChildNodes();
    final Map<String, String> urls = new HashMap<>();
    final Map<String, String> colors = new HashMap<>();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item = childNodes.item(i);
      if (!(item instanceof Element)) {
        continue;
      }
      Element childElement = (Element) item;
      String tagName = ((Element) item).getTagName();
      if (!"data".equals(tagName) && !"td".equals(tagName)) {
        continue;
      }
      String name = childElement.getAttribute("name");
      double value = Double.parseDouble(childElement.getAttribute("value"));
      String url = childElement.getAttribute("url");
      String color = childElement.getAttribute("color");
      dataset.setValue(name, value);
      if (!url.isEmpty()) {
        urls.put(name, url);
      }
      if (!color.isEmpty()) {
        colors.put(name, color);
      }
    }

    final JFreeChart chart1 =
        ChartUtils.createPieChart(e.getAttribute("title"), dataset, urls, colors);

    final ChartRenderingInfo renderingInfo = new ChartRenderingInfo();
    outputDevice.drawWithGraphics(
        (float) x,
        (float) y,
        (float) width / dotsPerPixel,
        (float) height / dotsPerPixel,
        graphics2D ->
            chart1.draw(
                graphics2D,
                new Rectangle2D.Float(
                    0, 0, (float) (width / dotsPerPixel), (float) (height / dotsPerPixel)),
                renderingInfo));

    return buildShapeLinkMap(renderingInfo, dotsPerPixel);
  }
}
