<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="java.util.Enumeration"%>
<html>
<head>
	<title>m2eclipse test - index.jsp</title>
</head>
<body>

	<p>Welcome to web.xml filtering 2</p>

	<ul>
	
	<%
	Enumeration<String> ctxParamNames = config.getServletContext().getInitParameterNames();
	while(ctxParamNames.hasMoreElements()) {
		String paramName = ctxParamNames.nextElement();
		String paramValue = config.getServletContext().getInitParameter(paramName);
		out.println("<li>");
			out.println("Context-Param: " + paramName +" = " + paramValue);
		out.println("</li>");
		
	}
	%>
	
	
	</ul>

</body>
</html>
