/*
 * verinice.veo reporting
 * Copyright (C) 2026 DataGood contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.veo.templating

import org.veo.reporting.ReportCreationParameters

import freemarker.cache.StringTemplateLoader
import groovy.json.JsonSlurper
import org.jsoup.Jsoup
import spock.lang.Specification

class DataGoodIsoRiskChartSpec extends Specification {

    def 'ISO summary chart labels agree with their inherent and residual data'() {
        given: 'the same mixed CIA scenario verified in the demo account'
        def fixture = new JsonSlurper().parse(getClass().getResourceAsStream('/datagood/iso-report-mixed.json'))
        def source = getClass().getResource('/templates/iso-risk-analysis.md').text
        def start = source.indexOf('<table class="risk_charts_container">')
        assert start >= 0
        def end = source.indexOf('</table>', start) + '</table>'.length()
        def loader = new StringTemplateLoader()
        // Render the production chart section with the real evaluator/directives.
        loader.putTemplate('production-charts.md', source.substring(start, end))
        def evaluator = new TemplateEvaluatorImpl(loader, false)
        def bundle = new PropertyResourceBundle(getClass().getResourceAsStream('/templates/iso-risk-analysis_en.properties'))
        def data = [domain: fixture.domain, riskDefinitionId: fixture.riskDefinitionId,
                    riskDefinition: fixture.riskDefinition,
                    risksInDomainWithData: fixture.risksInDomainWithData, bundle: bundle]
        def output = new ByteArrayOutputStream()

        when:
        evaluator.executeTemplate('production-charts.md', data, output,
                new ReportCreationParameters(Locale.ENGLISH, TimeZone.getTimeZone('UTC')))
        def charts = Jsoup.parse(output.toString('UTF-8')).select('object[type="jfreechart/veo-pie"]')

        then: 'each chart uses the correct label and category, not merely a translated word'
        charts.size() == 2
        fixture.expectedCharts.each { kind, expected ->
            def chart = charts.find { it.attr('title') == "Risk distribution (${kind})" }
            assert chart != null
            def points = chart.select('data').collectEntries {
                [(it.attr('name')): new BigDecimal(it.attr('value'))]
            }
            assert points == expected.collectEntries { name, count -> [(name): new BigDecimal(count.toString())] }
        }
    }
}
