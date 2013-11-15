var Jem = Jem || {};
Jem.realtime = Jem.realtime || {};

Jem.realtime.valueLabels = {};
Jem.realtime.gauges = {};
Jem.realtime.charts = {};
Jem.realtime.scales = {};
Jem.realtime.pfs    = {};

var gaugeOpts = {
lines: 12, // The number of lines to draw
       angle: 0.25, // The length of each line
       lineWidth: 0.5, // The line thickness
       pointer: {
length: 0.9, // The radius of the inner circle
        strokeWidth: 0.025, // The rotation offset
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

Jem.realtime.pfLabel = function(value) {
  if (value >= 0.999) {
    return "";
  } else if(value >= 0.0) {
    return " lagging";
  } else {
    return " leading";
  }
};

function writeToScreen(address, value) {

    if (address in Jem.realtime.valueLabels) {
      if(Jem.realtime.valueLabels[address]) {
	      var isPF = Jem.realtime.pfs[address];
        var scale = Jem.realtime.scales[address];

        if (isPF) {
          label = Jem.realtime.pfLabel(value * scale);
          value = Math.abs(value);
          Jem.realtime.valueLabels[address].innerHTML = (value * scale).toPrecision(5) + label;
        } else {
          Jem.realtime.valueLabels[address].innerHTML = (value * scale).toPrecision(5);
        }

        var gauge = Jem.realtime.gauges[address];
        //gauge.set(Math.min(gauge.maxValue, Math.max(gauge.minValue, value)));
        gauge.set(value * scale);
        //Jem.realtime.charts[address].append(new Date().getTime(), value);
      }
    } else {
      var meterDiv = $('[class~="meter"][data-register-address="' + address + '"]')[0];
      var gaugeCanvas = $(meterDiv).children('canvas.gauge')[0];

      if(!gaugeCanvas) {
        Jem.realtime.valueLabels[address] = false;
        return;
      }

      var scale = $(meterDiv).data("register-scale");
      Jem.realtime.scales[address] = scale;

      var isPF = $(meterDiv).data("register-is-pf");//=== "true";
      Jem.realtime.pfs[address] = isPF;

      var valueLabel = $(meterDiv).children('.register-value-label')[0];
      Jem.realtime.valueLabels[address] = valueLabel;

      if(isPF) {
        label = Jem.realtime.pfLabel(value * scale);
        value = Math.abs(value);
        Jem.realtime.valueLabels[address].innerHTML = (value * scale).toPrecision(5) + label;
      } else {
        Jem.realtime.valueLabels[address].innerHTML = ("%.4f", value * scale).toPrecision(5);
      }

      var gauge = new CornerGauge(gaugeCanvas).setOptions(gaugeOpts);
      Jem.realtime.gauges[address] = gauge;

      // Allow for open-ended max values
      if($(meterDiv).data("register-max-value")) {
        gauge.maxValue = $(meterDiv).data("register-max-value") * scale;
      } else {
        var minValue = $(meterDiv).data("register-min-value") * scale;
        gauge.maxValue = Math.max(value, minValue+5) * 2.0;
      }

      if (isPF) {
        gauge.minValue = 0.0;
      } else {
        gauge.minValue = $(meterDiv).data("register-min-value") * scale;
      }
      gauge.unitLabel = $(meterDiv).data("register-unit-of-measurement");

      // By setting the animation speed so high, we don't try to animate
      // the intermediate positions of the gauge when setting a new value.
      // This greatly reduces the CPU usage on the client, although it does
      // result in jerkier movement.
      gauge.animationSpeed = 8000000000000;

      if(value == 0) { // workaround
        gauge.set(1);
      }

      //gauge.set(Math.min(gauge.maxValue, Math.max(gauge.minValue, value)));
			gauge.set(value * scale);



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

