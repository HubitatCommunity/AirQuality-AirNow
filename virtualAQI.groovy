/*
 * Import URL: https://raw.githubusercontent.com/HubitatCommunity/??/master/??-Driver.groovy"
 *
 *	Copyright 2019 C Steele
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
	public static String version()      {  return "v0.0.1"  }
 
 
import groovy.transform.Field

metadata 
{
	definition(name: "Virtual AQI", namespace: "csteele", author: "C Steele", importUrl: "https://raw.githubusercontent.com/HubitatCommunity/??/master/??.groovy")
	{
 		capability "AirQuality"
 		capability "Sensor"
 		
 		command "setAQI", ["number"]
	}

      preferences 
      {
     

          //standard logging options
          input name: "debugOutput", type: "bool", title: "Enable debug logging", defaultValue: true
      }
}


/*
	updated
    
	Doesn't do much other than call initialize().
*/
void updated()
{
	initialize()
	unschedule()
      if (debugOutput) runIn(1800,logsOff) //disable debug logs after 30 min
	log.info "Updated ran. debug: $debugOutput"
}



/*
	installed
    
	Doesn't do much other than call initialize().
*/
void installed()
{
	initialize()
}


/*
	uninstalled
    
	When the device is removed to allow for any necessary cleanup.
*/
void uninstalled()
{
	log.trace "Msg: uninstalled ran"
}

/*
	initialize
    
	Doesn't do anything.
*/
void initialize()
{
	log.trace "Msg: initialize ran"
}


/*
	parse
    
	In a virtual world this should never be called.
*/
void parse(String description)
{
	log.trace "Msg: Description is $description"
}

/*

	generic driver stuff

*/

void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("debugOutput",[value:"false",type:"bool"])
}


/*

	driver command response

*/

/*
	setAQI
    
	Set the device value.
*/
void setAQI(val)
{
	// The server will update on/off status
	if (debugOutput) log.debug "$device AQI: $val"
	sendEvent(name: 'airQualityIndex', value: val as Integer)
}

