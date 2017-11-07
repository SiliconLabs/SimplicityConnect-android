/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.siliconlabs.bledemo.bluetoothdatamodel.datatypes;

import java.util.ArrayList;
import java.util.UUID;

// Service - It's wrapper for <Service> xml tag
public class Service {

    private String name;
    private String summary;
    private UUID uuid;
    private ArrayList<ServiceCharacteristic> characteristics;

    public Service() {
        characteristics = new ArrayList<ServiceCharacteristic>();
    }

    public Service(String serviceName, String summary, ArrayList<ServiceCharacteristic> characteristics) {
        this.name = serviceName;
        this.summary = summary;
        this.characteristics = characteristics;
    }

    public String getName() {
        return name;
    }

    public void setName(String serviceName) {
        this.name = serviceName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setCharacteristics(ArrayList<ServiceCharacteristic> characteristics) {
        this.characteristics = characteristics;
    }

    public ArrayList<ServiceCharacteristic> getCharacteristics() {
        return characteristics;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

}
