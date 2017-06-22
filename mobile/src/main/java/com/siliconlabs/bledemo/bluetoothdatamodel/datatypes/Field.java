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

// Field - It's wrapper for <Field> xml tag
public class Field {

    private String name;
    private String unit;
    private String format;
    private String type;
    private String requirement;
    private String reference;
    private long minimum = 0;
    private long maximum = 0;
    private ArrayList<Enumeration> enumerations;
    private BitField bitfield;
    private ArrayList<Field> referenceFields;

    public Field() {
        referenceFields = new ArrayList<Field>();
    }

    public Field(String name, String unit, String format, String type, String requirement, String reference,
            int minimum, int maximum, ArrayList<Enumeration> enumerations, BitField bitfield) {
        this.name = name;
        this.unit = unit;
        this.format = format;
        this.type = type;
        this.minimum = minimum;
        this.maximum = maximum;
        this.enumerations = enumerations;
        this.bitfield = bitfield;
        this.requirement = requirement;
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getMinimum() {
        return minimum;
    }

    public void setMinimum(long minimum) {
        this.minimum = minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public void setMaximum(long maximum) {
        this.maximum = maximum;
    }

    public ArrayList<Enumeration> getEnumerations() {
        return enumerations;
    }

    public void setEnumerations(ArrayList<Enumeration> enumerations) {
        this.enumerations = enumerations;
    }

    public BitField getBitfield() {
        return bitfield;
    }

    public void setBitfield(BitField bitfield) {
        this.bitfield = bitfield;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReference() {
        return reference;
    }

    public void setReferenceFields(ArrayList<Field> referenceFields) {
        this.referenceFields = referenceFields;
    }

    public ArrayList<Field> getReferenceFields() {
        return referenceFields;
    }
}
