/**
 * 
 */
package org.eclipse.m2eclipse.example;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * @author wemü
 *
 */
public class DefaultServlet extends HttpServlet {

	/**
	 * 
	 */
	public DefaultServlet() {
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		Enumeration<String> ctxParamNames = config.getInitParameterNames();
		while(ctxParamNames.hasMoreElements()) {
			String paramName = ctxParamNames.nextElement();
			String paramValue = config.getInitParameter(paramName);
			System.out.println("Servlet: Context-Param: " + paramName +" = " + paramValue);
		}
	}

}
