/*
 * verinice.veo reporting
 * Copyright (C) 2026  DataGood contributors
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
package org.veo.reporting;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.veo.reporting.CreateReport.TargetSpecification;
import org.veo.reporting.exception.InvalidReportParametersException;

/** Validate subtype restrictions against authorized source data before starting output. */
public final class ReportTargetValidator {
  private ReportTargetValidator() {}

  /** A named report context must match the authorized domain returned for the request. */
  public static void validateDomain(
      String requiredName, UUID requestedId, Map<String, Object> data) {
    if (requiredName == null) {
      return;
    }
    if (!(data.get("domain") instanceof Map<?, ?> domain)
        || !Objects.equals(String.valueOf(domain.get("id")), requestedId.toString())
        || !Objects.equals(domain.get("name"), requiredName)) {
      throw new InvalidReportParametersException(
          "Report is not available in the selected standard or regulation domain");
    }
  }

  public static void validate(
      Set<TypeSpecification> supported,
      TargetSpecification requested,
      UUID domainId,
      Map<String, Object> data) {
    // Reports without subtype constraints retain their existing provider contract.
    if (supported.stream()
        .anyMatch(
            spec ->
                spec.modelType() == requested.type()
                    && (spec.subTypes() == null || spec.subTypes().isEmpty()))) {
      return;
    }
    if (!(data.get("target") instanceof Map<?, ?> target)
        || !Objects.equals(String.valueOf(target.get("id")), requested.id().toString())
        || !Objects.equals(target.get("type"), requested.type().name().toLowerCase(Locale.ROOT))
        || !(target.get("domains") instanceof Map<?, ?> domains)
        || !(domains.get(domainId.toString()) instanceof Map<?, ?> association)
        || supported.stream()
            .noneMatch(
                spec ->
                    spec.modelType() == requested.type()
                        && spec.subTypes() != null
                        && spec.subTypes().contains(association.get("subType")))) {
      throw new InvalidReportParametersException(
          "Target does not match report subtype restrictions in the selected domain");
    }
  }
}
