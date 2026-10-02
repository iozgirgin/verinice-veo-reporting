/*
 * verinice.veo reporting - Copyright (C) 2026 DataGood contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.veo.templating

import freemarker.cache.StringTemplateLoader
import groovy.json.JsonSlurper
import org.veo.reporting.ReportCreationParameters
import spock.lang.Specification

class DataGoodRiskTreatmentLabelSpec extends Specification {
    def 'production risk treatment labels follow report language #language'() {
        given:
        def fixture = new JsonSlurper().parse(getClass().getResourceAsStream('/datagood/risk-treatment-labels.json'))
        def source = getClass().getResource('/templates/libs/risk-commons.md').text
        def start = source.indexOf('<#function riskReductionLabel')
        def end = source.indexOf('</#function>', start) + '</#function>'.length()
        def loader = new StringTemplateLoader()
        loader.putTemplate('labels.md', source.substring(start, end) + '<#list treatments as t>${riskReductionLabel(t)}|</#list>')
        def output = new ByteArrayOutputStream()

        when:
        new TemplateEvaluatorImpl(loader, false).executeTemplate('labels.md', [treatments: fixture.treatments], output,
                new ReportCreationParameters(Locale.forLanguageTag(language), TimeZone.getTimeZone('UTC')))

        then:
        org.jsoup.Jsoup.parse(output.toString('UTF-8')).text().trim().split('\\|').toList() == fixture.expected[language]

        where:
        language << ['en', 'de']
    }
}
