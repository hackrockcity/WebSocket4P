/**
 * you can put a one sentence description of your library here.
 *
 * ##copyright##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author		##author##
 * @modified	##date##
 * @version		##version##
 */

package com.nycresistor.processing.net;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import processing.core.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.eclipse.jetty.websocket.WebSocket.Outbound;

public class WebSocket4P {
	private PApplet parent;
	private Server server;
	private Method messageCallback = null;
	private Method dataCallback = null;
	private Method connectCallback = null;
	private Method disconnectCallback = null;
	private Method postCallback = null;
	private Method getCallback = null;
	private Hashtable<Integer, Outbound> clients = new Hashtable<Integer, Outbound>();
	private int connectionCount = 0;
	private int port = 8080;

	public final static String VERSION = "##version##";
	
	/**
	 * Creates a WebSocket Server
	 * 
	 * @param parent this
	 */
	public WebSocket4P(PApplet parent) {
		this.parent = parent;
		
		initCallbacks();
		initServer();
	}
	
	/**
	 * Creates a WebSocket Server
	 * 
	 * @param parent this
	 * @param port Port number. Defaults to 8080.
	 */
	public WebSocket4P(PApplet parent, int port) {
		this.port = port;
		this.parent = parent;
		
		initCallbacks();
		initServer();
	}

	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

	/**
	 * Sets up the server
	 */
	private void initServer() {
		this.server = new Server(port);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
	    context.setContextPath("/");
	    server.setHandler(context);
	 
	    context.addServlet(new ServletHolder(new ProcessingWebSocketServlet()),"/*");
	    
	    try {
			server.start();
	    } 
	    catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets a method from the sketch
	 * @param name Name of method
	 * @param args Classes of parameters
	 * @return A method or null
	 */
	private Method getMethod(String name, Class... args) {
		try {
			return parent.getClass().getMethod(name, args);
		} 
		catch (NoSuchMethodException e) {
			return null; 
		}		
	}
	
	/**
	 * Sets up the callbacks to the sketch
	 */
	private void initCallbacks() {
		messageCallback = getMethod("websocketMessage", int.class, String.class);
		connectCallback = getMethod("websocketConnect", int.class);
		disconnectCallback = getMethod("websocketDisconnect", int.class);
		dataCallback = getMethod("websocketMessage", int.class, byte[].class, int.class, int.class);
		getCallback = getMethod("get", String.class);
		postCallback = getMethod("post", String.class, Map.class);
	}
	
	/**
	 * Calls back the sketch.
	 * @param m Method
	 * @param args Arguments
	 */
	private void callback(Method m, Object... args) {
		
		try {
			if (m != null)
				m.invoke(parent, args);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calls back the sketch.
	 * @param m Method
	 * @param args Arguments
	 */
	private String callbackString(Method m, Object... args) {
		
		try {
			if (m != null) {
				Object result = m.invoke(parent, args);
				
				if (result != null) {
					return result.toString();
				}
			}
				
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	
	/**
	 * Returns number of connected clients. Not the number of the last connected client.
	 * @return Number of connected clients.
	 */
	public int connected() {
		return clients.size();
	}
	
	/**
	 * Send a message to a single client
	 * @param connectionNumber Number of client.
	 * @param message Message to send.
	 */
	public void send(int connectionNumber, String message) {
		try {			
			clients.get(connectionNumber).sendMessage(message);
		}
		catch (IOException e) {
			System.err.println("Connection #"+ connectionNumber + ": " + e.getMessage());
		}
	}
	
	/**
	 * Send a message to all connected clients.
	 * @param message Message to send.
	 */
	public void sendAll(String message) {
		for (Enumeration<Integer> e = clients.keys(); e.hasMoreElements();) {
			send(e.nextElement(),message);			
		}
	}
	
	/**
	 * Send a message to all connected clients except one.
	 * @param connectionNumber Excepted client.
	 * @param message Message to send.
	 */
	public void sendAllExcept(int connectionNumber, String message) {
		Integer i;
		for (Enumeration<Integer> e = clients.keys(); e.hasMoreElements();) {
			i = e.nextElement();
			if (i != connectionNumber) 
				send(e.nextElement(),message);			
		}
	}
	
	/**
	 * Servlet to handle WebSocket connections
	 * @author justin
	 *
	 */
	class ProcessingWebSocketServlet extends WebSocketServlet {

		private static final long serialVersionUID = 1L;
	
		/**
		 * Called when a regular HTTP GET is received.  For now just says connect with ws.
		 */
		protected void doGet(HttpServletRequest request, HttpServletResponse response) 
        	throws ServletException, IOException 
        {
			String result = callbackString(getCallback, request.getRequestURI());
			response.setContentType("text/javascript;charset=utf-8");
		    response.setStatus(HttpServletResponse.SC_OK);
		    response.getWriter().println(result);		    
        }
		    
		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

			String result = callbackString(postCallback, request.getRequestURI(), request.getParameterMap());
			response.setContentType("text/javascript;charset=utf-8");
		    response.setStatus(HttpServletResponse.SC_OK);
		    response.getWriter().println(result);		    
		}

		/**
		 * Called when a WebSocket connection is established.
		 */
		@Override
		protected WebSocket doWebSocketConnect(HttpServletRequest request, String arg1) {
			return new ProcessingWebSocket();
		}

		/**
		 * Represents a WebSocket connection
		 * @author justin
		 *
		 */
		class ProcessingWebSocket implements WebSocket {
			
			int connectionNumber;
			
			/**
			 * Creates a WebSocket object.
			 */
			public ProcessingWebSocket() {}
					
			/**
			 * Called when a connection is established.
			 */
			public void onConnect(Outbound outbound) {
				connectionNumber = connectionCount++;
				clients.put(connectionNumber, outbound);
				callback(connectCallback, connectionNumber);
			}

			/**
			 * Called when a connection is cut.
			 */
			public void onDisconnect() {
				clients.remove(connectionNumber);
				callback(disconnectCallback, connectionNumber);
			}

			/**
			 * Called when a message is received.
			 */
			public void onMessage(byte frame, String data) {
				callback(messageCallback, connectionNumber, data);
			}

			/**
			 * Called when a message is received.
			 */
			public void onMessage(byte frame, byte[] data, int offset, int length) {
				callback(dataCallback, connectionNumber, data, offset, length);
			}		
		}
	}
}
