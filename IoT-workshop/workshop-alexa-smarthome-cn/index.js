
// -*- coding: utf-8 -*-

// Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

// Licensed under the Amazon Software License (the "License"). You may not use this file except in
// compliance with the License. A copy of the License is located at

//    http://aws.amazon.com/asl/

// or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
// language governing permissions and limitations under the License.

'use strict';

const config = require('./config.json');

let AWS = require('aws-sdk');
AWS.config.update({region: process.env.AWS_REGION || 'us-east-1'});
const iotdata = new AWS.IotData({
  accessKeyId: 'xxxxxxxxxxxxxxxx', secretAccessKey: 'xxxxxxxxxxxxxxxxxxxxxxxxxxxx', region: 'cn-northwest-1', endpoint: config.iotEndpoint
});

let AlexaResponse = require("./alexa/skills/smarthome/AlexaResponse");
const { decodeToken } = require('./auth');

// If you don't have your own global account, use the code below.
const ddb = new AWS.DynamoDB.DocumentClient({ region: process.env.AWS_REGION || 'us-east-1' });
// If you have your own global account, use the code below.
// const ddb = new AWS.DynamoDB.DocumentClient({ accessKeyId: 'xxxxxxxxxxxxx', secretAccessKey: 'xxxxxxxxxxxxxxxxxxxxx', region: 'us-east-1' });

exports.handler = async function (event, context) {

  // Dump the request for logging - check the CloudWatch logs
  console.log("index.handler request  -----");
  console.log(JSON.stringify(event));

  if (context !== undefined) {
    console.log("index.handler context  -----");
    console.log(JSON.stringify(context));
  }
  console.log('flag0 from chian');
  // Validate we have an Alexa directive
  if (!('directive' in event)) {
    let aer = new AlexaResponse(
      {
        "name": "ErrorResponse",
        "payload": {
          "type": "INVALID_DIRECTIVE",
          "message": "Missing key: directive, Is request a valid Alexa directive?"
        }
      });
    return sendResponse(aer.get());
  }
  console.log('flag1');
  // Check the payload version
  if (event.directive.header.payloadVersion !== "3") {
    let aer = new AlexaResponse(
      {
        "name": "ErrorResponse",
        "payload": {
          "type": "INTERNAL_ERROR",
          "message": "This skill only supports Smart Home API version 3"
        }
      });
    return sendResponse(aer.get())
  }

  let namespace = ((event.directive || {}).header || {}).namespace;
  console.log('flag2');
  if (namespace.toLowerCase() === 'alexa.authorization') {
    let aar = new AlexaResponse({"namespace": "Alexa.Authorization", "name": "AcceptGrant.Response",});
    return sendResponse(aar.get());
  }
  console.log('flag3');
  if (namespace.toLowerCase() === 'alexa.discovery') {
    let token = event.directive.payload.scope.token;
    const { username } = await decodeToken(token);

    let adr = new AlexaResponse({"namespace": "Alexa.Discovery", "name": "Discover.Response"});
    let capability_alexa = adr.createPayloadEndpointCapability();
    let capability_alexa_powercontroller = adr.createPayloadEndpointCapability({"interface": "Alexa.PowerController", "supported": [{"name": "powerState"}]});

    if (username) {

      const devices = await getDevicesByUsername(username);
      devices.forEach(device => {
        adr.addPayloadEndpoint({"friendlyName": "Smart Lamp", "endpointId": device.thingName, "capabilities": [capability_alexa, capability_alexa_powercontroller]});
      });
      return sendResponse(adr.get());
    } else {

      return sendResponse(adr.get());
    }
  }
  console.log('flag4');
  if (namespace.toLowerCase() === 'alexa.powercontroller') {
    let power_state_value = "OFF";
    if (event.directive.header.name === "TurnOn")
      power_state_value = "ON";
    console.log('flag2——1');
    let thingName = event.directive.endpoint.endpointId;
    let token = event.directive.endpoint.scope.token;
    let correlationToken = event.directive.header.correlationToken;
    console.log('flag2——2');
    const { username } = await decodeToken(token);

    let ar = new AlexaResponse(
      {
        "correlationToken": correlationToken,
        "token": token,
        "endpointId": thingName
      }
    );
    console.log('flag2——3');
    ar.addContextProperty({"namespace":"Alexa.PowerController", "name": "powerState", "value": power_state_value});

    // Check for an error when setting the state
    console.log('username: %s, thingName: %s, power_state_value: %s', username,thingName,power_state_value);
    let state_set = await updateDeviceState(username, thingName, power_state_value);
    console.log('flag5');
    if (!state_set) {
      return new AlexaResponse(
        {
          "name": "ErrorResponse",
          "payload": {
            "type": "ENDPOINT_UNREACHABLE",
            "message": "Unable to reach endpoint database."
          }
        }).get();
    }
    console.log('flag6');
    return sendResponse(ar.get());
  }
  console.log('flag7');
};

function sendResponse(response)
{
  // TODO Validate the response
  console.log("index.handler response -----");
  console.log(JSON.stringify(response));
  return response
}

/**
 * Update the device power state
 * this function will validate the binding relationship.
 * @param username
 * @param thingName
 * @param state
 * @returns {Object}
 */
async function updateDeviceState(username, thingName, state) {
  console.log('flag1——1');
  const ddbParams = {
    TableName: config.deviceTable,
    IndexName: "ByUsernameThingName",
    KeyConditionExpression: "username = :username AND thingName = :thingName",
    ExpressionAttributeValues: {
      ':username': username,
      ':thingName': thingName
    }
  };
  console.log('flag1——2');
  const ddbResult = await ddb.query(ddbParams).promise();

  // If can find device, it is a valid device
  if (ddbResult.Count > 0) {
    const iotParams = {
      thingName: thingName,
      payload: JSON.stringify({
        state: {
          desired: {
            power: state
          }
        }
      })
    };
    console.log('Send update message to the IOT');
    return await iotdata.updateThingShadow(iotParams).promise()

  } else {
    console.log('Error: No device found in the table!!');
    return new Error('No device found')
  }

}

/**
 * Get the devices by username
 * @param username
 * @returns {Promise<*>} Array of devices
 */
async function getDevicesByUsername(username) {
  const params = {
    TableName: config.deviceTable,
    IndexName: "ByUsernameThingName",
    KeyConditionExpression: "username = :username",
    ExpressionAttributeValues: {
      ':username': username
    }
  };

  const result = await ddb.query(params).promise();

  return result.Items;
}


