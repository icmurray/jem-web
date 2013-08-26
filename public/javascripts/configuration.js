var Jem = Jem || {};
Jem.systemConfiguration = Jem.systemConfiguration || {};

Jem.systemConfiguration.updatePower = function (deviceId) {
    var checkbox = $('[class~="auto-update-checkbox"][data-device="' + deviceId + '"]');
    if (checkbox.prop("checked")) {
      var powerField = $('[class~="power-field"][data-device="' + deviceId + '"]');
      var currentField = $('[class~="current-field"][data-device="' + deviceId + '"]');
      var voltageField = $('[class~="voltage-field"][data-device="' + deviceId + '"]');

      var current = currentField.prop("value");
      var voltage = voltageField.prop("value");

      powerField.prop("value", Math.ceil(current * voltage * Math.sqrt(3) / 1000));
    }
};

Jem.systemConfiguration.init = function() {
  //$(".power-field").prop("disabled", true);
  $(".auto-update-checkbox").click(function() {
    var deviceId = $(this).data("device");
    var input = $('[class~="power-field"][data-device="' + deviceId + '"]');
    var checked = $(this).prop("checked");
    input.prop("readonly", checked);
    if(checked) {
      Jem.systemConfiguration.updatePower(deviceId); 
    }
  });

  $(".current-field").change(function() {
    var deviceId = $(this).data("device");
    Jem.systemConfiguration.updatePower(deviceId)
  });

  $(".voltage-field").change(function() {
    var deviceId = $(this).data("device");
    Jem.systemConfiguration.updatePower(deviceId)
  });
}

window.addEventListener("load", Jem.systemConfiguration.init, false);

