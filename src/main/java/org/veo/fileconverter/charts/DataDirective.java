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

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import freemarker.core.Environment;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

@Component
public class DataDirective implements TemplateDirectiveModel {

  public record ChartDataPoint(
      String label, Number value, String series, String color, String url) {
    public ChartDataPoint(String label, Number value, String series, String color, String url) {
      this.label = label;
      this.value = value;
      this.series = series != null ? series : "Default";
      this.color = color;
      this.url = url;
    }
  }

  @Override
  public void execute(
      Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
      throws TemplateException {

    String label = params.containsKey("label") ? params.get("label").toString() : "Unknown";
    String series = params.containsKey("series") ? params.get("series").toString() : null;
    String color = params.containsKey("color") ? params.get("color").toString() : null;
    String url = params.containsKey("url") ? params.get("url").toString() : null;
    Number value;

    if (params.get("value") instanceof SimpleScalar s) {
      value = Double.parseDouble(s.getAsString());
    } else {
      throw new TemplateModelException(
          "Unexpected type for number: " + params.get("value").getClass());
    }

    List<ChartDataPoint> dataList =
        (List<ChartDataPoint>) env.getCustomAttribute(ChartDirective.CHART_DATA_CONTEXT);

    if (dataList != null) {
      dataList.add(new ChartDataPoint(label, value, series, color, url));
    } else {
      throw new TemplateModelException(
          DataDirective.class.getSimpleName()
              + " must be used inside a "
              + ChartDirective.class.getSimpleName());
    }
  }
}
