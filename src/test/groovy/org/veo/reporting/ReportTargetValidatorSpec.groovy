/*
 * verinice.veo reporting - Copyright (C) 2026 DataGood contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.veo.reporting

import groovy.json.JsonSlurper
import org.veo.reporting.exception.InvalidReportParametersException
import spock.lang.Specification

class ReportTargetValidatorSpec extends Specification {
    static def fixture() {
        new JsonSlurper().parse(ReportTargetValidatorSpec.getResourceAsStream('/datagood/report-subtype-cases.json'))
    }

    def inputs(c) {
        def f = fixture()
        def graph = new JsonSlurper().parse(getClass().getResourceAsStream('/datagood/kvkk-report-cases.json'))
        def target = ['processes', 'incidents'].collectMany { graph.data[it] }.find { it.id == graph.targetIds[c.target] }
        def supported = f.reports[c.report].collect {
            new TypeSpecification(EntityType.valueOf(it.modelType.toUpperCase()), it.subTypes as Set)
        } as Set
        def request = new CreateReport.TargetSpecification(
            EntityType.valueOf((c.requestModel ?: target.type).toUpperCase()),
            UUID.fromString(c.requestId ?: target.id))
        def domain = UUID.fromString(c.selectedDomain ?: graph.data.domain.id)
        def data = [target: target]
        if (c.missingTarget) data.remove('target')
        if (c.missingAssociation) target.domains = [:]
        [supported, request, domain, data]
    }

    def 'allowed canonical target #c.caseId is accepted'() {
        given:
        def args = inputs(c)
        when:
        ReportTargetValidator.validate(*args)
        then:
        noExceptionThrown()
        where:
        c << fixture().cases.findAll { it.expectedAllowed }
    }

    def 'incompatible or malformed target #c.caseId is rejected'() {
        given:
        def args = inputs(c)
        when:
        ReportTargetValidator.validate(*args)
        then:
        thrown(InvalidReportParametersException)
        where:
        c << fixture().cases.findAll { !it.expectedAllowed }
    }

    def 'unrestricted legacy providers remain supported #subTypes'() {
        when:
        ReportTargetValidator.validate([new TypeSpecification(EntityType.PERSON, subTypes)] as Set,
            new CreateReport.TargetSpecification(EntityType.PERSON, UUID.fromString('20000000-0000-4000-8000-000000000001')),
            UUID.fromString('30000000-0000-4000-8000-000000000001'), [person: [name: 'TEST Person']])
        then:
        noExceptionThrown()
        where:
        subTypes << [null, [] as Set]
    }
}
