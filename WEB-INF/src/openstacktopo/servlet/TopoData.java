package openstacktopo.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import openstacktopo.Topo;

public class TopoData extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("sssssssssssss");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		Topo topo = new Topo();
		topo.getTopo();
		out.println(topo.getTopo().toString());

		out.flush();
		out.close();
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
	
}
