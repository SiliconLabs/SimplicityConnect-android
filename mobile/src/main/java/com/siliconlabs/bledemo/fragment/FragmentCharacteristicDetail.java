package com.siliconlabs.bledemo.fragment;

import android.app.Dialog;
import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.DeviceServicesActivity;
import com.siliconlabs.bledemo.ble.BluetoothDeviceInfo;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Bit;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Characteristic;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Descriptor;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Enumeration;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Field;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.Service;
import com.siliconlabs.bledemo.bluetoothdatamodel.datatypes.ServiceCharacteristic;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Common;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Consts;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Converters;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Engine;
import com.siliconlabs.bledemo.bluetoothdatamodel.parsing.Unit;
import com.siliconlabs.bledemo.services.BluetoothLeService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static java.lang.StrictMath.abs;

public class FragmentCharacteristicDetail extends Fragment {
    //padding
    public static final int FIELD_CONTAINER_PADDING_LEFT = 10;
    public static final int FIELD_CONTAINER_PADDING_TOP = 15;
    public static final int FIELD_CONTAINER_PADDING_RIGHT = 10;
    public static final int FIELD_CONTAINER_PADDING_BOTTOM = 15;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_LEFT = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_TOP = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_RIGHT = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_BOTTOM = 0;
    //textsize
    public static final int FIELD_NAME_TEXT_SIZE = 12;
    public static final int FIELD_UNIT_TEXT_SIZE = 14;
    public static final int FIELD_VALUE_EDIT_TEXT_SIZE = 14;
    public static final int FIELD_VALUE_TEXT_SIZE = 14;
    public static final int FIELD_VALUE_EDIT_TEXT_TEXT_SIZE = 12;
    public static final int FIELD_VALUE_NAME_TEXT_SIZE = 12;
    public static final int CHECKBOX_LIST_HEADER_TEXT_SIZE = 14;
    public static final int BIT_NAME_VIEW_TEXT_SIZE = 12;
    public static final int PROBLEM_TEXT_SIZE = 12;
    //margins
    public static int FIELD_VALUE_EDIT_LEFT_MARGIN = 15;
    public static final int FIELD_NAME_OF_VALUE_MARGIN_RIGHT = 15;
    public static final int SPINNER_WITH_LABEL_CONTAINER_MARGIN_TOP = 15;
    public static final int FIELD_NAME_MARGIN_BOTTOM = 15;

    private Context context;

    final private int REFRESH_INTERVAL = 500; // miliseconds

    final private String TYPE_FLOAT = "FLOAT";
    final private String TYPE_SFLOAT = "SFLOAT";
    final private String TYPE_FLOAT_32 = "float32";
    final private String TYPE_FLOAT_64 = "float64";

    private BluetoothGattCharacteristic mBluetoothCharact;
    private BluetoothGattService mGattService;
    private Characteristic mCharact;
    private BluetoothLeService mBluetoothLeService;
    private Service mService;
    private List<BluetoothGattDescriptor> mDescriptors;
    private Iterator<BluetoothGattDescriptor> iterDescriptor;
    private BluetoothGattDescriptor lastDescriptor;
    private boolean readable = false;
    private boolean writeable = false;
    private boolean notify = false;
    private boolean notificationsEnabled = false;
    private boolean indicationsEnabled = false;
    private boolean isRawValue = false;
    private boolean parseProblem = false;
    private int offset = 0; // in bytes
    private int currRefreshInterval = REFRESH_INTERVAL; // in seconds
    private byte[] value;
    private BluetoothGatt mDevice;
    private BluetoothDevice bluetoothDevice;
    private BluetoothDeviceInfo bluetoothDeviceInfo;
    private int defaultMargin;
    private boolean foundField = false;
    // the following arraylist is used to check if fields in dialog for editable characteristics are empty, then set enabled stat for save btn
    ArrayList<EditText> editTexts = new ArrayList<>();

    private int viewBackgroundColor = 0;
    public String address;
    public View fragmentRootView;
    public ViewGroup viewGroup;

    private LinearLayout valuesLayout;
    private EditText hexEdit;
    private EditText asciiEdit;
    private EditText decimalEdit;
    private TextView hex;
    private TextView ascii;
    private TextView decimal;
    Dialog editableFieldsDialog;
    LinearLayout writableFieldsContainer;
    Button saveValueBtn;
    HashMap<Field, Boolean> fieldsInRangeMap;
    HashMap<Field, Boolean> fieldsValidMap;

    private boolean writeString = false;

    public FragmentCharacteristicDetail() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO inflate appropriate layout file
        viewBackgroundColor = ContextCompat.getColor(getActivity(), R.color.silabs_white);

        View view = inflater.inflate(R.layout.fragment_characteristic_details, container, false);
        fragmentRootView = view;
        viewGroup = container;
        context = getActivity();
        defaultMargin = getResources().getDimensionPixelSize(R.dimen.characteristic_text_left_margin);

        valuesLayout = view.findViewById(R.id.values_layout);

        mDevice = ((DeviceServicesActivity) getActivity()).getBluetoothGatt();
        mCharact = Engine.getInstance().getCharacteristic(mBluetoothCharact.getUuid());
        mService = Engine.getInstance().getService(mBluetoothCharact.getService().getUuid());
        mDescriptors = new ArrayList<>();
        setProperties();

        Log.d("Charac", mBluetoothCharact.getUuid().toString() + " " + mBluetoothCharact.getInstanceId());

        mBluetoothCharact.getProperties();
        configureWriteable();

        updateBall();

        if (readable) {
            //mBluetoothLeService.readCharacteristic(mDevice, mBluetoothCharact);
            mDevice.readCharacteristic(mBluetoothCharact);
        } else { // Another case prepare empty data and show UI
            if (!isRawValue) {
                prepareValueData();
            }
            loadValueViews();
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //getActivity().unregisterReceiver(mBluetoothLeReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
    }

    // Builds activity UI based on characteristic content
    private void loadValueViews() {
        fieldsInRangeMap = new HashMap<>();
        fieldsValidMap = new HashMap<>();

        valuesLayout.removeAllViews();
        if (!isRawValue) {
            if (parseProblem || !addNormalValue()) {
                addInvalidValue();
            }
        } else {
            addRawValue();
        }
    }

    // Configures characteristic if it is writeable
    private void configureWriteable() {
        if (writeable) {
            initCharacteristicWriteDialog();
        }
    }

    public void onDescriptorWrite(UUID descriptorUuid) {
        if (Common.equalsUUID(descriptorUuid, lastDescriptor.getUuid())) {
            writeNextDescriptor();
        }
    }

