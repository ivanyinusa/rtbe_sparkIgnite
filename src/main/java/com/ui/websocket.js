/**
 * Created by root on 3/31/16.
 */
var ws = new WebSocket("ws://192.168.1.101:8889/");

ws.onopen = function() {
    alert("Opened");
    ws.send("I'm client");
};

ws.onmessage = function (evt) {
};

ws.onclose = function() {
    alert("Closed");
};

ws.onerror = function(err) {
    alert("Error: " + err);
};

ws.binaryType = "arraybuffer";
var button = document.getElementById("button");
button.onclick = function() {
    ws.send("image"); // send the fetch request
};
ws.onmessage = function (evt) { // display the image
    var bytes = new Uint8Array(evt.data);
    var data = "";
    var len = bytes.byteLength;
    for (var i = 0; i < len; ++i) {
        data += String.fromCharCode(bytes[i]);
    }
    var img = document.getElementById("image");
    img.src = "data:image/png;base64,"+window.btoa(data);
};
