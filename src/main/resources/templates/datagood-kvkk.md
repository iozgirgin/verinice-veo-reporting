<#-- DataGood-owned KVKK synthetic reporting adaptation. Copyright 2026 DataGood contributors. AGPL-3.0-or-later. -->
<#import "/libs/commons.md" as com>
<#if domain.name != 'KVKK (geliştirme)' || !target.domains[domain.id]??>
  <#stop "KVKK report requires its selected domain and a target in that domain">
</#if>
<#assign allowed = ['ProcessingActivity','DataSubjectRequest','ErasureRecord','TransferAssessment','PersonalDataBreach']>
<#if !allowed?seq_contains(target.domains[domain.id].subType)>
  <#stop "Unsupported KVKK report target subtype">
</#if>
<style>
<@com.defaultStyles/>
@page { @bottom-right { content: 'Sayfa ' counter(page) ' / ' counter(pages); } }
@page:first { @bottom-right { content: 'Sayfa ' counter(page) ' / ' counter(pages); } }
h1, h2, h3 { page-break-after: avoid; }
table.kvkk-record { width: 100%; table-layout: fixed; -fs-table-paginate: paginate; }
table.kvkk-record th:first-child, table.kvkk-record td:first-child { width: 35%; }
table.kvkk-record td { vertical-align: top; word-wrap: break-word; }
.record-section { page-break-before: always; }
.notice { font-size: 90%; }
</style>

<#function display value>
  <#if value?is_boolean><#return value?then(bundle.dg_yes,bundle.dg_no)></#if>
  <#if value?is_sequence><#return value?map(v->display(v))?join(', ')></#if>
  <#if value?is_number><#return value?c></#if>
  <#if value?is_string && bundle?keys?seq_contains(value)><#return bundle[value]></#if>
  <#return value?has_content?then(value,bundle.dg_missing)>
</#function>

<#macro field label value=''>
<tr><td>${label}</td><td>${display(value)}</td></tr>
</#macro>

<#macro aspects entity>
<#list (entity.domains[domain.id].customAspects!{})?keys?sort as aspect>
<#assign values = entity.domains[domain.id].customAspects[aspect]>
<#list values?keys?sort as fieldName>
<#assign label = bundle[aspect+'_'+fieldName]!bundle[fieldName]!fieldName>
<#assign value = values[fieldName]>
<#if fieldName == 'legalSource' && value?has_content>
<tr><td>${label}</td><td><a href="${value}">${bundle.dg_source_link}</a><br/>${value?replace('/', '/ ')}</td></tr>
<#else>
<@field label value/>
</#if>
</#list>
</#list>
</#macro>

<#macro record entity>
<section class="record-section">
<h2>${entity.name}</h2>
<table class="table kvkk-record">
<thead><tr><th>${bundle.dg_field}</th><th>${bundle.dg_value}</th></tr></thead>
<tbody>
<@field bundle.dg_id entity.id/>
<@field bundle.dg_status com.status(entity,domain)/>
<@field bundle.dg_description entity.description!''/>
<@aspects entity/>
</tbody>
</table>
<h3>${bundle.dg_relations}</h3>
<table class="table kvkk-record"><tbody>
<#list ['responsible','activities','relatedRequests','systems','evidenceDocuments','measures'] as relation>
<#assign linked = entity.findLinked(relation)>
<#if linked?has_content>
<@field bundle['dg_'+relation] linked?map(e->e.name)?join('; ')/>
</#if>
</#list>
</tbody></table>
<#list (entity.findLinked('evidenceDocuments') + entity.findLinked('measures'))?sort_by('name_naturalized') as evidence>
<h3>${evidence.name}</h3>
<table class="table kvkk-record"><tbody><@aspects evidence/></tbody></table>
</#list>
</section>
</#macro>

<h1>${bundle.dg_title}</h1>
<p class="notice">${bundle.dg_notice}</p>
<p>${bundle.dg_timestamp_notice}</p>
<table class="table kvkk-record"><tbody>
<@field bundle.dg_unit unit.name/>
<@field bundle.dg_domain domain.name/>
<@field bundle.dg_record target.name/>
<@field bundle.dg_report_version '1.0.0'/>
</tbody></table>

<#assign subType = target.domains[domain.id].subType>
<#assign related = []>
<#if subType == 'ProcessingActivity'>
<#assign candidates = processes + incidents>
<#assign related = candidates?filter(e->e.id != target.id && e.findLinked('activities')?map(a->a.id)?seq_contains(target.id))?sort_by('name_naturalized')>
<p>${bundle.dg_scope_notice}</p>
<p>${bundle.dg_related_count}: ${related?size?c}</p>
<#elseif subType == 'DataSubjectRequest'>
<#assign related = processes?filter(e->e.domains[domain.id].subType == 'ErasureRecord' && e.findLinked('relatedRequests')?map(r->r.id)?seq_contains(target.id))?sort_by('name_naturalized')>
<#elseif subType == 'ErasureRecord'>
<#assign related = target.findLinked('relatedRequests')?sort_by('name_naturalized')>
</#if>
<@record target/>
<#list related as entity><@record entity/></#list>
