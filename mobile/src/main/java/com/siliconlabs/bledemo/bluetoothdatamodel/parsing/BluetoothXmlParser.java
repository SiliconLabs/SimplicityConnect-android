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
package com.siliconlabs.bledemo.bluetoothdatamodel.parsing;

import android.content.Context;
import android.util.Xml;

import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.BitField;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Service;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.ServiceCharacteristic;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Bit;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Characteristic;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Descriptor;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Enumeration;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Field;

// BluetoothXmlParser - parses Bluetooth xml resources from /assets/xml/ directory
// It is used only once when application is starting
public class BluetoothXmlParser {
    private static final String ns = null;
    private static Object locker = new Object();
    private static BluetoothXmlParser instance = null;

    private Context appContext;
    private ConcurrentHashMap<UUID, Characteristic> characteristics;

    public static BluetoothXmlParser getInstance() {
        if (instance == null) {
            synchronized (locker) {
                if (instance == null) {
                    instance = new BluetoothXmlParser();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        appContext = context;
    }

    // Parses service files
    public HashMap<UUID, Service> parseServices() throws XmlPullParserException, IOException {
        String serviceFiles[] = appContext.getAssets().list(Consts.DIR_SERVICE);
        InputStream in = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        HashMap<UUID, Service> services = new HashMap<UUID, Service>();
        for (String fileName : serviceFiles) {
            try {
                in = appContext.getAssets().open(Consts.DIR_SERVICE + File.separator + fileName);
                parser.setInput(in, null);
                parser.nextTag();
                UUID uuid = readUUID(parser);
                Service service = readService(parser);
                service.setUuid(uuid);
                services.put(uuid, service);
                in.close();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return services;
    }

    // Reads single service file
    private Service readService(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_SERVICE);

        String serviceName = readServiceName(parser);
        String summary = "";
        ArrayList<ServiceCharacteristic> characteristics = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(Consts.TAG_INFORMATIVE_TEXT)) {
                summary = readSummary(parser);
            } else if (name.equals(Consts.TAG_CHARACTERISTICS)) {
                characteristics = readCharacteristics(parser);
            } else {
                skip(parser);
            }
        }
        return new Service(serviceName, summary, characteristics);
    }

    // Reads service characteristics
    private ArrayList<ServiceCharacteristic> readCharacteristics(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTICS);

        ArrayList<ServiceCharacteristic> characteristics = new ArrayList<ServiceCharacteristic>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(Consts.TAG_CHARACTERISTIC)) {
                characteristics.add(readServiceCharacteristic(parser));
            } else {
                skip(parser);
            }
        }

