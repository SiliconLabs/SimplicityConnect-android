# EFR Connect Mobile Application
This is the source code for the EFR Connect mobile application.

## What is EFR Connect BLE mobile app? 

Silicon Labs EFR Connect is a generic BLE mobile app for testing and debugging Bluetooth® Low Energy applications. With EFR Connect, you can quickly troubleshoot your BLE embedded application code, Over-the-Air (OTA) firmware update, data throughput, and interoperability with Android and iOS mobiles, among the many other features. You can use the EFR Connect app with all Silicon Labs Bluetooth development kits, Systems on Chip (SoC), and modules.

## Why download EFR Connect? 
EFR Connect radically saves the time you will use for testing and debugging! With EFR Connect, you can quickly see what’s wrong with your code and how to fix and optimize it. EFR Connect is the first BLE mobile app allowing you to test data throughput and mobile interoperability with a single tap on the app.

## How does it work? 
Using EFR Connect BLE mobile app is easy. It runs on your mobile devices such as a smartphone or tablet. It utilizes the Bluetooth adapter on the mobile to scan, connect and interact with nearby BLE hardware.

After connecting the EFR Connect app and BLE hardware (e.g., a dev kit), the Blinky test on the app shows a green light indicating when your setup is ready to go. The app includes simple demos to teach you how to get started with EFR Connect and all Silicon Labs development tools.

The Browser, Advertiser, and Logging features help you to find and fix bugs quickly and test throughput and mobile interoperability simply, with a tap of a button. With our Simplicity Studio’s Network Analyzer tool (free of charge), you can view the packet trace data and dive into the details.

## Demos and Sample Apps
EFR Connect includes many demos to test sample apps in the Silicon Labs GSDK quickly. Here are demo examples: 

- **Blinky**: The ”Hello World” of BLE – Toggling a LED is only one tap away. 
- **Throughput**: Measure application data throughput between the BLE hardware 
 and your mobile device in both directions
- **Health Thermometer**: Connect to a BLE hardware kit and receive the temperature data from the on-board sensor.
- **Connected Lighting DMP**: Leverage the dynamic multi-protocol (DMP) sample apps to control a DMP light node from a mobile and protocol-specific switch node (Zigbee, proprietary) while keeping the light status in sync across all devices.
- **Range Test**: Visualize the RSSI and other RF performance data on the mobile phone while running the Range Test sample application on a pair of Silicon Labs radio boards.

## Development Features
EFR Connect helps developers create and troubleshoot Bluetooth applications running on Silicon Labs’ BLE hardware. Here’s a rundown of some example functionalities.

**Bluetooth Browser** - A powerful tool to explore the BLE devices around you. Key features include:
- Scan and sort results with a rich data set
- Label favorite devices to surface on the top of scanning results
- Advanced filtering to identify the types of devices you want to find
- Save filters for later use
- Multiple connections
- Bluetooth 5 advertising extensions
- Rename services and characteristics with 128-bit UUIDs (mappings dictionary)
- Over-the-air (OTA) device firmware upgrade (DFU) in reliable and fast modes
- Configurable MTU and connection interval
- All GATT operations

**Bluetooth Advertiser** – Create and enable multiple parallel advertisement sets:
- Legacy and extended advertising
- Configurable advertisement interval, TX Power, primary/secondary PHYs
- Manual advertisement start/stop and stop based on a time/event limit
- Support for multiple AD types

**Bluetooth GATT Configurator** – Create and manipulate multiple GATT databases
- Add services, characteristics and descriptors
- Operate the local GATT from the browser when connected to a device
- Import/export GATT database between the mobile device and Simplicity Studio GATT Configurator

**Bluetooth Interoperability Test** – Verify interoperability between the BLE hardware
 and your mobile device 
- Runs a sequence of BLE operations to verify interoperability
- Export results log



## Additional information
The app can be found on the [Google PlayStore](https://play.google.com/store/apps/details?id=com.siliconlabs.bledemo&hl=en) and [Apple App Store](https://apps.apple.com/us/app/blue-gecko/id1030932759).

[Learn more about EFR Connect BLE mobile app](https://www.silabs.com/developers/efr-connect-mobile-app).

[Release Notes](https://www.silabs.com/developers/efr-connect-mobile-app)

For more information on Silicon Labs product portfolio please visit [www.silabs.com](https://www.silabs.com). 


## License

    Copyright 2021 Silicon Laboratories
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.



