# EFR Connect Mobile Application
This is the source code for the EFR Connect mobile application.



## Overview

The Silicon Labs EFR Connect app utilizes the Bluetooth adapter on your phone/tablet to scan, connect and interact with BLE devices.

The app is divided into two main functional areas, the demo and the develop view.

The demo view lists a number of demos that are meant for quickly testing some of the sample apps in the Silicon Labs Bluetooth SDK. The currently supported demos are:

- **Health Thermometer demo:** Connects to an EFR32/BGM device running the soc-thermometer sample application from the Bluetooth SDK and displays the temperature read from the SI7021 sensor on the WSTK mainboard.

The develop view contains functionalities focused on helping developers create and troubleshoot their Bluetooth applications based on EFR32/BGM devices. The currently supported functionalities are:

- **Bluetooth Browser:** This is a generic and powerful tool that allows you to explore the BLE devices around you. Key features of the browser include:
  - Scan results with rich data set
    - Connectable/non-connectable
    - Beacon type
    - Advertisement interval
    - RSSI
    - Bluetooth address
  - Ability to favorite devices on the scan list so that they surface to the top
  - Advanced filtering options to quickly identify the devices you want
    - Device name
    - Raw advertisement data
    - RSSI
    - Beacon type (iBeacon, Eddystone, AtlBeacon)
    - Favorites only
    - Connectable only
  - Option to save filters for later use
  - Support for multiple connections
  - Support for Bluetooth 5 advertising extensions
  - Ability to rename services/characteristics with 128-bit UUIDs (mappings dictionary)
  - Over-the-air (OTA) device firmware upgrade (DFU) with both reliable and speed modes
  - Configurable MTU and connection interval
  - Support for all GATT operations



## Additional information
The app can be found on the [Google PlayStore](https://play.google.com/store/apps/details?id=com.siliconlabs.bledemo&hl=en) and [Apple App Store](https://apps.apple.com/us/app/blue-gecko/id1030932759).

For more information on Silicon Labs product portfolio please visit [www.silabs.com](https://www.silabs.com). 



## License

    Copyright 2020 Silicon Laboratories
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



