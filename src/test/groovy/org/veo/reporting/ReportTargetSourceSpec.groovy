/*
 * verinice.veo reporting - Copyright (C) 2026 DataGood contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.veo.reporting

import groovy.json.JsonSlurper
import org.springframework.http.client.ClientHttpRequestFactory
import org.veo.reporting.exception.DataFetchingException
import spock.lang.Specification

class ReportTargetSourceSpec extends Specification {
    def graph() {
        new JsonSlurper().parse(getClass().getResourceAsStream('/datagood/kvkk-report-cases.json'))
    }

    def client(f) {
        new VeoClientImpl(Mock(ClientHttpRequestFactory), 'http://localhost:8783', false) {
            Object fetchData(String path, String authorization, String accept) {
                if (path.startsWith('/units/')) {
                    return [unit: f.data.unit, risks: [], elements:
                        ['processes', 'incidents', 'assets', 'controls', 'documents', 'persons'].collectMany { f.data[it] }]
                }
                f.data.domain
            }
        }
    }

    def 'selected target resolves in its own canonical domain'() {
        given:
        def f = graph()
        when:
        def result = client(f).fetchData(UUID.fromString('10000000-0000-4000-8000-000000000001'),
            UUID.fromString(f.data.domain.id), UUID.fromString(f.targetIds.request), 'TEST authorization')
        then:
        result.target.id == f.targetIds.request
    }

    def 'unresolvable target is a not-found response #kind'() {
        given:
        def f = graph()
        def domainId = kind == 'other-domain' ? '30000000-0000-4000-8000-000000000099' : f.data.domain.id
        def targetId = kind == 'missing-id' ? '20000000-0000-4000-8000-000000000099' : f.targetIds.request
        when:
        client(f).fetchData(UUID.fromString('10000000-0000-4000-8000-000000000001'),
            UUID.fromString(domainId), UUID.fromString(targetId), 'TEST authorization')
        then:
        def error = thrown(DataFetchingException)
        error.statusCode == 404
        where:
        kind << ['other-domain', 'missing-id']
    }
}
