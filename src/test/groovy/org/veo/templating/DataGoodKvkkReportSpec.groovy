/*
 * verinice.veo reporting - Copyright (C) 2026 DataGood contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.veo.templating

import freemarker.cache.ClassTemplateLoader
import groovy.json.JsonSlurper
import org.jsoup.Jsoup
import org.veo.reporting.ReportCreationParameters
import org.veo.reporting.MapResourceBundle
import spock.lang.Specification

class DataGoodKvkkReportSpec extends Specification {
    def fixture() {
        new JsonSlurper().parse(getClass().getResourceAsStream('/datagood/kvkk-report-cases.json'))
    }

    def render(f, key) {
        def data = f.data
        def entities = ['processes', 'incidents', 'assets', 'documents', 'controls', 'persons'].collectMany { data[it] }
        data.target = entities.find { it.id == f.targetIds[key] }
        def entries = new Properties()
        getClass().getResourceAsStream('/templates/datagood-kvkk_tr.properties').withCloseable {
            entries.load(new InputStreamReader(it, 'UTF-8'))
        }
        entities.each { e ->
            def association = e.domains[data.domain.id]
            entries[e.type + '_' + association.subType + '_status_' + association.status] = association.status
        }
        entries['ERASURE'] = 'Silme / imha talebi'
        data.bundle = new MapResourceBundle(entries as Map)
        def output = new ByteArrayOutputStream()
        new TemplateEvaluatorImpl(new ClassTemplateLoader(getClass(), '/templates'), false)
            .executeTemplate('datagood-kvkk.md', data, output,
                new ReportCreationParameters(Locale.forLanguageTag('tr'), TimeZone.getTimeZone('Europe/Istanbul')))
        output.toString('UTF-8')
    }

    def 'inventory includes exactly the four directly linked KVKK workflows'() {
        given:
        def f = fixture()
        when:
        def html = Jsoup.parse(render(f, 'inventory'))
        then:
        html.select('section.record-section').size() == 5
        html.text().contains('İlişkili iş akışı kaydı sayısı: 4')
        html.text().contains(f.expected.requestReceivedUtc)
        html.text().contains(f.expected.requestResponseDueUtc)
        !html.text().contains('TEST GDPR')
        !html.text().contains('TEST NIS2')
    }

    def 'request and erasure resolve each other and preserve evidence metadata'() {
        given:
        def f = fixture()
        when:
        def text = Jsoup.parse(render(f, key)).text()
        then:
        text.contains('TEST KVKK İlgili Kişi İmha Başvurusu')
        text.contains('TEST KVKK İmha İşlem Kaydı')
        text.contains('TEST KVKK Başvuru Cevap Kanıtı')
        text.contains('TEST-DOC-RESPONSE')
        text.contains('Uzman kabulü kaydı Hayır')
        text.contains('TEST-REQUEST-01')
        text.contains('Silme / imha talebi')
        where:
        key << ['request', 'erasure']
    }

    def 'missing and false fields remain distinct and markup is escaped'() {
        given:
        def f = fixture()
        def request = f.data.processes.find { it.id == f.targetIds.request }
        request.description = '<script>TEST unsafe markup</script>'
        request.domains[f.data.domain.id].customAspects.kvkkRequest.responseReason = ''
        when:
        def html = Jsoup.parse(render(f, 'request'))
        then:
        html.select('script').empty
        html.text().contains('<script>TEST unsafe markup</script>')
        html.text().contains('Cevap ve gerekçe özeti Kayıt yok')
        html.text().contains('Uzman kabulü kaydı Hayır')
    }

    def 'transfer and breach retain declared execution and affected count #key'() {
        given:
        def f = fixture()
        when:
        def text = Jsoup.parse(render(f, key)).text()
        then:
        text.contains(expectedCode)
        text.contains(expectedValue)
        text.contains('Uzman kabulü kaydı Hayır')
        where:
        key        | expectedCode       | expectedValue
        'transfer' | 'TEST-TRANSFER-01'  | 'Hayır'
        'breach'   | 'TEST-BREACH-01'    | '40'
    }

    def 'wrong standard context cannot render a KVKK report'() {
        given:
        def f = fixture()
        f.data.domain.name = 'GDPR (geliştirme)'
        when:
        render(f, 'request')
        then:
        thrown(freemarker.core.StopException)
    }
}
