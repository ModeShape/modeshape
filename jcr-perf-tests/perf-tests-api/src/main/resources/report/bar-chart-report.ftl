<html>
	<head>
		<title>Performance Report</title>
	</head>
    <body>
        <ul>
            <#list testMap?keys as testName>
            <li><a href="${testMap[testName]}" target="_blank">${testName}</a></li>
            </#list>
        </ul>
   </body>
</html>