        return characteristics;
    }

    // Reads single service characteristic
    private ServiceCharacteristic readServiceCharacteristic(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC);

        ServiceCharacteristic characteristic = new ServiceCharacteristic();
        characteristic.setName(readName(parser));
        characteristic.setType(readType(parser));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(Consts.TAG_DESCRIPTORS)) {
                characteristic.setDescriptors(readDescriptors(parser));
            } else {
                skip(parser);
            }
        }
        return characteristic;
    }

    // Reads descriptors
    private ArrayList<Descriptor> readDescriptors(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_DESCRIPTORS);

        ArrayList<Descriptor> descriptors = new ArrayList<Descriptor>();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(Consts.TAG_DESCRIPTOR)) {
                descriptors.add(readDescriptor(parser));
            } else {
                skip(parser);
            }
        }

        return descriptors;
    }

    // Reads single descriptor
    private Descriptor readDescriptor(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_DESCRIPTOR);

        Descriptor descriptor = new Descriptor();
        descriptor.setName(readName(parser));
        descriptor.setType(readType(parser));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            skip(parser);
        }

        return descriptor;
    }

    // Reads summary
    private String readSummary(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_INFORMATIVE_TEXT);
        String summary = Consts.EMPTY_STRING;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals(Consts.TAG_SUMMARY)) {
                summary += readText(parser);
                if (parser.getName().equals(Consts.TAG_P)) {
                    summary += readText(parser);
                }
            } else if (name.equals(Consts.TAG_P)) {
                summary += readText(parser);
            } else {
                skip(parser);
            }
        }
        while (parser.getName() == null || !parser.getName().equals(Consts.TAG_INFORMATIVE_TEXT)) {
            parser.next();
        }
        return summary;
    }

    // Reads service name
    private String readServiceName(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_SERVICE);

        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME);
    }

    // Reads uuid
    private UUID readUUID(XmlPullParser parser) throws XmlPullParserException, IOException {

        String uuid = parser.getAttributeValue(null, Consts.ATTRIBUTE_UUID);

        return UUID.fromString(Common.convert16to128UUID(uuid));
    }

    // Reads type
    private String readType(XmlPullParser parser) {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_TYPE);
    }

    // Parses characteristic files
    public ConcurrentHashMap<UUID, Characteristic> parseCharacteristics() throws XmlPullParserException, IOException {
        String characteristicFiles[] = appContext.getAssets().list(Consts.DIR_CHARACTERISTIC);
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        characteristics = new ConcurrentHashMap<UUID, Characteristic>();
        for (String fileName : characteristicFiles) {
            try {
                Characteristic charact = parseCharacteristic(parser, Consts.DIR_CHARACTERISTIC + File.separator
                        + fileName);
                characteristics.put(charact.getUuid(), charact);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return characteristics;
    }

    // Parse single characteristic for given file
    public Characteristic parseCharacteristic(XmlPullParser parser, String fileName) throws XmlPullParserException,
            IOException {
        InputStream in = null;
        in = appContext.getAssets().open(fileName);
        parser.setInput(in, null);
        parser.nextTag();

        UUID uuid = readUUID(parser);
        String type = readType(parser);

        Characteristic charact = readCharacteristic(parser);
        charact.setUuid(uuid);
        charact.setType(type);

        in.close();
        return charact;
    }

    // Reads single characteristic
    private Characteristic readCharacteristic(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC);

        Characteristic characteristic = new Characteristic();

        String characteristicName = readCharacteristicName(parser);
        characteristic.setName(characteristicName);

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_INFORMATIVE_TEXT)) {
                String summary = readSummary(parser);
                characteristic.setSummary(summary);
            } else if (name.equals(Consts.TAG_VALUE)) {
                ArrayList<Field> fields = readFieldValue(parser, characteristic);
            } else {
                skip(parser);
            }
        }

        return characteristic;
    }

    // Reads characteristic fields
    private ArrayList<Field> readFieldValue(XmlPullParser parser, Characteristic characteristic)
            throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_VALUE);

        ArrayList<Field> fields = new ArrayList<Field>();
        characteristic.setFields(fields);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_FIELD)) {
                Field field = readField(parser, characteristic);
                fields.add(field);
                if (field.getReference() != null) {
                    addCharacteristicReference(field, field.getReference());
                }
            } else {
                skip(parser);
            }
        }

        return fields;
    }

    // Reads single field
    private Field readField(XmlPullParser parser, Characteristic characteristic) throws XmlPullParserException,
            IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_FIELD);

        Field field = new Field();

        field.setName(readName(parser));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_FORMAT)) {
                field.setFormat(readFormat(parser));
            } else if (name.equals(Consts.TAG_MINIMUM)) {
                field.setMinimum(readMinimum(parser));
            } else if (name.equals(Consts.TAG_MAXIMUM)) {
                field.setMaximum(readMaximum(parser));
            } else if (name.equals(Consts.TAG_UNIT)) {
                field.setUnit(readUnit(parser));
            } else if (name.equals(Consts.TAG_BITFIELD)) {
                field.setBitfield(readBitField(parser));
            } else if (name.equals(Consts.TAG_ENUMERATIONS)) {
                field.setEnumerations(readEnumerations(parser));
            } else if (name.equals(Consts.TAG_REQUIREMENT)) {
                field.setRequirement(readRequirement(parser));
            } else if (name.equals(Consts.TAG_REFERENCE)) {
                field.setReference(readReference(parser));
            } else {
                skip(parser);
            }
        }
        return field;
    }

    // Reads requirement
    private String readRequirement(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_REQUIREMENT);
        String requirement = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_REQUIREMENT);
        return requirement;
    }

    // Adds characteristic reference to given field
    private void addCharacteristicReference(Field field, String reference)
            throws XmlPullParserException, IOException {

        Characteristic ref = null;
        for (Characteristic charact : characteristics.values()) {
            if (charact.getType().equals(reference)) {
                ref = charact;
            }
        }

        if (ref != null) {
            for (Field fie : ref.getFields()) {
                field.getReferenceFields().add(fie);
            }
        } else {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            Characteristic newCharacteristic = parseCharacteristic(parser, Consts.DIR_CHARACTERISTIC + File.separator
                    + reference.trim() + Consts.FILE_EXTENSION);
            characteristics.put(newCharacteristic.getUuid(), newCharacteristic);

            for (Field fie : newCharacteristic.getFields()) {
                field.getReferenceFields().add(fie);
            }
        }
    }

    // Reads characteristic name
    private String readCharacteristicName(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_CHARACTERISTIC);

        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME);
    }

    // Reads field format
    private String readFormat(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_FORMAT);
        String format = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_FORMAT);
        return format;
    }

    // Reads field minimum value
    private long readMinimum(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_MINIMUM);
        long minimum = readLong(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_MINIMUM);
        return minimum;
    }

    // Reads field maximum value
    private long readMaximum(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_MAXIMUM);
        long maximum = readLong(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_MAXIMUM);
        return maximum;
    }

    // Reads field unit
    private String readUnit(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_UNIT);
        String unit = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_UNIT);
        return unit;
    }

    // Reads field reference
    private String readReference(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_REFERENCE);
        String unit = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, Consts.TAG_REFERENCE);
        return unit;
    }

    // Reads bit field
    private BitField readBitField(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_BITFIELD);

        BitField field = new BitField();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_BIT)) {
                field.getBits().add(readBit(parser));
            } else {
                skip(parser);
            }
        }
        return field;
    }

    // Reads single bit
    private Bit readBit(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_BIT);

        Bit bit = new Bit();
        bit.setIndex(readIndex(parser));
        bit.setSize(readSize(parser));
        bit.setName(readName(parser));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_ENUMERATIONS)) {
                bit.setEnumerations(readEnumerations(parser));
            } else {
                skip(parser);
            }
        }
        return bit;
    }

    // Reads enumerations
    private ArrayList<Enumeration> readEnumerations(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, Consts.TAG_ENUMERATIONS);

        ArrayList<Enumeration> enumerations = new ArrayList<Enumeration>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();
            if (name.equals(Consts.TAG_ENUMERATION)) {
                enumerations.add(readEnumeration(parser));
            } else {
                skip(parser);
            }
        }
        return enumerations;
    }

    // Reads single enumeration
    private Enumeration readEnumeration(XmlPullParser parser) throws XmlPullParserException, IOException {
        Enumeration enumeration = new Enumeration();
        int key = Integer.parseInt(readKey(parser));
        String value = readValue(parser);
        String requires = readRequires(parser);

        enumeration.setKey(key);
        enumeration.setRequires(requires);
        enumeration.setValue(value);

        parser.next();
        return enumeration;
    }

    // Parse descriptors
    public HashMap<UUID, Descriptor> parseDescriptors() throws IOException, XmlPullParserException {

        String descriptorsFiles[] = appContext.getAssets().list(Consts.DIR_DESCRIPTOR);
        InputStream in = null;
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        HashMap<UUID, Descriptor> descriptors = new HashMap<UUID, Descriptor>();

        for (String fileName : descriptorsFiles) {
            try {
                in = appContext.getAssets().open(Consts.DIR_DESCRIPTOR + File.separator + fileName);
                parser.setInput(in, null);
                parser.nextTag();
                UUID uuid = readUUID(parser);
                Descriptor descriptor = readDescriptor(parser);
                descriptor.setUuid(uuid);
                descriptors.put(uuid, descriptor);
                in.close();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
        return descriptors;
    }

    // Reads requires attribute
    private String readRequires(XmlPullParser parser) {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_REQUIRES);
    }

    // Reads index attribute
    private int readIndex(XmlPullParser parser) {
        return Integer.parseInt(parser.getAttributeValue(null, Consts.ATTRIBUTE_INDEX));
    }

    // Reads size attribute
    private int readSize(XmlPullParser parser) {
        return Integer.parseInt(parser.getAttributeValue(null, Consts.ATTRIBUTE_SIZE));
    }

    // Reads key attribute
    private String readKey(XmlPullParser parser) {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_KEY);
    }

    // Reads value attribute
    private String readValue(XmlPullParser parser) {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_VALUE);
    }

    // Reads name attribute
    private String readName(XmlPullParser parser) {
        return parser.getAttributeValue(null, Consts.ATTRIBUTE_NAME);
    }

    // Skips useless xml tags
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
            case XmlPullParser.END_TAG:
                depth--;
                break;
            case XmlPullParser.START_TAG:
                depth++;
                break;
            }
        }
    }

    // Reads text type
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Reads integer type
    private long readLong(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        long result = 0;
        if (parser.next() == XmlPullParser.TEXT) {
            result = Long.parseLong(parser.getText().trim());
            parser.nextTag();
        }
        return result;
    }
}
