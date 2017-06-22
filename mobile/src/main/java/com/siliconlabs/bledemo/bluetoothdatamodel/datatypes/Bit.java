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

// Bit - It's wrapper for <Bit> xml tag
public class Bit {
    private int index;
    private int size;
    private String name;
    private ArrayList<Enumeration> enumerations;

    public Bit() {
        enumerations = new ArrayList<Enumeration>();
    }

    public Bit(int index, int size, String name, ArrayList<Enumeration> enumerations) {
        this.index = index;
        this.size = size;
        this.name = name;
        this.enumerations = enumerations;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Enumeration> getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(ArrayList<Enumeration> enumerations) {
        this.enumerations = enumerations;
    }
}