    public void onActionDataWrite(String uuid, final int status) {
        if (!mBluetoothCharact.getUuid().toString().equals(uuid)) {
            return;
        }

        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Toast.makeText(getActivity(), getText(R.string.characteristic_write_success),
                            Toast.LENGTH_SHORT).show();
                    ((DeviceServicesActivity) getActivity()).refreshCharacteristicExpansion();
                } else {
                    Toast.makeText(getActivity(), getText(R.string.characteristic_write_fail),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void onActionDataAvailable(String uuidCharacteristic) {
        if (currRefreshInterval >= REFRESH_INTERVAL) {
            if (uuidCharacteristic.equals(mBluetoothCharact.getUuid().toString())) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (currRefreshInterval >= REFRESH_INTERVAL) {
                            currRefreshInterval = 0;
                            offset = 0;
                            value = mBluetoothCharact.getValue();
                            loadValueViews();
                        }
                    }
                });
            }
        }
    }

    public void setmBluetoothCharact(BluetoothGattCharacteristic mBluetoothCharact) {
        this.mBluetoothCharact = mBluetoothCharact;
    }

    public void setmService(BluetoothGattService service) {
        mGattService = service;
    }

    // Sets property members for characteristics
    private void setProperties() {
        if (Common.isSetProperty(Common.PropertyType.READ, mBluetoothCharact.getProperties())) {
            readable = true;
        }

        if (Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact.getProperties())
                || Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact.getProperties())) {
            writeable = true;
        }

        if (Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact.getProperties())
                || Common.isSetProperty(Common.PropertyType.INDICATE, mBluetoothCharact.getProperties())) {
            notify = true;
        }

        if (mCharact == null || mCharact.getFields() == null) {
            isRawValue = true;
        }
    }

    private void writeValueToCharacteristic() {
        EditText hexEdit = editableFieldsDialog.findViewById(R.id.hexEdit);
        if (hexEdit != null) {
            String hex = hexEdit.getText().toString().replaceAll("\\s+", "");
            byte[] newValue = hexToByteArray(hex);
            try {
                if (Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact.getProperties())) {
                    mBluetoothCharact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                } else if (Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact.getProperties())) {
                    mBluetoothCharact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                }

                Log.d("Name", "" + mDevice.getDevice().getName());
                Log.d("Address", "" + mDevice.getDevice().getAddress());
                Log.d("Service", "" + mBluetoothCharact.getService().getUuid());
                Log.d("Charac", "" + mBluetoothCharact.getUuid());
                mBluetoothCharact.setValue(newValue);
                Log.d("hex", "" + Converters.getHexValue(mBluetoothCharact.getValue()));
                mDevice.writeCharacteristic(mBluetoothCharact);
                editableFieldsDialog.dismiss();

            } catch (Exception e) {
                Log.e("Service", "null" + e);
            }
        } else {
            if (possibleToSave()) {
                mBluetoothCharact.setValue(value);
                mDevice.writeCharacteristic(mBluetoothCharact);
                Log.d("write_val", "Standard Value to write (hex): " + Converters.getHexValue(value));
            }
        }
    }

    private boolean possibleToSave() {
        boolean validField = true;
        for (Map.Entry<Field, Boolean> entry : fieldsValidMap.entrySet()) {
            validField = entry.getValue();
            if (!validField) {
                break;
            }
        }

        boolean entryInRange = true;
        for (Map.Entry<Field, Boolean> entry : fieldsInRangeMap.entrySet()) {
            entryInRange = entry.getValue();
            if (!entryInRange) {
                break;
            }
        }

        if (!validField) {
            Toast.makeText(context, context.getString(R.string.characteristic_dialog_invalid_input), Toast.LENGTH_SHORT).show();
            return false;
        } else if (!entryInRange) {
            Toast.makeText(context, context.getString(R.string.characteristic_dialog_invalid_out_of_range), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    // Count time that is used to preventing from very fast refreshing view
    private void updateBall() {
        Timer timer = new Timer();
        TimerTask updateBall = new TimerTask() {

            @Override
            public void run() {
                currRefreshInterval += REFRESH_INTERVAL;
            }
        };
        timer.scheduleAtFixedRate(updateBall, 0, REFRESH_INTERVAL);
    }

    public boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean enabled) {
        notificationsEnabled = enabled;
    }

    public boolean getIndicationsEnabled() {
        return indicationsEnabled;
    }

    public void setIndicationsEnabled(boolean enabled) {
        indicationsEnabled = enabled;
    }

    // Gets all characteristic descriptors
    private ArrayList<Descriptor> getCharacteristicDescriptors() {
        if (mService == null || mCharact == null) {
            return null;
        }

        ArrayList<Descriptor> descriptors = new ArrayList<>();

        for (ServiceCharacteristic charact : mService.getCharacteristics()) {
            if (charact.getType().equals(mCharact.getType())) {
                for (Descriptor descriptor : charact.getDescriptors()) {
                    descriptors.add(Engine.getInstance().getDescriptorByType(descriptor.getType()));
                }
            }
        }
        return descriptors;
    }

    // Checks if given descriptor is available in this characteristic
    private boolean isDescriptorAvailable(ArrayList<Descriptor> descriptors, BluetoothGattDescriptor blDescriptor) {
        for (Descriptor descriptor : descriptors) {
            if (Common.equalsUUID(descriptor.getUuid(), blDescriptor.getUuid())) {
                return true;
            }
        }
        return false;
    }

    // Writes next descriptor in order to enable notification or indication
    protected void writeNextDescriptor() {
        if (iterDescriptor.hasNext()) {
            lastDescriptor = iterDescriptor.next();

            if (lastDescriptor.getCharacteristic() == mBluetoothCharact) {
                lastDescriptor.setValue(Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact
                        .getProperties()) ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                //mBluetoothLeService.writeDescriptor(mDevice, lastDescriptor);
                mDevice.writeDescriptor(lastDescriptor);
            }
        }
    }

    // Builds activity UI in tree steps:
    // a) add views based on characteristic content without setting values
    // b) add problem info view
    // c) add raw views (hex, ASCII, decimal) with setting values
    private void addInvalidValue() {
        valuesLayout.removeAllViews();
        addNormalValue();
        addProblemInfoView();
        addRawValue();
    }

    // Only called when characteristic is standard Bluetooth characteristic
    // Build activity UI based on characteristic content and also take account
    // of field requirements
    private boolean addNormalValue() {
        if (writableFieldsContainer != null) {
            writableFieldsContainer.removeAllViews();
        }
        for (int i = 0; i < mCharact.getFields().size(); i++) {
            try {
                Field field = mCharact.getFields().get(i);
                addField(field);
            } catch (Exception ex) {
                Log.i("CharacteristicUI", String.valueOf(i));
                //Log.i("Characteristic fields size", String.valueOf(mCharact.getFields().size()));
                Log.i("Characteristic value", Converters.getDecimalValue(value));
                parseProblem = true;
                return false;
            }
        }
        return true;
    }

    // Add single field
    private void addField(Field field) {
        if (isFieldPresent(field)) {
            if (field.getReferenceFields().size() > 0) {
                for (Field subField : field.getReferenceFields()) {
                    addField(subField);
                }
            } else {
                if (field.getBitfield() != null) {
                    addBitfield(field);
                } else if (field.getEnumerations() != null && field.getEnumerations().size() > 0) {
                    addEnumeration(field);
                } else {
                    addValue(field);
                }
            }
        }
    }

    // Initializes byte array with empty characteristic content
    private void prepareValueData() {
        int size = characteristicSize();
        if (size != 0) {
            value = new byte[size];
        }
    }

    // Returns characteristic size in bytes
    private int characteristicSize() {
        int size = 0;
        for (Field field : mCharact.getFields()) {
            size += fieldSize(field);
        }
        return size;
    }

    // Returns only one field size in bytes
    private int fieldSize(Field field) {
        String format = field.getFormat();
        if (format != null) {
            return Engine.getInstance().getFormat(format);
        } else if (field.getReferenceFields().size() > 0) {
            int subFieldsSize = 0;
            for (Field subField : field.getReferenceFields()) {
                subFieldsSize += fieldSize(subField);
            }
            return subFieldsSize;
        } else {
            return 0;
        }
    }

    // Checks if field is present based on it's requirements and bitfield
    // settings
    private boolean isFieldPresent(Field field) {
        if (parseProblem) {
            return true;
        }
        if (field.getRequirement() == null || field.getRequirement().equals(Consts.REQUIREMENT_MANDATORY)) {
            return true;
        } else {
            for (Field bitField : getBitFields()) {
                for (Bit bit : bitField.getBitfield().getBits()) {
                    for (Enumeration enumeration : bit.getEnumerations()) {
                        if (enumeration.getRequires() != null
                                && field.getRequirement().equals(enumeration.getRequires())) {
                            return checkRequirement(bitField, enumeration, bit);
                        }
                    }
                }
            }
        }
        return false;
    }

    // Checks requirement on exactly given bitfield, enumeration and bit
    private boolean checkRequirement(Field bitField, Enumeration enumeration, Bit bit) {
        int formatLength = Engine.getInstance().getFormat(bitField.getFormat());
        int off = getFieldOffset(bitField);
        int val = readInt(off, formatLength);
        int enumVal = readEnumInt(bit.getIndex(), bit.getSize(), val);
        return (enumVal == enumeration.getKey());
    }

    /*
     *
     * --- VALUE SETTERS & GETTERS SECTION ---
     */

    // Converts string given in hexadecimal system to byte array
    public byte[] hexToByteArray(String hex) {

        if (hex.length() != 0 && hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        int len = hex.length() / 2;
        byte[] byteArr = new byte[len];
        for (int i = 0; i < byteArr.length; i++) {
            int init = i * 2;
            int end = init + 2;
            int temp = Integer.parseInt(hex.substring(init, end), 16);
            byteArr[i] = (byte) (temp & 0xFF);
        }
        return byteArr;
    }

    // Converts string given in decimal system to byte array
    private byte[] decToByteArray(String dec) {
        if (dec.length() == 0) {
            return new byte[]{};
        }
        String[] decArray = dec.split(" ");
        byte[] byteArr = new byte[decArray.length];

        for (int i = 0; i < decArray.length; i++) {
            try {
                byteArr[i] = (byte) (Integer.parseInt(decArray[i]));
            } catch (NumberFormatException e) {
                return new byte[]{0};
            }
        }
        return byteArr;
    }

    // Converts int to byte array
    private byte[] intToByteArray(int newVal, int formatLength) {
        byte[] val = new byte[formatLength];
        for (int i = 0; i < formatLength; i++) {
            val[i] = (byte) (newVal & 0xff);
            newVal >>= 8;
        }
        return val;
    }

    // Checks if decimal input value is valid
    private boolean isDecValueValid(String decValue) {
        char[] value = decValue.toCharArray();
        int valLength = value.length;
        boolean valid = false;
        if (decValue.length() < 4) {
            valid = true;
        } else {
            valid = value[valLength - 1] == ' ' || value[valLength - 2] == ' ' || value[valLength - 3] == ' '
                    || value[valLength - 4] == ' ';
        }
        return valid;
    }

    // Reads integer value for given offset and field size
    private int readInt(int offset, int size) {
        int val = 0;
        for (int i = 0; i < size; i++) {
            val <<= 8;
            val |= value[offset + i];
        }
        return val;
    }

    // Reads next enumeration value for given enum length
    private int readNextEnum(int formatLength) {
        int result = 0;
        for (int i = 0; i < formatLength; i++) {
            result |= value[offset];
            if (i < formatLength - 1) {
                result <<= 8;
            }
        }
        offset += formatLength;
        return result;
    }

    // Reads next value for given format
    private String readNextValue(String format) {
        if (value == null) {
            return "";
        }

        int formatLength = Engine.getInstance().getFormat(format);

        // binaryString is used for sints, used to fix original bluegiga code ignoring data format type
        StringBuilder binaryString = new StringBuilder();
        try {
            for (int i = offset; i < offset + formatLength; i++) {
                binaryString.append(String.format("%8s", Integer.toBinaryString(value[i] & 0xFF)).replace(' ', '0'));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuilder result = new StringBuilder();
        // If field length equals 0 then reads from offset to end of characteristic data
        if (formatLength == 0) {
            if (format.toLowerCase().equals("reg-cert-data-list")) {
                result = new StringBuilder("0x" + Converters.getHexValue(Arrays.copyOfRange(value, offset, value.length)));
                result = new StringBuilder(result.toString().replace(" ", ""));
            } else {
                result = new StringBuilder(new String(Arrays.copyOfRange(value, offset, value.length)));
            }
            offset += value.length;
        } else {
            // If format type is kind of float type then reads float value
            if (format.equals(TYPE_SFLOAT) || format.equals(TYPE_FLOAT) || format.equals(TYPE_FLOAT_32)
                    || format.equals(TYPE_FLOAT_64)) {
                double fValue = readFloat(format, formatLength);
                result = new StringBuilder(String.format(Locale.US, "%.3f", fValue));
            } else {
                for (int i = offset; i < offset + formatLength; i++) {
                    result.append((int) (value[i] & 0xff));
                }
            }

            offset += formatLength;
        }

        // bluegiga code fix, original source code did not check for sint or uint
        if (format.toLowerCase().startsWith("sint")) {
            try {
                result = new StringBuilder(Converters.getDecimalValueFromTwosComplement(binaryString.toString()));
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (format.toLowerCase().startsWith("uint")) {
            try {
                // note that the (- formatLength) gets the original offset.
                // java uses big endian, payload is little endian
                byte[] bytes = Arrays.copyOfRange(value, offset - formatLength, offset);
                Long uintAsLong = 0L;
                for (int i = 0; i < formatLength; i++) {
                    uintAsLong = uintAsLong << 8;
                    int byteAsInt = (bytes[formatLength - 1 - i] & 0xff);
                    uintAsLong = uintAsLong | byteAsInt;
                }
                String uintVal = formatLength < 9 ? "" + uintAsLong : (new BigInteger("0" + binaryString, 2)).toString(16);
                return "" + uintVal;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }

    // Reads float value for given format
    private double readFloat(String format, int formatLength) {
        double result = 0.0;
        switch (format) {
            case TYPE_SFLOAT:
                result = Common.readSfloat(value, offset, formatLength - 1);
                break;
            case TYPE_FLOAT:
                result = Common.readFloat(value, offset, formatLength - 1);
                break;
            case TYPE_FLOAT_32:
                result = Common.readFloat32(value, offset, formatLength);
                break;
            case TYPE_FLOAT_64:
                result = Common.readFloat64(value, offset, formatLength);
                break;
        }
        return result;
    }

    // Reads enum for given value
    private int readEnumInt(int index, int size, int val) {
        int result = 0;
        for (int i = 0; i < size; i++) {
            result <<= 8;
            result |= ((val >> (index + i)) & 0x1);
        }
        return result;
    }

    // Sets value from offset position
    private void setValue(int off, byte[] val) {
        for (int i = off; i < off + val.length; i++) {
            value[i] = val[i - off];
        }
    }

    // Gets field offset in bytes
    private int getFieldOffset(Field searchField) {
        foundField = false;
        int off = 0;

        for (Field field : mCharact.getFields()) {
            off += getOffset(field, searchField);
        }
        foundField = true;
        return off;
    }

    // Gets field offset when field has references to other fields
    private int getOffset(Field field, Field searchField) {
        int off = 0;
        if (field == searchField) {
            foundField = true;
            return off;
        }
        if (!foundField && isFieldPresent(field)) {
            if (field.getReferenceFields().size() > 0) {
                for (Field subField : field.getReferenceFields()) {
                    off += getOffset(subField, searchField);
                }
            } else {
                if (field.getFormat() != null) {
                    off += Engine.getInstance().getFormat(field.getFormat());
                }
            }
        }

        return off;
    }

    // Gets all bit fields for this characteristic
    private ArrayList<Field> getBitFields() {
        ArrayList<Field> bitFields = new ArrayList<>();
        for (Field field : mCharact.getFields()) {
            bitFields.addAll(getBitField(field));
        }
        return bitFields;
    }

    // Gets bit field when field has references to other fields
    private ArrayList<Field> getBitField(Field field) {
        ArrayList<Field> bitFields = new ArrayList<>();
        if (field.getBitfield() != null) {
            bitFields.add(field);
        } else if (field.getReferenceFields().size() > 0) {
            for (Field subField : field.getReferenceFields()) {
                bitFields.addAll(getBitField(subField));
            }
        }
        return bitFields;
    }

    /*
     *
     * --- UI SECTION
     */

    // Builds activity UI if characteristic is not standard characteristic (from
    // Bluetooth specifications)
    private void addRawValue() {
        // read only fields and value display for characteristic (inline)
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View readableFieldsForInline = layoutInflater.inflate(R.layout.characteristic_value_read_only, null);

        hex = readableFieldsForInline.findViewById(R.id.hex_readonly);
        ascii = readableFieldsForInline.findViewById(R.id.ascii_readonly);
        decimal = readableFieldsForInline.findViewById(R.id.decimal_readonly);

        hexEdit = readableFieldsForInline.findViewById(R.id.hex_edit_readonly);
        asciiEdit = readableFieldsForInline.findViewById(R.id.ascii_edit_readonly);
        decimalEdit = readableFieldsForInline.findViewById(R.id.decimalEdit_readonly);

        hex.setKeyListener(null);
        ascii.setKeyListener(null);
        decimal.setKeyListener(null);
        hexEdit.setKeyListener(null);
        asciiEdit.setKeyListener(null);
        decimalEdit.setKeyListener(null);

        editTexts.add(hexEdit);
        editTexts.add(asciiEdit);
        editTexts.add(decimalEdit);

        if (writeable) {
            hex.setVisibility(View.GONE);
            ascii.setVisibility(View.GONE);
            decimal.setVisibility(View.GONE);

            hexEdit.setText(Converters.getHexValue(value));
            asciiEdit.setText(Converters.getAsciiValue(value));
            decimalEdit.setText(Converters.getDecimalValue(value));
        } else {
            hexEdit.setVisibility(View.GONE);
            asciiEdit.setVisibility(View.GONE);
            decimalEdit.setVisibility(View.GONE);

            hex.setText(Converters.getHexValue(value));
            ascii.setText(Converters.getAsciiValue(value));
            decimal.setText(Converters.getDecimalValue(value));
        }
        valuesLayout.addView(readableFieldsForInline);

        // writable fields for dialog
        View writableFieldsForDialog = layoutInflater.inflate(R.layout.characteristic_value, null);

        hex = writableFieldsForDialog.findViewById(R.id.hex);
        ascii = writableFieldsForDialog.findViewById(R.id.ascii);
        decimal = writableFieldsForDialog.findViewById(R.id.decimal);

        hexEdit = writableFieldsForDialog.findViewById(R.id.hexEdit);
        asciiEdit = writableFieldsForDialog.findViewById(R.id.asciiEdit);
        decimalEdit = writableFieldsForDialog.findViewById(R.id.decimalEdit);
        editTexts.add(hexEdit);
        editTexts.add(asciiEdit);
        editTexts.add(decimalEdit);


        TextWatcher hexWatcher = getHexTextWatcher();
        TextWatcher decWatcher = getDecTextWatcher();
        TextWatcher asciiWatcher = getAsciiTextWatcher();

        View.OnFocusChangeListener hexListener = getHexFocusChangeListener();

        hexEdit.setOnFocusChangeListener(hexListener);
        WriteCharacteristic commiter = new WriteCharacteristic();
        hexEdit.setOnEditorActionListener(commiter);
        asciiEdit.setOnEditorActionListener(commiter);
        decimalEdit.setOnEditorActionListener(commiter);

        hexEdit.addTextChangedListener(hexWatcher);
        asciiEdit.addTextChangedListener(asciiWatcher);
        decimalEdit.addTextChangedListener(decWatcher);

        if (writeable) {
            hex.setVisibility(View.GONE);
            ascii.setVisibility(View.GONE);
            decimal.setVisibility(View.GONE);
        } else {
            hexEdit.setVisibility(View.GONE);
            asciiEdit.setVisibility(View.GONE);
            decimalEdit.setVisibility(View.GONE);

            hex.setText(Converters.getHexValue(value));
            ascii.setText(Converters.getAsciiValue(value));
            decimal.setText(Converters.getDecimalValue(value));
        }

        updateSaveButtonState();

        if (writableFieldsContainer != null) {
            writableFieldsContainer.removeAllViews();
            writableFieldsContainer.addView(writableFieldsForDialog);
        }
    }

    private void initCharacteristicWriteDialog() {
        editableFieldsDialog = new Dialog(getActivity());
        editableFieldsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editableFieldsDialog.setContentView(R.layout.dialog_characteristic_write);
        editableFieldsDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        writableFieldsContainer = editableFieldsDialog.findViewById(R.id.characteristic_writable_fields_container);

        saveValueBtn = editableFieldsDialog.findViewById(R.id.save_btn);
    }

    public void showCharacteristicWriteDialog() {
        // if any textfields are empty, save rounded_button_red will be initialized to be disabled
        updateSaveButtonState();

        saveValueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeValueToCharacteristic();
            }
        });

        Button cancelBtn = editableFieldsDialog.findViewById(R.id.cancel_btn);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (EditText et : editTexts) {
                    et.setText("");
                }
            }
        });

        String serviceName = mService != null ? mService.getName().trim() : getString(R.string.unknown_service);
        String characteristicName = mCharact != null ? mCharact.getName().trim() : getString(R.string.unknown_characteristic_label);
        String characteristicUuid = mCharact != null ? Common.getUuidText(mCharact.getUuid()) : Common.getUuidText(mBluetoothCharact.getUuid());

        TextView serviceNameTextView = editableFieldsDialog.findViewById(R.id.picker_dialog_service_name);
        TextView characteristicTextView = editableFieldsDialog.findViewById(R.id.characteristic_dialog_characteristic_name);
        TextView uuidTextView = editableFieldsDialog.findViewById(R.id.picker_dialog_characteristic_uuid);
        serviceNameTextView.setText(serviceName);
        characteristicTextView.setText(characteristicName);
        uuidTextView.setText(characteristicUuid);

        LinearLayout propertiesContainer = editableFieldsDialog.findViewById(R.id.picker_dialog_properties_container);
        initPropertiesForEditableFieldsDialog(propertiesContainer);

        editableFieldsDialog.show();
        updateSaveButtonState();
    }

    private void initPropertiesForEditableFieldsDialog(LinearLayout propertiesContainer) {
        propertiesContainer.removeAllViews();
        String propertiesString = Common.getProperties(getActivity(), mBluetoothCharact.getProperties());
        String[] propsExploded = propertiesString.split(",");
        for (String propertyValue : propsExploded) {
            TextView propertyView = new TextView(context);
            String propertyValueTrimmed = propertyValue.trim();
            // length 13 is used to cut off property string at length "Write no resp"
            propertyValueTrimmed = propertyValue.length() > 13 ? propertyValue.substring(0, 13) : propertyValueTrimmed;
            propertyValueTrimmed.toUpperCase();
            propertyView.setText(propertyValueTrimmed);
            propertyView.append("  ");
            propertyView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_property_text_size));
            propertyView.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
            propertyView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

            LinearLayout propertyContainer = new LinearLayout(context);
            propertyContainer.setOrientation(LinearLayout.HORIZONTAL);

            ImageView propertyIcon = new ImageView(context);
            int iconId;
            if (propertyValue.trim().toUpperCase().equals("BROADCAST")) {
                iconId = R.drawable.debug_prop_broadcast;
            } else if (propertyValue.trim().toUpperCase().equals("READ")) {
                iconId = R.drawable.ic_icon_read_on;
            } else if (propertyValue.trim().toUpperCase().equals("WRITE NO RESPONSE")) {
                iconId = R.drawable.debug_prop_write_no_resp;
            } else if (propertyValue.trim().toUpperCase().equals("WRITE")) {
                iconId = R.drawable.ic_icon_edit_on;
            } else if (propertyValue.trim().toUpperCase().equals("NOTIFY")) {
                iconId = R.drawable.ic_icon_notify_on;
            } else if (propertyValue.trim().toUpperCase().equals("INDICATE")) {
                iconId = R.drawable.ic_icon_indicate_on;
            } else if (propertyValue.trim().toUpperCase().equals("SIGNED WRITE")) {
                iconId = R.drawable.debug_prop_signed_write;
            } else if (propertyValue.trim().toUpperCase().equals("EXTENDED PROPS")) {
                iconId = R.drawable.debug_prop_ext;
            } else {
                iconId = R.drawable.debug_prop_ext;
            }
            propertyIcon.setBackgroundResource(iconId);

            LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsText.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;

            LinearLayout.LayoutParams paramsIcon = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsIcon.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

            if(propertyValue.trim().toUpperCase().equals("WRITE NO RESPONSE")) {
                float d = getResources().getDisplayMetrics().density;
                paramsIcon = new LinearLayout.LayoutParams((int)(24*d),((int)(24*d)));
                paramsIcon.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
            }

            propertyContainer.addView(propertyView, paramsText);
            propertyContainer.addView(propertyIcon, paramsIcon);

            LinearLayout.LayoutParams paramsTextAndIconContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            paramsTextAndIconContainer.gravity = Gravity.RIGHT;
            propertiesContainer.addView(propertyContainer, paramsTextAndIconContainer);
        }
    }

    // Gets text watcher for hex edit view
    private TextWatcher getHexTextWatcher() {
        return new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (hexEdit.hasFocus()) {
                    int textLength = hexEdit.getText().toString().length();

                    byte[] newValue;
                    if (textLength % 2 == 1) {
                        String temp = hexEdit.getText().toString();
                        temp = temp.substring(0, textLength - 1) + "0" + temp.charAt(textLength - 1);
                        newValue = hexToByteArray(temp.replaceAll("\\s+", ""));
                    } else {
                        newValue = hexToByteArray(hexEdit.getText().toString().replaceAll("\\s+", ""));
                    }
                    asciiEdit.setText(Converters.getAsciiValue(newValue));
                    decimalEdit.setText(Converters.getDecimalValue(newValue));
                }

                updateSaveButtonState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        };
    }

    // Gets text watcher for decimal edit view
    private TextWatcher getDecTextWatcher() {
        return new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (decimalEdit.hasFocus()) {
                    if (isDecValueValid(decimalEdit.getText().toString())) {
                        byte[] newValue = decToByteArray(decimalEdit.getText().toString());
                        hexEdit.setText(Converters.getHexValue(newValue));
                        asciiEdit.setText(Converters.getAsciiValue(newValue));
                    } else {
                        decimalEdit.setText(decimalEdit.getText().toString().substring(0,
                                decimalEdit.getText().length() - 1));
                        decimalEdit.setSelection(decimalEdit.getText().length());
                        Toast.makeText(context, R.string.invalid_dec_value, Toast.LENGTH_SHORT)
                                .show();
                    }
                }

                updateSaveButtonState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        };
    }

    // Gets text watcher for ascii edit view
    private TextWatcher getAsciiTextWatcher() {
        TextWatcher watcher = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (asciiEdit.hasFocus()) {
                    byte[] newValue = asciiEdit.getText().toString().getBytes();
                    hexEdit.setText(Converters.getHexValue(newValue));
                    decimalEdit.setText(Converters.getDecimalValue(newValue));
                }

                updateSaveButtonState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        };
        return watcher;
    }

    // Gets focus listener for hex edit view
    private View.OnFocusChangeListener getHexFocusChangeListener() {
        View.OnFocusChangeListener listener = new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    hexEdit.setText(hexEdit.getText().toString().replaceAll("\\s+", ""));
                } else {
                    int textLength = hexEdit.getText().toString().length();
                    String hexValue;
                    if (textLength % 2 == 1) {
                        String temp = hexEdit.getText().toString();
                        hexValue = temp.substring(0, textLength - 1) + "0" + temp.charAt(textLength - 1);
                    } else {
                        hexValue = hexEdit.getText().toString();
                    }
                    byte[] value = hexToByteArray(hexValue);
                    hexEdit.setText(Converters.getHexValue(value));
                }
                updateSaveButtonState();
            }
        };
        return listener;
    }

    // Adds views related to single field value
    private void addValue(final Field field) {

        LinearLayout parentLayout = new LinearLayout(context);
        LinearLayout.LayoutParams parentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        parentLayout.setOrientation(LinearLayout.VERTICAL);

        parentParams.setMargins(0, defaultMargin, 0, defaultMargin / 2);
        parentLayout.setLayoutParams(parentParams);

        LinearLayout valueLayout = addValueLayout();
        TextView fieldNameView = addValueFieldName(field.getName(), valueLayout.getId());
        fieldNameView.setGravity(Gravity.CENTER_VERTICAL);
        fieldNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_label_text_size));
        TextView fieldUnitView = addValueUnit(field);
        fieldUnitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_label_text_size));
        fieldUnitView.setGravity(Gravity.CENTER_VERTICAL);

        if (!parseProblem && field.getReference() == null) {
            String format = field.getFormat();
            String val = readNextValue(format);


            if (!(format.toLowerCase().equals("utf8s") || format.toLowerCase().equals("utf16s"))) {
                int decimalExponentAbs = (int) abs(field.getDecimalExponent());
                double divider = Math.pow(10, decimalExponentAbs);
                double valDouble = Double.parseDouble(val);
                double valTmp = valDouble / divider;
                val = Double.toString(valTmp);
            } else {
                writeString = true;
                val = val.replace("\0", "");
            }


            if (writeable) {
                // inline field value
                EditText fieldValueEdit = addValueEdit(field, val);
                fieldValueEdit.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                params.setMargins(5, 0, 0, 0);
                params.gravity = Gravity.CENTER_VERTICAL;
                fieldValueEdit.setLayoutParams(params);
                fieldValueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
                editTexts.add(fieldValueEdit);
                TextView fieldValue = (TextView) addFieldName(fieldValueEdit.getText().toString());
                fieldValue.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));
                valueLayout.addView(fieldValue);

                // dialog field value
                // field name
                View fieldName = addFieldName(field.getName());
                params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                params.gravity = Gravity.CENTER_VERTICAL;
                fieldName.setLayoutParams(params);
                // container for editable field value and field name
                LinearLayout fieldContainer = new LinearLayout(context);
                fieldContainer.setOrientation(LinearLayout.HORIZONTAL);
                params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 5, 0, 5);
                fieldContainer.setLayoutParams(params);
                fieldContainer.addView(fieldName);
                fieldContainer.addView(fieldValueEdit);
                fieldContainer.setPadding(FIELD_CONTAINER_PADDING_LEFT, FIELD_CONTAINER_PADDING_TOP, FIELD_CONTAINER_PADDING_RIGHT, FIELD_CONTAINER_PADDING_BOTTOM);
                writableFieldsContainer.addView(fieldContainer);
            } else {
                TextView fieldValueView = addValueText(val);
                fieldValueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
                valueLayout.addView(fieldValueView);
            }

            updateSaveButtonState();
        }

        valueLayout.addView(fieldUnitView);
        parentLayout.addView(valueLayout);
        parentLayout.addView(fieldNameView);
        valuesLayout.addView(parentLayout);
    }

    // Adds parent layout for normal value
    private LinearLayout addValueLayout() {
        LinearLayout valueLayout = new LinearLayout(context);
        valueLayout.setBackgroundColor(viewBackgroundColor);
        RelativeLayout.LayoutParams valueLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        valueLayoutParams.setMargins(0, 0, defaultMargin, 0);
        valueLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        valueLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        valueLayout.setLayoutParams(valueLayoutParams);
        valueLayout.setOrientation(LinearLayout.HORIZONTAL);
        //noinspection ResourceType
        valueLayout.setId(2);

        return valueLayout;
    }

    // Adds unit text view
    private TextView addValueUnit(Field field) {
        TextView fieldUnitView = new TextView(context);
        fieldUnitView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        fieldUnitView.setBackgroundColor(viewBackgroundColor);
        LinearLayout.LayoutParams fieldUnitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldUnitParams.setMargins(0, 0, 0, 0);
        fieldUnitView.setLayoutParams(fieldUnitParams);

        Unit unit = Engine.getInstance().getUnit(field.getUnit());
        if (unit != null) {
            if (!TextUtils.isEmpty(unit.getSymbol())) {
                if (unit.getFullName().toLowerCase().equals("celsius") || unit.getFullName().toLowerCase().equals("fahrenheit")) {
                    // this makes sure that the degrees symbol for temperature is displayed correctly
                    fieldUnitView.setText(Html.fromHtml(unit.getSymbol()));
                } else {
                    fieldUnitView.setText(unit.getSymbol());
                }
            } else {
                fieldUnitView.setText(unit.getFullName());
            }
        }

        fieldUnitView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));
        return fieldUnitView;
    }


    private boolean isNumberFormat(String format) {
        switch (format) {
            case "uint8":
            case "uint16":
            case "uint24":
            case "uint32":
            case "uint40":
            case "uint48":
            case "sint8":
            case "sint16":
            case "sint24":
            case "sint32":
            case "sint40":
            case "sint48":
            case "float32":
            case "float64":
                return true;
            case "utf8s":
            case "utf16s":
            default:
                return false;
        }
    }

    // Adds value edit view
    private EditText addValueEdit(final Field field, String value) {

        final EditText fieldValueEdit = new EditText(context);
        fieldValueEdit.setBackgroundResource(R.drawable.edittext_custom_color);
        fieldValueEdit.setPadding(FIELD_VALUE_EDIT_TEXT_PADDING_LEFT, FIELD_VALUE_EDIT_TEXT_PADDING_TOP, FIELD_VALUE_EDIT_TEXT_PADDING_RIGHT, FIELD_VALUE_EDIT_TEXT_PADDING_BOTTOM);

        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldValueParams.gravity = Gravity.CENTER_VERTICAL;
        fieldValueParams.leftMargin = FIELD_VALUE_EDIT_LEFT_MARGIN;

        fieldValueEdit.setLayoutParams(fieldValueParams);
        fieldValueEdit.setTextColor(context.getColor(R.color.silabs_primary_text));
        fieldValueEdit.setSingleLine();
        fieldValueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        fieldValueEdit.setText(value);

        int formatLength = Engine.getInstance().getFormat(field.getFormat());
        // Bluegiga code had a bug where formatlength = 0 fields were ignored on write
        if (formatLength == 0) {
            formatLength = value.length();
        }
        final byte[] valArr = new byte[formatLength];

        if (isNumberFormat(field.getFormat())) {
            fieldValueEdit.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        }

        fieldValueEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (writeString) {
                    int textSize = fieldValueEdit.getText().toString().length();
                    byte[] array = fieldValueEdit.getText().toString().getBytes();

                    if (valArr.length > textSize) {
                        Arrays.fill(valArr, (byte) 0);
                        System.arraycopy(array, 0, valArr, 0, textSize);
                        fillValue(valArr);
                    } else {
                        fillValue(array);
                    }

                } else {
                    Arrays.fill(valArr, (byte) 0);
                    String inputVal = fieldValueEdit.getText().toString();

                    int decimalExponentAbs = (int) abs(field.getDecimalExponent());
                    StringBuilder inputValMoved = new StringBuilder(inputVal);
                    if (decimalExponentAbs != 0) {
                        int index = inputValMoved.indexOf(".");
                        if (index != -1) {
                            if (inputValMoved.length() - 1 - index > decimalExponentAbs) {
                                inputValMoved = new StringBuilder(inputValMoved.toString().replace(".", ""));
                                inputValMoved = new StringBuilder(inputValMoved.substring(0, index + decimalExponentAbs) + "." + inputValMoved.substring(index + decimalExponentAbs));
                            } else if (inputValMoved.length() - 1 - index == decimalExponentAbs) {
                                inputValMoved = new StringBuilder(inputValMoved.toString().replace(".", ""));
                            } else {
                                inputValMoved = new StringBuilder(inputValMoved.toString().replace(".", ""));
                                for (int i = inputValMoved.length() - index; i < decimalExponentAbs; i++) {
                                    inputValMoved.append("0");
                                }
                            }
                        } else {
                            for (int i = 0; i < decimalExponentAbs; i++) {
                                inputValMoved.append("0");
                            }
                        }
                    }

                    Pair<byte[], Boolean> pair = Converters.convertStringTo(inputValMoved.toString(), field.getFormat());
                    byte[] newVal = pair.first;
                    Boolean inRange = pair.second;


                    Log.d("write_val", "Value to write from edittext conversion (hex): " + Converters.getHexValue(newVal));

                    for (int i = 0; i < valArr.length; i++) {
                        if (i < newVal.length) {
                            valArr[i] = newVal[i];
                        }
                    }

                    int off = getFieldOffset(field);

                    fieldsInRangeMap.put(field, inRange);
                    if (isNumberFormat(field.getFormat())) {
                        fieldsValidMap.put(field, isNumeric(inputVal));
                    } else {
                        fieldsValidMap.put(field, true);
                    }

                    setValue(off, valArr);
                }
                updateSaveButtonState();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButtonState();
            }
        });

        return fieldValueEdit;
    }

    private void fillValue(byte[] array) {
        value = new byte[array.length];
        System.arraycopy(array, 0, value, 0, array.length);
    }

    private void updateSaveButtonState() {
        boolean emptyFieldExists = true;
        for (EditText editableField : editTexts) {
            emptyFieldExists = !editableField.getText().toString().equals("");
            Log.d("editableField", " Empty: " + editableField.getText().toString().equals(""));
        }

        if (saveValueBtn != null) {
            saveValueBtn.setEnabled(emptyFieldExists);
            saveValueBtn.setClickable(emptyFieldExists);
        }
    }

    // Adds value text view
    private TextView addValueText(String value) {
        TextView fieldValueView = new TextView(context);
        fieldValueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        fieldValueView.setBackgroundColor(viewBackgroundColor);
        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        fieldValueView.setLayoutParams(fieldValueParams);
        fieldValueView.setText(value);

        fieldValueView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));
        return fieldValueView;
    }

    // Adds TextView with field name
    private TextView addValueFieldName(String name, int leftViewId) {
        TextView fieldNameView = new TextView(context);
        fieldNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_label_text_size));

        fieldNameView.setBackgroundColor(viewBackgroundColor);
        fieldNameView.setText(name);

        RelativeLayout.LayoutParams fieldNameParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        fieldNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        fieldNameParams.addRule(RelativeLayout.LEFT_OF, leftViewId);
        fieldNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
        fieldNameParams.setMargins(0, 0, 0, 15);

        fieldNameView.setLayoutParams(fieldNameParams);

        fieldNameView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_subtle_text));
        return fieldNameView;
    }

    // Adds views related to bitfield value
    // Each bit is presented as CheckBox view
    private void addBitfield(Field field) {

        TextView checkboxListHeader = (TextView) addFieldName(field.getName());
        checkboxListHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        checkboxListHeader.setTextColor(Color.BLACK);
        valuesLayout.addView(checkboxListHeader);

        if (field.getReference() == null) {

            String format = field.getFormat();
            final int formatLength = Engine.getInstance().getFormat(format);
            final int off = getFieldOffset(field);
            final int fieldValue = readNextEnum(formatLength);

            for (final Bit bit : field.getBitfield().getBits()) {
                RelativeLayout parentLayout = new RelativeLayout(context);
                parentLayout.setBackgroundColor(viewBackgroundColor);
                parentLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout bitsLayout = new LinearLayout(context);
                bitsLayout.setBackgroundColor(viewBackgroundColor);
                //noinspection ResourceType
                bitsLayout.setId(1);
                RelativeLayout.LayoutParams bitsLayoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bitsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                bitsLayout.setLayoutParams(bitsLayoutParams);
                bitsLayout.setOrientation(LinearLayout.HORIZONTAL);

                TextView bitNameView = new TextView(context);
                bitNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_label_text_size));
                bitNameView.setBackgroundColor(viewBackgroundColor);
                RelativeLayout.LayoutParams bitNameParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bitNameParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                bitNameParams.addRule(RelativeLayout.CENTER_VERTICAL);
                bitNameParams.addRule(RelativeLayout.LEFT_OF, bitsLayout.getId());
                bitNameParams.setMargins(0, 0, 0, 0);
                bitNameView.setLayoutParams(bitNameParams);
                bitNameView.setText(bit.getName());
                bitNameView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));

                for (int i = 0; i < Math.pow(2, bit.getSize() - 1); i++) {
                    CheckBox checkBox = new CheckBox(context);
                    checkBox.setBackgroundColor(viewBackgroundColor);
                    checkBox.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

                    if (!parseProblem) {
                        checkBox.setEnabled(writeable);
                        checkBox.setChecked(Common.isBitSet(bit.getIndex() + i, fieldValue));

                        final int whichBit = i;

                        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                int newVal = Common.toggleBit(bit.getIndex() + whichBit, fieldValue);
                                byte[] val = intToByteArray(newVal, formatLength);
                                setValue(off, val);
                            }

                        });
                    } else {
                        checkBox.setEnabled(false);
                    }

                    bitsLayout.addView(checkBox);
                }

                parentLayout.addView(bitNameView);
                parentLayout.addView(bitsLayout);

                valuesLayout.addView(parentLayout);
            }
        }

        View spacingAferCheckboxes = new View(context);
        valuesLayout.addView(spacingAferCheckboxes, 1, 15);
    }

    // Adds views related to enumeration value
    // Each enumeration is presented as Spinner view
    private void addEnumeration(final Field field) {
        if (field.getReference() == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_characteristic_picker);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                if (!context.getResources().getBoolean(R.bool.isTablet)) {
                    int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.80);
                    dialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
                }
            }


            final RadioGroup radioGroup = dialog.findViewById(R.id.characteristic_dialog_radio_group);
            Button selectValueBtn = dialog.findViewById(R.id.confirm_selection_btn);

            String serviceName = mService != null ? mService.getName().trim() : getString(R.string.unknown_characteristic_label);
            String characteristicName = mCharact != null ? mCharact.getName().trim() : getString(R.string.unknown_characteristic_label);
            String characteristicUuid = (mCharact != null ? Common.getUuidText(mCharact.getUuid()) : getString(R.string.unknown_characteristic_uuid_label));

            ((TextView) dialog.findViewById(R.id.picker_dialog_service_name)).setText(serviceName);
            dialog.findViewById(R.id.picker_dialog_service_name).setSelected(true);
            ((TextView) dialog.findViewById(R.id.characteristic_dialog_characteristic_name)).setText(characteristicName);
            dialog.findViewById(R.id.characteristic_dialog_characteristic_name).setSelected(true);
            ((TextView) dialog.findViewById(R.id.picker_dialog_characteristic_uuid)).setText(characteristicUuid);
            dialog.findViewById(R.id.picker_dialog_characteristic_uuid).setSelected(true);

            LinearLayout propertiesContainer = dialog.findViewById(R.id.picker_dialog_properties_container);
            String propertiesString = Common.getProperties(getActivity(), mBluetoothCharact.getProperties());
            String[] propsExploded = propertiesString.split(",");
            for (String propertyValue : propsExploded) {
                TextView propertyView = new TextView(context);
                String propertyValueTrimmed = propertyValue.trim();
                propertyValueTrimmed = propertyValue.length() > 13 ? propertyValue.substring(0, 13) : propertyValueTrimmed;
                propertyValueTrimmed.toUpperCase();
                propertyView.setText(propertyValueTrimmed);
                propertyView.append("  ");
                propertyView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.silabs_dialog_title_background));
                propertyView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_property_text_size));
                propertyView.setTextColor(ContextCompat.getColor(context, R.color.silabs_blue));
                propertyView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

                LinearLayout propertyContainer = new LinearLayout(context);
                propertyContainer.setOrientation(LinearLayout.HORIZONTAL);

                ImageView propertyIcon = new ImageView(context);
                int iconId;
                if (propertyValue.trim().toUpperCase().equals("BROADCAST")) {
                    iconId = R.drawable.debug_prop_broadcast;
                } else if (propertyValue.trim().toUpperCase().equals("READ")) {
                    iconId = R.drawable.ic_icon_read_on;
                } else if (propertyValue.trim().toUpperCase().equals("WRITE NO RESPONSE")) {
                    iconId = R.drawable.debug_prop_write_no_resp;
                } else if (propertyValue.trim().toUpperCase().equals("WRITE")) {
                    iconId = R.drawable.ic_icon_edit_on;
                } else if (propertyValue.trim().toUpperCase().equals("NOTIFY")) {
                    iconId = R.drawable.ic_icon_notify_on;
                } else if (propertyValue.trim().toUpperCase().equals("INDICATE")) {
                    iconId = R.drawable.ic_icon_indicate_on;
                } else if (propertyValue.trim().toUpperCase().equals("SIGNED WRITE")) {
                    iconId = R.drawable.debug_prop_signed_write;
                } else if (propertyValue.trim().toUpperCase().equals("EXTENDED PROPS")) {
                    iconId = R.drawable.debug_prop_ext;
                } else {
                    iconId = R.drawable.debug_prop_ext;
                }
                propertyIcon.setBackgroundResource(iconId);

                LinearLayout.LayoutParams paramsText = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                paramsText.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;

                LinearLayout.LayoutParams paramsIcon = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                paramsIcon.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;

                if(propertyValue.trim().toUpperCase().equals("WRITE NO RESPONSE")) {
                    float d = getResources().getDisplayMetrics().density;
                    paramsIcon = new LinearLayout.LayoutParams((int)(24*d),((int)(24*d)));
                    paramsIcon.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
                }

                propertyContainer.addView(propertyView, paramsText);
                propertyContainer.addView(propertyIcon, paramsIcon);

                LinearLayout.LayoutParams paramsTextAndIconContainer = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                paramsTextAndIconContainer.gravity = Gravity.RIGHT;
                paramsTextAndIconContainer.setMargins(0, defaultMargin, 0, defaultMargin);
                propertiesContainer.addView(propertyContainer, paramsTextAndIconContainer);
            }

            ArrayList<String> enumerationArray = new ArrayList<>();

            for (Enumeration en : field.getEnumerations()) {
                enumerationArray.add(en.getValue());

                RadioButton radioButton = new RadioButton(context);
                radioButton.setText(en.getValue());
                radioGroup.addView(radioButton);
            }

            if (!parseProblem) {
                radioGroup.setEnabled(writeable);
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    radioGroup.getChildAt(i).setClickable(writeable);
                }

                int off = getFieldOffset(field);
                int formatLength = Engine.getInstance().getFormat(field.getFormat());

                int pos = 0;

                int val = 0;
                if (field.getFormat().toLowerCase().equals("16bit")) {
                    if (offset == value.length - 1) {
                        // case for when only 8 bits of 16 are sent
                        val = value[offset] & 0xff;
                        val = val << 8;
                    } else if (offset < value.length - 1) {
                        // for field "Category, last 6 bits of payload are used for sub categories
                        if (field.getName().equals("Category")) {
                            int byte1 = value[offset] & 0xff;
                            int byte2 = value[offset + 1] & 0xff;
                            val = (byte2 << 8) | byte1;
                            val = 0xffc0 & val;
                        } else {
                            // case for when 16 full bits are sent
                            val = value[offset] & 0xff;
                            val = val << 8;
                            val = val | (value[offset + 1] & 0xff);
                        }
                    }
                } else {
                    val = readInt(offset, formatLength);
                }

                // Bluegiga code was using getFieldOffset() and getting wrong offset
                // this ensures that fields are consistently offset while reading characteristic
                offset += formatLength;

                if (val != 0) {
                    // value was read or notified
                    for (Enumeration en : field.getEnumerations()) {
                        if (en.getKey() == val) {
                            break;
                        }
                        pos++;
                    }
                }

                if (pos >= enumerationArray.size()) {
                    pos = 0;
                }

                radioGroup.getChildAt(pos).setSelected(true);

                ((RadioButton) radioGroup.getChildAt(pos)).setChecked(true);
                selectValueBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (writeable) {
                            int radioButtonID = radioGroup.getCheckedRadioButtonId();
                            View radioButton = radioGroup.findViewById(radioButtonID);
                            int position = radioGroup.indexOfChild(radioButton);

                            int key = field.getEnumerations().get(position).getKey();
                            int off = getFieldOffset(field);
                            int formatLength = Engine.getInstance().getFormat(field.getFormat());
                            byte[] val = intToByteArray(key, formatLength);
                            setValue(off, val);

                            writeValueToCharacteristic();
                        }

                        dialog.dismiss();
                    }
                });
            } else {
                radioGroup.setEnabled(false);
                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    radioGroup.getChildAt(i).setClickable(false);
                }
            }

            LinearLayout spinnerWithLabelContainer = new LinearLayout(context);
            spinnerWithLabelContainer.setGravity(Gravity.CENTER_VERTICAL);
            spinnerWithLabelContainer.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams spinnerWithLabelContainerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            spinnerWithLabelContainerParams.setMargins(0, defaultMargin, 0, defaultMargin);

            View spinnerBtnWithImage = inflater.inflate(R.layout.characteristic_spinner_btn, null);
            spinnerBtnWithImage.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));
            spinnerBtnWithImage.findViewById(R.id.btn_show_picker_dialog).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.show();
                }
            });
            LinearLayout spinnerBtnContainer = new LinearLayout(context);
            spinnerBtnContainer.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            spinnerBtnContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.2f));
            spinnerBtnContainer.addView(spinnerBtnWithImage);

            LinearLayout nameAndValueContainer = new LinearLayout(context);
            nameAndValueContainer.setOrientation(LinearLayout.VERTICAL);
            nameAndValueContainer.setGravity(Gravity.CENTER_VERTICAL);
            nameAndValueContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));

            int radioButtonID = radioGroup.getCheckedRadioButtonId();
            RadioButton radioButton = radioGroup.findViewById(radioButtonID);
            View spinnerValue = addValueText((String) (radioButton.getText()));
            View spinnerName = addFieldName(field.getName());
            nameAndValueContainer.addView(spinnerValue);
            nameAndValueContainer.addView(spinnerName);

            spinnerWithLabelContainer.addView(nameAndValueContainer);
            spinnerWithLabelContainer.addView(spinnerBtnContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            valuesLayout.addView(spinnerWithLabelContainer, spinnerWithLabelContainerParams);
        }
    }

    // Adds TextView with error info
    // Called when characteristic parsing error occured
    private void addProblemInfoView() {
        TextView problemTextView = new TextView(context);
        problemTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        problemTextView.setBackgroundColor(viewBackgroundColor);
        LinearLayout.LayoutParams fieldValueParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldValueParams.setMargins(0, 0, 0, 0);

        problemTextView.setLayoutParams(fieldValueParams);
        problemTextView.setTypeface(Typeface.DEFAULT_BOLD);

        problemTextView.setText(getText(R.string.parse_problem));
        valuesLayout.addView(problemTextView);

        problemTextView.setTextColor(Color.RED);
    }

    // Adds TextView with field name
    private View addFieldName(String name) {
        TextView fieldNameView = new TextView(context);
        fieldNameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_label_text_size));
        fieldNameView.setBackgroundColor(viewBackgroundColor);
        LinearLayout.LayoutParams fieldNameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldNameParams.gravity = Gravity.CENTER_VERTICAL;
        fieldNameParams.setMargins(0, 0, 0, 0);
        fieldNameView.setLayoutParams(fieldNameParams);
        fieldNameView.setText(name);

        fieldNameView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_subtle_text));
        return fieldNameView;
    }

    // Adds horizontal line to separate UI sections
    private View addHorizontalLine(int height) {
        View horizontalLine = new View(context);
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                height);
        lineParams.setMargins(0, convertPxToDp(5), 0, 0);
        horizontalLine.setLayoutParams(lineParams);
        horizontalLine.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.silabs_divider));
        return horizontalLine;
    }

    // Converts pixels to 'dp' unit
    private int convertPxToDp(int sizeInPx) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (sizeInPx * scale + 0.5f);
    }

    class WriteCharacteristic implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                writeValueToCharacteristic();
                return true;
            }
            return false;
        }
    }

    private boolean isNumeric(String string) {
        try {
            Double.parseDouble(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}


