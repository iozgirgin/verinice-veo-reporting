<#-- DataGood synthetic SoA adaptation of SerNet iso-soa.md; AGPL-3.0-or-later. -->
<#import "/libs/commons.md" as com>
<#import "/libs/risk-commons.md" as rcom>

<#assign table = com.table
  multiline = com.multiline
  riskCell = rcom.riskCell/>

<style>
<@com.defaultStyles true/>
<#if .lang == 'tr'>
@page { @bottom-right { content: 'Sayfa ' counter(page) ' / ' counter(pages); } }
</#if>

h1, h2, h3, h4 {
  page-break-after: avoid;
}

td {
  vertical-align: top;
}

.main_page {
  page-break-after: always;
}

.main_page table th:first-child, .main_page table td:first-child {
  width: 8cm;
}

table.control_list {
  -fs-table-paginate: paginate;
  font-size: 85%;
}

table.control_list th:first-child, table.control_list td:first-child {
  width: 1cm;
}

.fullwidth {
  width: 100%;
}
</style>

<#assign scope = target/>

<div class="footer-left">
  <table>
    <tr>
      <td>${bundle.scope_SCP_isoScope_singular}: </td>
      <td>${scope.name}</td>
    </tr>
    <tr>
      <td>${bundle.creation_date}: </td>
      <td>${.now?date}</td>
    </tr>
  </table>
</div>

<div class="cover">
<h1><@multiline bundle.title/></h1>
<p>powered by verinice</p>
</div>

# ${bundle.main_page} {#main_page}

<div class="main_page">

<@table bundle.scope_SCP_isoScope_singular,
scope,
['name',
'description',
'status'
],
domain/>

</div>

# ${bundle.control_CTL_ISOControl_plural}

<#function sortCIs cis>
<#assign sortedControls = cis?map(it->it.control)?sort_by('abbreviation_naturalized')>
<#return sortedControls?map(it->cis?filter(ci->ci.control._self == it._self)?first)>
</#function>

<#-- TODO: #3385: use domain-specific status -->
<#assign statusMap = {
'YES': {"color":"#12AE0F"},
'NO': {"color":"#AE0D11"},
'PARTIAL': {"color":"#EDE92F"},
'N_A': {"color":"#49A2ED"},
'UNKNOWN': {"color": "#ffffff"}
} />


<#macro sq color="#767676">
<div style="background-color:${color};width:1em;height:1em;"></div>
</#macro>

<#assign cid = (domain.elementTypeDefinitions.scope.controlImplementationDefinition!) />
<#assign useCICAs = (cid.customAspects?keys?seq_contains('scope_isoSoA'))!false />

<#macro row ci>
<#local ri=scope.requirementImplementations?filter(ri->ri.control._self == control._self)?first />
<#local applicable = ci.implementationStatus != 'N_A'>
<#local reason = (ri.implementationStatement)!ci.description!'' />
<tr>
<td>${control.abbreviation!}</td>
<td>${control.name}</td>
<td><#if applicable>${bundle.yes}<#else>${bundle.no}</#if></td>
<@riskCell color=statusMap[ci.implementationStatus].color>${bundle[ci.implementationStatus]}</@riskCell>
<td>${reason}</td>
<td>${(ri.responsible.name)!''}</td>
<td><#if ri.document??>${ri.document.name} (${ri.document.designator!ri.document.id})</#if></td>
</tr>
</#macro>


<#assign numCols = 7/>

<#macro section title cis>
<#if cis?has_content>
<tbody>
<tr>
<th colspan="${numCols}">${title}</th>
</tr>

<#list cis as ci>
<#assign control=ci.control />
<@row ci />
</#list>
</tbody>
</#if>
</#macro>

<#assign cis=sortCIs(scope.controlImplementations?filter(ci->ci.control.domains[domain.id].subType == 'CTL_ISOControl')) />
<#assign officialCIs=cis?filter(ci->ci.control.domains[domain.id].appliedCatalogItem?has_content) />
<#assign customCIs=cis?filter(ci->!ci.control.domains[domain.id].appliedCatalogItem?has_content) />

<#-- TODO group controls -->
<table class="table fullwidth control_list">
<thead>
<tr>
<th>${bundle.abbr}</th>
<th>${bundle.name}</th>
<th>${bundle.applicable_abbr}</th>
<th>${bundle.implementation_status}</th>
<th>${bundle.reason}</th>
<th>${bundle.datagood_responsible}</th>
<th>${bundle.datagood_evidence}</th>
</tr>
</thead>
<@section bundle.a5_controls officialCIs?filter(ci->ci.control.abbreviation?starts_with('A-5')) />
<@section bundle.a6_controls officialCIs?filter(ci->ci.control.abbreviation?starts_with('A-6')) />
<@section bundle.a7_controls officialCIs?filter(ci->ci.control.abbreviation?starts_with('A-7')) />
<@section bundle.a8_controls officialCIs?filter(ci->ci.control.abbreviation?starts_with('A-8')) />
<@section bundle.custom_controls customCIs  />
</table>