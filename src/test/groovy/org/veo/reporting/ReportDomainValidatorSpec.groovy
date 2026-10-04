/*
 * verinice.veo reporting - Copyright (C) 2026 DataGood contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.veo.reporting

import groovy.json.JsonSlurper
import org.veo.reporting.exception.InvalidReportParametersException
import spock.lang.Specification

class ReportDomainValidatorSpec extends Specification {
    static def fixture() {
        new JsonSlurper().parse(ReportDomainValidatorSpec.getResourceAsStream('/datagood/report-domain-cases.json'))
    }

    def inputs(c) {
        def f = fixture()
        def data = c.missingDomain ? [:] : [domain: [id: c.actualId ?: f.domainId, name: c.actualName]]
        def requiredName = c.containsKey('requiredName') ? c.requiredName : f.requiredName
        [requiredName, UUID.fromString(f.domainId), data]
    }

    def 'named and unrestricted contexts are accepted #c.caseId'() {
        when:
        ReportTargetValidator.validateDomain(*inputs(c))
        then:
        noExceptionThrown()
        where:
        c << fixture().cases.findAll { it.expectedAllowed }
    }

    def 'wrong or missing domain context is rejected #c.caseId'() {
        when:
        ReportTargetValidator.validateDomain(*inputs(c))
        then:
        thrown(InvalidReportParametersException)
        where:
        c << fixture().cases.findAll { !it.expectedAllowed }
    }
}
