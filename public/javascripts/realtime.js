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
  Jem.realtime.meters = document.getElementById("meters");
  testWebSocket();
}

function testWebSocket() {
  Jem.realtime.websocket = new WebSocket(Jem.realtime.wsUri);
  Jem.realtime.websocket.onopen = function(evt) { onOpen(evt) };
  Jem.realtime.websocket.onclose = function(evt) { onClose(evt) };
  Jem.realtime.websocket.onmessage = function(evt) { onMessage(evt) };
  Jem.realtime.websocket.onerror = function(evt) { onError(evt) };
}

function onOpen(evt) {}
function onClose(evt) {}

function onMessage(evt) {

  var msg = $.parseJSON(evt.data);
  var deviceAddress = msg['device']['gateway']['host'].replace(/\./g, '-') + '-' +
                      msg['device']['gateway']['port'] + '-' +
                      msg['device']['unit'];

  for(var i=0; i<msg['values'].length; i++) {
    var address = deviceAddress + '-' + msg['values'][i][0];
    var value = msg['values'][i][1];
    writeToScreen(address, value);
  }
}

function onError(evt) {}
function doSend(message) {}

function writeToScreen(address, value) {

    if (address in Jem.realtime.valueLabels) {
      if(Jem.realtime.valueLabels[address]) {
        Jem.realtime.valueLabels[address].innerHTML = value;
        Jem.realtime.gauges[address].set(value);
        //Jem.realtime.charts[address].append(new Date().getTime(), value);
      }
    } else {
      var gaugeCanvas = $('[class~="meter"][data-register-address="' + address + '"] canvas.gauge')[0];

      if(!gaugeCanvas) {
        Jem.realtime.valueLabels[address] = false;
        return;
      }

      var gauge = new Gauge(gaugeCanvas).setOptions(gaugeOpts);
      gauge.maxValue = 100;
      gauge.minValue = 0;

      // By setting the animation speed so high, we don't try to animate
      // the intermediate positions of the gauge when setting a new value.
      // This greatly reduces the CPU usage on the client, although it does
      // result in jerkier movement.
      gauge.animationSpeed = 8000000;
      gauge.set(value);

      var valueLabel = $('[class~="meter"][data-register-address="' + address + '"] .register-value-label')[0];

      Jem.realtime.gauges[address] = gauge;
      Jem.realtime.valueLabels[address] = valueLabel;

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

