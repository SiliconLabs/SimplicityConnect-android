# Blue Gecko for Android 
This is the source code for the Blue Gecko mobile application for Android.

## Overview:

* [Project Overview](#project-overview) - A general overview of the purpose of this project.
* [Project Setup](#project-setup) - How to get set up with the Blue Gecko Android project.
* [Architecture](#architecture) - A general overview of the app's architecture.

## Project Overview

The Silicon Labs Blue Gecko App displays temperature measurements from the Silicon Labs Wireless Starter Kit (SLWSK). The app can also indicate proximity to the SLWSTK using Find Me Profile (FMP) & Proximity Profiles (PXP). Finally, the SLWSK can be configured as a Retail Beacon to send advertisement data to the App.

The current retail version can be found on the [US Google Play store](https://play.google.com/store/apps/details?id=com.siliconlabs.bledemo&hl=en_US).

<p align="center"><img src="images/bluegecko-main.png" width="200"/>
<img src="images/bluegecko-scanner2.png" width="200"/>
<img src="images/bluegecko-thermometer.png" width="200"/>
<img src="images/bluegecko-advdata.png" width="200"/></p>

## Project Setup

In order to load firmware to the device, get set up with [Simplicity Studio](https://www.silabs.com/products/development-tools/software/simplicity-studio).

The application also contains a "hidden" debug button to the immediate right of the navigation bar that becomes visible on TouchDownInside, and contains a local Bluetooth device explorer.

## Architecture

#### Health Thermometer

To test the Health Thermometer, load the `SOC - Thermometer` firmware in Simplicity Studio. Open the app and select the Health Thermometer cell. You will be presented with a popover. You are able to select Blue Gecko devices or Other devices. Blue Geckos is selected by default. Select Other. You should see a thermometer called Thermometer Example in the list. You should be able to select that thermometer and be brought to a temperature readout screen.

#### Bluetooth Beaconing

When you enter the Bluetooth Beaconing portion of the app, the app begins to look for several types of "beacons" including iBeacon, AltBeacons, BGBeacons, and Eddystone Beacons.

#### Key Fobs

In order to test the Key Fob mode, make sure that the `SOC - Smart Phone App` firmware is loaded. Open the app and select Key Fobs. Make sure that the hardware is displaying "HTM/KEYFOB MODE" on its LCD display. If it isn't, press the PB0 button. Then press the FIND button in the app. notice that the two small LEDs between the LCD display and the Blue Gecko board are now flashing! If you back out of the Key Fob screen you will see that the app is disconnecting and the LEDs will stop flashing.

#### Bluetooth Browser

The Bluetooth browser searches for all Bluetooth peripherals in the surrounding area.

## Additional information
For more information, please visit [www.silabs.com](https://www.silabs.com). 





