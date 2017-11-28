<#-- @ftlvariable name="" type="edu.olivet.harvester.bugreport.model.Bug" -->
<html>
<#include "include/HtmlHeader.ftl">
<body>
<#assign defaultVal="Not Provided">
<table style="width: 100%">
    <tr>
        <th width="120">Module</th>
        <td>${issueCategory!defaultVal}</td>
    </tr>
    <tr>
        <th>Priority</th>
        <td>${priority!defaultVal}</td>
    </tr>
    <tr>
        <th>Sid</th>
        <td>${context!defaultVal}</td>
    </tr>
    <tr>
        <th>Teamviewer Id</th>
        <td>${teamviewerId!defaultVal}</td>
    </tr>
    <tr>
        <th>Short Description</th>
        <td>${title!defaultVal}</td>
    </tr>
    <tr>
        <th>Details</th>
        <td>${detail!defaultVal}</td>
    </tr>
    <tr>
        <th>Contact</th>
        <td>${reporterEmail!defaultVal}</td>
    </tr>

    <tr>
        <th>Harvester Version</th>
        <td>${version!defaultVal}</td>
    </tr>
</table>
</body>
</html>