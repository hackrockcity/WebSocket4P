/*
Super simple WebSocket chat server.  Below is the HTML+Javascript to connect. 
I've only tested this in Chrome, but it should work in other browsers that
support WebSocket.

<script>
	var ws = new WebSocket("ws://localhost:8080/");
	ws.onopen = function(evt) { console.log("Connection open ..."); };
	ws.onmessage = function(evt) { console.log( "Received Message: "  +  evt.data); };
	ws.onclose = function(evt) { console.log("Connection closed."); };
</script>

<h1>WebSocket4P Test</h1>
<input type="text" id="text" size="40" />
<input type="button" 
	onclick="ws.send(document.getElementById('text').value); return true;" 
	value="Send" />
*/

import com.nycresistor.processing.net.*;

WebSocket4P websocket;

void setup() {
  websocket = new WebSocket4P(this);
}

void draw() {
}

void websocketMessage(int clientNumber, String message) {
  println(clientNumber + " said " + message);
  websocket.sendAll(clientNumber + " said " + message);
}

void websocketConnect(int clientNumber) {
  println(clientNumber + " connected");
  websocket.send(clientNumber, "Welcome connection number " + clientNumber);  
}

void websocketDisconnect(int clientNumber) {
  println(clientNumber + " disconnected");
}
