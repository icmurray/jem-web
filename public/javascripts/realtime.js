var Jem = Jem || {};
Jem.realtime = Jem.realtime || {};

Jem.realtime.valueLabels = {};
Jem.realtime.gauges = {};
Jem.realtime.charts = {};

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
  Jem.realtime.registerTemplate = Handlebars.compile($('#register-template').html());
  Jem.realtime.meters = document.getElementById("meters");
  testWebSocket();
}

function testWebSocket() {
  Jem.realtime.websocket = new WebSocket(Jem.realtime.wsUri);
  Jem.realtime.websocket.binaryType = "arraybuffer";
  Jem.realtime.websocket.onopen = function(evt) { onOpen(evt) };
  Jem.realtime.websocket.onclose = function(evt) { onClose(evt) };
  Jem.realtime.websocket.onmessage = function(evt) { onMessage(evt) };
  Jem.realtime.websocket.onerror = function(evt) { onError(evt) };
}

function onOpen(evt) {}
function onClose(evt) {}

function onMessage(evt) {
  var d = new DataView(evt.data);
  writeToScreen(d);
}

function onError(evt) {}
function doSend(message) {}

function writeToScreen(message) {

  var address = message.getUint16(0, false);
  var value   = message.getFloat64(2, false)

    if (address in Jem.realtime.valueLabels) {
      Jem.realtime.valueLabels[address].innerHTML = value;
      Jem.realtime.gauges[address].set(value);
      //Jem.realtime.charts[address].append(new Date().getTime(), value);
    } else {

      var templateContext = {
        address: address,
        value: value
      };

      var htmlString = Jem.realtime.registerTemplate(templateContext);
      var html = $.parseHTML(htmlString.trim())[0];
      var gaugeCanvas = $(html).find('canvas.gauge')[0];
      var gauge = new Gauge(gaugeCanvas).setOptions(gaugeOpts);
      gauge.maxValue = 20;
      gauge.minValue = 0;
      gauge.animationSpeed = 8;
      gauge.set(value);

      var valueLabel = $(html).find('.register-value-label')[0];

      Jem.realtime.gauges[address] = gauge;
      Jem.realtime.valueLabels[address] = valueLabel;

      Jem.realtime.meters.insertBefore(html, Jem.realtime.meters.firstChild);
      //var chartCanvas = document.createElement("canvas");
      //chartCanvas.width=400;
      //chartCanvas.height=50;
      //var chart = new SmoothieChart({
      //  maxValue: 20,
      //  minValue: 0
      //});
      //chart.streamTo(chartCanvas,600);
      //var chartLine = new TimeSeries();
      //chart.addTimeSeries(chartLine);
      //charts[address] = chartLine;
      //chartLine.append(new Date().getTime(), value);

  }

}
window.addEventListener("load", init, false);

