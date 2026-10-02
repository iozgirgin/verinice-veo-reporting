<#import "/libs/commons.md" as com>

<#function riskReductionLabel raw>
  <#if .lang == 'en'>
    <#return {"RISK_TREATMENT_ACCEPTANCE": "Risk acceptance",
      "RISK_TREATMENT_AVOIDANCE": "Risk avoidance",
      "RISK_TREATMENT_NONE": "None",
      "RISK_TREATMENT_REDUCTION": "Risk reduction",
      "RISK_TREATMENT_TRANSFER": "Risk transfer"}[raw] />
  </#if>
  <#return { "RISK_TREATMENT_ACCEPTANCE": "Risikoakzeptanz",
      "RISK_TREATMENT_AVOIDANCE": "Risikovermeidung",
      "RISK_TREATMENT_NONE": "Keins",
      "RISK_TREATMENT_REDUCTION": "Risikoreduktion",
      "RISK_TREATMENT_TRANSFER": "Risikotransfer"}[raw] />
</#function>

<#macro impactdisplay riskDefinition category value=""><#if value?has_content><span style="color:#222;border-left:3mm solid ${riskDefinition.getImpact(category.id, value).color};padding-left:1.5mm">${riskDefinition.getImpact(category.id, value).label}</span></#if></#macro>

<#macro probabilitydisplay riskDefinition value=""><#if value?has_content><span style="color:#222;border-left:3mm solid ${riskDefinition.getProbability(value).color};padding-left:1.5mm">${riskDefinition.getProbability(value).label}</span></#if></#macro>

<#macro riskCell color>
  <td style="background-image: linear-gradient(to right, ${color} 0mm, ${color} 5mm, white 5mm, white);padding-left: 7mm;"><#nested></td>
</#macro>