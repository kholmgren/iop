var i = 0;

function getDeviceId() {
    i++;
    return i % 2 ? 5 : 6;
}

module.exports = function (requestId) {
    var deviceId = getDeviceId();

    return {
        "userId": String(2),
        "deviceId": String(deviceId),
        "messageId": String(deviceId),
        "messageBody": "random"
    }
}
