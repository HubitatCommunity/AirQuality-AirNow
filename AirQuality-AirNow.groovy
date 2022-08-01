/*
 * Import URL: https://raw.githubusercontent.com/HubitatCommunity/AirQuality-AirNow/master/AirQuality-AirNow.groovy"
 *
 *	Copyright 2021 C Steele
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */
 
/*
 *         v1.0.2  PR from cmbruns
 *			 According to Hubitat docs the airQualityIndex attribute is supposed range from 0 to 500, meaning it should be the 
 *			 full PM10-equivalent value, not the 6-category meaning previously used and matches Ecowitt air quality sensor range.
 *         v1.0.1  renamed "PM2.5" attribute to not use a dot (.)
 * csteele v1.0.0  created.
 */ 

static String version()	{  return '1.0.2'  }

import groovy.transform.Field

metadata {
	definition (name: "Air Quality from AirNow", namespace: "csteele", author: "CSteele") {
		capability "AirQuality"
		capability "Sensor"

		attribute 'O3', 'number'
		attribute 'PM2_5', 'number'
		attribute 'PM10', 'number'
		attribute 'airQualityIndex', 'number'
		attribute 'airQualityColor', 'STRING'

		command 'pollAirNow'
	}

	preferences {
		input "apiKey",      "text", title: "<b>Type AirNow.org API Key Here</b>", required: true, defaultValue: null
		input "pollEvery",   "enum", title: "<b>Publish AQI how frequently?</b>",  required:false, defaultValue: 1, options:[1:"1 hour",2:"2 hours",8:"8 hours",16:"16 hours"]
		input "basedOn",     "enum", title: "<b>Publish AQI Number based on?</b>", required:false, defaultValue: 1, options:[1:"O3 ozone",2:"PM2.5 particle",3:"PM10 partice", 4: "Worst AQI"]
		input "debugOutput", "bool", title: "Enable debug logging",                required:false, defaultValue: true
		input "txtEnable",   "bool", title: "Enable descriptionText logging",      required:false, defaultValue: true
	}
}


void pollAirNow() {
	if ( apiKey == null ) {
		return
	}
	Map params = [ 
	   uri: 'https://www.airnowapi.org/aq/observation/latLong/current/?format=application/json&latitude=' + (String)location.latitude + '&longitude=' + (String)location.longitude + '&distance=25&API_KEY=' + (String)apiKey,
	   timeout: 20 ]
	//if (debugOutput) log.debug "params:${params}"
	asynchttpGet('pollHandler', params)
}


void pollHandler(resp, data) {
	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		//if (debugOutput) log.debug "R: $resp.data"
		aqi = parseJson(resp.data)

		def isBasis = aqiBasis[basedOn as Integer]
		def maxAQI  = -1
		def maxAQICat = -1

		aqi.each { obs ->
			if ((obs.AQI >= 0) && (obs.AQI <= 2000)) { // sanity check the value - AirNow api glitch removal
				if (obs.AQI > maxAQI) {
					maxAQI = obs.AQI
					maxAQICat = obs.Category.Number
				}

				def descriptionText = "${device.displayName} ${obs.ParameterName} is ${obs.AQI}"
				def attrNam = obs.ParameterName.replace('.', '_')

				if (debugOutput) log.info "${descriptionText}"
				sendEvent(name: attrNam, value: obs.AQI, descriptionText: descriptionText)

				if (isBasis == obs.ParameterName) {
					descriptionText = "${device.displayName} airQualityIndex is ${obs.AQI}"
					if (txtEnable) log.info "${descriptionText}"
					sendEvent(name: "airQualityIndex", value: obs.AQI, descriptionText: descriptionText)
					descriptionText = "${device.displayName} airQualityColor is ${aqiColor[obs.Category.Number]}"
					if (debugOutput) log.info "${descriptionText}"
					sendEvent(name: "airQualityColor", value: aqiColor[obs.Category.Number], descriptionText: descriptionText)
				}
			}
		}
		if (isBasis == "maxAQI") {
			descriptionText = "${device.displayName} airQualityIndex is $maxAQI"
			if (txtEnable) log.info "${descriptionText}"
			sendEvent(name: "airQualityIndex", value: maxAQI, descriptionText: descriptionText)
			descriptionText = "${device.displayName} airQualityColor is ${aqiColor[maxAQICat]}"
			if (debugOutput) log.info "${descriptionText}"
			sendEvent(name: "airQualityColor", value: aqiColor[maxAQICat], descriptionText: descriptionText)

		}
	}
}


void updated() {
	unschedule()
	//               "Seconds" "Minutes" "Hours" "Day Of Month" "Month" "Day Of Week" "Year"
	if (apiKey) schedule("3 7 0/${pollEvery} ? * * *", pollAirNow)
	if (debugOutput) runIn(1800,logsOff)
}


void uninstalled() {
	unschedule()
}


def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}


@Field static aqiColor = [1: "Green - Good", 2: "Yellow - Moderate", 3: "Orange - Unhealthy for Sensitive Groups", 4: "Red - Unhealthy", 5: "Purple - Very Unhealthy", 6: "Maroon - Hazardous"]
@Field static aqiBasis = [1: "O3", 2: "PM2.5", 3: "PM10", 4: "maxAQI"]
