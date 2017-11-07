package com.siliconlabs.bledemo.bluetoothdatamodel.datatypes;

import java.util.ArrayList;

// ServiceCharacteristic - It's wrapper for <Characteristic> xml tag from service resources
public class ServiceCharacteristic {

    private String name;
    private String type;
    private ArrayList<Descriptor> descriptors;

    public ServiceCharacteristic() {
        descriptors = new ArrayList<Descriptor>();
    }

    public ServiceCharacteristic(String name, String type, ArrayList<Descriptor> descriptors) {
        this.name = name;
        this.type = type;
        this.descriptors = descriptors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<Descriptor> getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(ArrayList<Descriptor> descriptors) {
        this.descriptors = descriptors;
    }
}
