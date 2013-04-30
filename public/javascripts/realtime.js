var output;
var tds = {};
var gauges = {};
var charts = {};
var gaugeOpts = {
lines: 12, // The number of lines to draw
       angle: 0.15, // The length of each line
       lineWidth: 0.44, // The line thickness
       pointer: {
length: 0.9, // The radius of the inner circle
        strokeWidth: 0.035, // The rotation offset
        color: '#000000' // Fill color
       },
colorStart: '#11CF2A',   // Colors
            colorStop: '#11CF2A',    // just experiment with them
            strokeColor: '#E0E0E0',   // to see which ones work best for you
            generateGradient: false
};

function init() {
  output = document.getElementById("output"); testWebSocket();
  //document.getElementById("message").addEventListener("submit", function(evt) {
  //	var title = document.getElementById("title")
  //	var content = document.getElementById("content")
  //	console.log(title.value, content.value)
  //	websocket.send(JSON.stringify({title: title.value, content: content.value}))
  //	evt.stopPropagation()
  //	evt.preventDefault()
  //})
}
function testWebSocket() {
  websocket = new WebSocket(wsUri);
  websocket.binaryType = "arraybuffer";
  websocket.onopen = function(evt) { onOpen(evt) };
  websocket.onclose = function(evt) { onClose(evt) };
  websocket.onmessage = function(evt) { onMessage(evt) };
  websocket.onerror = function(evt) { onError(evt) };
}
function onOpen(evt) {}
function onClose(evt) { /*writeToScreen("DISCONNECTED");*/ }
function onMessage(evt) {
  //writeToScreen(JSON.parse(evt.data));
  var d = new DataView(evt.data);
  writeToScreen(d);
}
function onError(evt) { writeToScreen('<span style="color: red;">ERROR:</span> ' + evt.data); }
function doSend(message) { writeToScreen("SENT: " + message);  websocket.send(message); }
function writeToScreen(message) {

  var address = message.getUint16(0, false);
  var value   = message.getFloat64(2, false)

    if (address in tds) {
      var td = tds[address];
      td.innerHTML = value;
      gauges[address].set(value);
      //charts[address].append(new Date().getTime(), value);
    } else if (address >= 50462) {

      var tr = document.createElement("tr");
      var th = document.createElement("th");
      var td = document.createElement("td");
      var canvas = document.createElement("canvas");
      canvas.width=100;
      canvas.height=50;
      var gauge = new Gauge(canvas).setOptions(gaugeOpts);
      gauge.maxValue = 20;
      gauge.minValue = 0;
      gauge.animationSpeed =8;
      gauge.set(value);

      var chartCanvas = document.createElement("canvas");
      chartCanvas.width=400;
      chartCanvas.height=50;
      var chart = new SmoothieChart({
maxValue: 20,
minValue: 0
});
//chart.streamTo(chartCanvas,600);
var chartLine = new TimeSeries();
chart.addTimeSeries(chartLine);
charts[address] = chartLine;
chartLine.append(new Date().getTime(), value);

tds[address] = td;
gauges[address] = gauge;
td.innerHTML = value;
th.innerHTML = address;
tr.insertBefore(chartCanvas, tr.firstChild);
tr.insertBefore(canvas, tr.firstChild);
//tr.insertBefore(td, tr.firstChild);
tr.insertBefore(th, tr.firstChild);
output.insertBefore(tr, output.firstChild);
}

}
window.addEventListener("load", init, false);

