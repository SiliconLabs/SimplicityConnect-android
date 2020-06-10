package com.siliconlabs.bledemo.fragment;

import android.app.Dialog;
import android.app.Fragment;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import android.os.Handler;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.siliconlabs.bledemo.R;
import com.siliconlabs.bledemo.activity.DeviceServicesActivity;
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
import com.siliconlabs.bledemo.utils.StringUtils;

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
    public static final int FIELD_CONTAINER_PADDING_TOP = 15;
    public static final int FIELD_CONTAINER_PADDING_BOTTOM = 15;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_LEFT = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_TOP = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_RIGHT = 0;
    public static final int FIELD_VALUE_EDIT_TEXT_PADDING_BOTTOM = 0;

    //margins
    public static int FIELD_VALUE_EDIT_LEFT_MARGIN = 15;

    private int EDIT_NOT_CLEAR_ID = 1000;

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
    private boolean writeableWithoutResponse = false;
    private boolean notify = false;
    private boolean notificationsEnabled = false;
    private boolean indicationsEnabled = false;
    private boolean isRawValue = false;
    private boolean parseProblem = false;
    private int offset = 0; // in bytes
    private int currRefreshInterval = REFRESH_INTERVAL; // in seconds
    private byte[] value;
    private byte[] previousValue;
    private BluetoothGatt mDevice;
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
    private EditText hex;
    private EditText ascii;
    private EditText decimal;
    Dialog editableFieldsDialog;
    LinearLayout writableFieldsContainer;
    Button saveValueBtn;
    Button clearBtn;
    ImageView closeIV;
    HashMap<Field, Boolean> fieldsInRangeMap;
    HashMap<Field, Boolean> fieldsValidMap;

    private boolean writeWithResponse = true;
    private Handler handler;

    private boolean writeString = false;

    private String parsingProblemInfo;

    public boolean displayWriteDialog = false;

    private final String HEX_ID = "HEX";
    private final String ASCII_ID = "ASCII";
    private final String DECIMAL_ID = "DECIMAL";

    private ArrayList<View> hidableViews = new ArrayList<>();
    private ArrayList<EditText> rawValueViews = new ArrayList<>();
    private ArrayList<String> rawValueData;

    private final Runnable postLoadValueViews = new Runnable() {
        @Override
        public void run() {
            loadValueViews();
        }
    };

    private final Runnable postDisplayValues = new Runnable() {
        @Override
        public void run() {
            displayValues();
        }
    };

    public FragmentCharacteristicDetail() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // TODO inflate appropriate layout file
        viewBackgroundColor = ContextCompat.getColor(getActivity(), R.color.silabs_white);

        handler = new Handler();

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

        if (!isRawValue) {
            prepareValueData();
        }
        loadValueViews();

        if (displayWriteDialog) {
            showCharacteristicWriteDialog();
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
        editTexts.clear();


        if (!isRawValue) {
            if (parseProblem || !addNormalValue()) {
                editTexts.clear();
                addInvalidValue();
            }
        } else {
            addRawValue();
        }
    }

    // Configures characteristic if it is writeable
    private void configureWriteable() {
        if (writeable || writeableWithoutResponse) {
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
                    editableFieldsDialog.dismiss();
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

                            if (indicationsEnabled || notificationsEnabled) {
                                valuesLayout.removeAllViews();
                                loadValueViews();
                            } else if (Arrays.equals(value, previousValue)) {
                                // redraw ui elements
                                hideValues();
                                handler.removeCallbacks(postDisplayValues);
                                handler.postDelayed(postDisplayValues, 150);
                            } else {

                                valuesLayout.removeAllViews();
                                handler.removeCallbacks(postLoadValueViews);
                                handler.postDelayed(postLoadValueViews, 150);
                            }

                            if (value != null) {
                                previousValue = value.clone();
                            }
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

        if (Common.isSetProperty(Common.PropertyType.WRITE, mBluetoothCharact.getProperties())) {
            writeable = true;
        }

        if (Common.isSetProperty(Common.PropertyType.WRITE_NO_RESPONSE, mBluetoothCharact.getProperties())) {
            writeableWithoutResponse = true;
        }

        if (Common.isSetProperty(Common.PropertyType.NOTIFY, mBluetoothCharact.getProperties())
                || Common.isSetProperty(Common.PropertyType.INDICATE, mBluetoothCharact.getProperties())) {
            notify = true;
        }

        //Display IEEE characteristic as raw data
        if (mCharact == null || mCharact.getFields() == null || mCharact.getName().equals("IEEE 11073-20601 Regulatory Certification Data List")) {
            isRawValue = true;
        }
    }

    private void writeValueToCharacteristic() {
        EditText hexEdit = editableFieldsDialog.findViewById(R.id.hex_edit);

        if (writeWithResponse) {
            mBluetoothCharact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        } else {
            mBluetoothCharact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        }

        if (hexEdit != null) {
            String hex = hexEdit.getText().toString().replaceAll("\\s+", "");
            byte[] newValue = hexToByteArray(hex);
            try {
                Log.d("Name", "" + mDevice.getDevice().getName());
                Log.d("Address", "" + mDevice.getDevice().getAddress());
                Log.d("Service", "" + mBluetoothCharact.getService().getUuid());
                Log.d("Charac", "" + mBluetoothCharact.getUuid());
                mBluetoothCharact.setValue(newValue);
                Log.d("hex", "" + Converters.getHexValue(mBluetoothCharact.getValue()));
                mDevice.writeCharacteristic(mBluetoothCharact);

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

    private void hideValues() {

        for (View view : hidableViews) {
            view.setVisibility(View.GONE);
        }

        rawValueData = new ArrayList<>();
        for (EditText et : rawValueViews) {
            rawValueData.add(et.getText().toString());
            et.setText("");
        }

    }

    private void displayValues() {

        for (View view : hidableViews) {
            view.setVisibility(View.VISIBLE);
        }

        int i = 0;
        for (EditText et : rawValueViews) {
            et.setText(rawValueData.get(i++));
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

    public Characteristic getmCharact() {
        return mCharact;
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
                Log.i("Characteristic value", Converters.getDecimalValue(value));
                parsingProblemInfo = prepareParsingProblemInfo(mCharact);
                parseProblem = true;
                return false;
            }
        }

        for (EditText et : editTexts) {
            et.setText("");
        }

        return true;
    }

    private String prepareParsingProblemInfo(Characteristic characteristic) {
        StringBuilder builder = new StringBuilder();
        builder.append("An error occurred while parsing this characteristic.").append("\n");
        if (value == null) return builder.toString();

        int expectedBytes = 0;
        int readSize = value.length;

        try {
            for (int i = 0; i < characteristic.getFields().size(); i++) {
                Field field = characteristic.getFields().get(i);
                expectedBytes += Engine.getInstance().getFormat(field.getFormat());
            }
        } catch (NullPointerException ex) {
            return builder.toString();
        }

        if (expectedBytes != readSize) {

            int expectedBits = expectedBytes * 8;
            int readBits = readSize * 8;

            builder.append("Reason: expected data length is ")
                    .append(expectedBits)
                    .append("-bit (")
                    .append(expectedBytes);

            if (expectedBytes == 1) {
                builder.append(" byte), ");
            } else {
                builder.append(" bytes), ");
            }

            builder.append("\n")
                    .append("read data length is ")
                    .append(readBits)
                    .append("-bit (")
                    .append(readSize);

            if (readSize == 1) {
                builder.append(" byte).");
            } else {
                builder.append(" bytes).");
            }
        }

        return builder.toString();
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


    private String getSint16AsString(byte[] array) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            if (array[i] < 0) {
                array[i] = (byte) (array[i] + 256);
            }
        }

        for (int i = array.length - 1; i >= 0; i--) {
            builder.append(Converters.getHexValue(array[i]));
        }

        int result = Integer.parseInt(builder.toString(), 16);

        if (result >= 32768) result = result - 65536;

        return String.valueOf(result);
    }

    // Reads next value for given format
    private String readNextValue(String format) {
        if (value == null) {
            return "";
        }

        int formatLength = Engine.getInstance().getFormat(format);

        // if format is sint16
        if (format.toLowerCase().equals("sint16")) {
            byte[] array = Arrays.copyOfRange(value, offset, offset + formatLength);
            offset += formatLength;
            return getSint16AsString(array);
        }

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

        ImageView hexCopyIV = readableFieldsForInline.findViewById(R.id.hex_copy);
        ImageView asciiCopyIV = readableFieldsForInline.findViewById(R.id.ascii_copy);
        ImageView decimalCopyIV = readableFieldsForInline.findViewById(R.id.decimal_copy);

        hex.setId(EDIT_NOT_CLEAR_ID);
        ascii.setId(EDIT_NOT_CLEAR_ID);
        decimal.setId(EDIT_NOT_CLEAR_ID);

        hex.setKeyListener(null);
        ascii.setKeyListener(null);
        decimal.setKeyListener(null);

        hex.setText(Converters.getHexValue(value));
        ascii.setText(Converters.getAsciiValue(value));
        decimal.setText(Converters.getDecimalValue(value));

        rawValueViews.add(hex);
        rawValueViews.add(ascii);
        rawValueViews.add(decimal);

        setCopyListener(hex, hexCopyIV);
        setCopyListener(ascii, asciiCopyIV);
        setCopyListener(decimal, decimalCopyIV);

        valuesLayout.addView(readableFieldsForInline);

        if (writeable || writeableWithoutResponse) {
            View writableFieldsForDialog = layoutInflater.inflate(R.layout.characteristic_value, null);

            hexEdit = writableFieldsForDialog.findViewById(R.id.hex_edit);
            asciiEdit = writableFieldsForDialog.findViewById(R.id.ascii_edit);
            decimalEdit = writableFieldsForDialog.findViewById(R.id.decimal_edit);

            ImageView hexPasteIV = writableFieldsForDialog.findViewById(R.id.hex_paste);
            ImageView asciiPasteIV = writableFieldsForDialog.findViewById(R.id.ascii_paste);
            ImageView decimalPasteIV = writableFieldsForDialog.findViewById(R.id.decimal_paste);

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

            setPasteListener(hexEdit, hexPasteIV, HEX_ID);
            setPasteListener(asciiEdit, asciiPasteIV, ASCII_ID);
            setPasteListener(decimalEdit, decimalPasteIV, DECIMAL_ID);

            updateSaveButtonState();

            if (writableFieldsContainer != null) {
                writableFieldsContainer.removeAllViews();
                writableFieldsContainer.addView(writableFieldsForDialog);
            }

        }
    }

    private void setCopyListener(final EditText copyFromET, final ImageView copyIV) {
        copyIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = context.getSystemService(ClipboardManager.class);
                ClipData clip = ClipData.newPlainText("characteristic-value", copyFromET.getText().toString());
                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(clip);
                    Toast.makeText(context, getString(R.string.Copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setPasteListener(final EditText pasteToET, final ImageView pasteIV, final String expectedPasteType) {

        pasteIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = context.getSystemService(ClipboardManager.class);
                if (clipboardManager != null && clipboardManager.getPrimaryClip() != null) {
                    ClipData clip = clipboardManager.getPrimaryClip();
                    String text = clip.getItemAt(0).getText().toString();

                    pasteToET.requestFocus();

                    switch (expectedPasteType) {
                        case HEX_ID:
                            text = StringUtils.getStringWithoutWhitespaces(text);
                            if (isHexStringCorrect(text)) pasteToET.setText(text);
                            else
                                Toast.makeText(context, getString(R.string.Incorrect_data_format), Toast.LENGTH_SHORT).show();
                            break;
                        case ASCII_ID:
                            pasteToET.setText(text);
                            break;
                        case DECIMAL_ID:
                            if (isDecimalCorrect(text)) pasteToET.setText(text);
                            else
                                Toast.makeText(context, getString(R.string.Incorrect_data_format), Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        });
    }

    private boolean isHexStringCorrect(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!StringUtils.HEX_VALUES.contains(String.valueOf(text.charAt(i)))) return false;
        }
        return true;
    }

    private boolean isDecimalCorrect(String text) {
        String[] arr = text.split(" ");

        try {
            for (String s : arr) {
                int tmp = Integer.parseInt(s);
                if (!(0 <= tmp && tmp <= 255)) return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void initCharacteristicWriteDialog() {
        editableFieldsDialog = new Dialog(getActivity());
        editableFieldsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        editableFieldsDialog.setContentView(R.layout.dialog_characteristic_write);
        editableFieldsDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        writableFieldsContainer = editableFieldsDialog.findViewById(R.id.characteristic_writable_fields_container);

        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        editableFieldsDialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);

        initWriteModeView(editableFieldsDialog);

        saveValueBtn = editableFieldsDialog.findViewById(R.id.save_btn);
        clearBtn = editableFieldsDialog.findViewById(R.id.clear_btn);
        closeIV = editableFieldsDialog.findViewById(R.id.image_view_close);
    }

    private boolean isAnyWriteFieldEmpty() {
        for (EditText e : editTexts) {
            if (e.getId() == EDIT_NOT_CLEAR_ID) continue;
            if (e.getText().toString().isEmpty()) return true;
        }
        return false;
    }

    public void showCharacteristicWriteDialog() {
        // if any textfields are empty, save rounded_button_red will be initialized to be disabled
        updateSaveButtonState();

        saveValueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAnyWriteFieldEmpty()) {
                    writeValueToCharacteristic();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.You_cannot_send_empty_value_to_charac), Toast.LENGTH_SHORT).show();
                }
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (EditText et : editTexts) {
                    if (et.getId() != EDIT_NOT_CLEAR_ID) {
                        et.setText("");
                    }
                }
            }
        });

        closeIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editableFieldsDialog != null) {
                    editableFieldsDialog.dismiss();
                }
            }
        });

        String serviceName = mService != null ? mService.getName().trim() : getString(R.string.unknown_service);
        serviceName = Common.checkOTAService(mGattService.getUuid().toString(), serviceName);

        String characteristicName;
        if (mCharact != null) {
            characteristicName = mCharact.getName().trim();
        } else {
            characteristicName = getOtaSpecificCharacteristicName(mBluetoothCharact.getUuid().toString());
        }


        String characteristicUuid = mCharact != null ? Common.getUuidText(mCharact.getUuid()) : Common.getUuidText(mBluetoothCharact.getUuid());

        TextView serviceNameTextView = editableFieldsDialog.findViewById(R.id.picker_dialog_service_name);
        TextView characteristicTextView = editableFieldsDialog.findViewById(R.id.characteristic_dialog_characteristic_name);
        TextView uuidTextView = editableFieldsDialog.findViewById(R.id.picker_dialog_characteristic_uuid);
        serviceNameTextView.setText(serviceName);
        characteristicTextView.setText(characteristicName);
        uuidTextView.setText(characteristicUuid);

        LinearLayout propertiesContainer = editableFieldsDialog.findViewById(R.id.picker_dialog_properties_container);
        initPropertiesForEditableFieldsDialog(propertiesContainer);

        //Clear EditText fields
        for (EditText et : editTexts) {
            if (et.getId() != EDIT_NOT_CLEAR_ID) {
                et.setText("");
            }
        }

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

            if (propertyValue.trim().toUpperCase().equals("WRITE NO RESPONSE")) {
                float d = getResources().getDisplayMetrics().density;
                paramsIcon = new LinearLayout.LayoutParams((int) (24 * d), ((int) (24 * d)));
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
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.BOTTOM;
        fieldUnitView.setLayoutParams(layoutParams);
        hidableViews.add(fieldUnitView);

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


            if (writeable || writeableWithoutResponse) {
                // inline field value
                EditText fieldValueEdit = addValueEdit(field, val);
                fieldValueEdit.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                params.setMargins(8, 0, 0, 0);
                params.gravity = Gravity.CENTER_VERTICAL;
                fieldValueEdit.setLayoutParams(params);
                fieldValueEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
                editTexts.add(fieldValueEdit);
                TextView fieldValue = (TextView) addFieldName(fieldValueEdit.getText().toString());
                fieldValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
                hidableViews.add(fieldValue);
                fieldValue.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));
                valueLayout.addView(fieldValue);

                // dialog field value
                // field name
                View fieldName = addFieldName(field.getName());
                params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                params.setMargins(0, 0, 8, 0);
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
                fieldContainer.setPadding(0, FIELD_CONTAINER_PADDING_TOP, 0, FIELD_CONTAINER_PADDING_BOTTOM);
                writableFieldsContainer.addView(fieldContainer);
            } else {
                TextView fieldValueView = addValueText(val);
                fieldValueView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
                hidableViews.add(fieldValueView);
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
                    byte[] array = fieldValueEdit.getText().toString().getBytes();
                    fillValue(array);
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
/*
        boolean emptyFieldExists = true;
        for (EditText editableField : editTexts) {
            emptyFieldExists = !editableField.getText().toString().equals("");
            Log.d("editableField", " Empty: " + editableField.getText().toString().equals(""));
        }
*/
        if (saveValueBtn != null) {
            saveValueBtn.setEnabled(true);
            saveValueBtn.setClickable(true);
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


    private TextView getBitNameView(String name, LinearLayout bitsLayout) {
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
        bitNameView.setText(name);
        bitNameView.setTextColor(ContextCompat.getColor(getActivity(), R.color.silabs_primary_text));

        return bitNameView;
    }

    private TextView getCheckboxListHeader(String name) {
        TextView checkboxListHeader = (TextView) addFieldName(name);
        checkboxListHeader.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.characteristic_list_item_value_text_size));
        checkboxListHeader.setTextColor(Color.BLACK);

        return checkboxListHeader;
    }

    private LinearLayout getBitsLayout() {
        LinearLayout bitsLayout = new LinearLayout(context);
        bitsLayout.setBackgroundColor(viewBackgroundColor);
        //noinspection ResourceType
        bitsLayout.setId(1);
        RelativeLayout.LayoutParams bitsLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bitsLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        bitsLayout.setLayoutParams(bitsLayoutParams);
        bitsLayout.setOrientation(LinearLayout.HORIZONTAL);

        return bitsLayout;
    }

    private void setStringBuilderBitsInRange(StringBuilder builder, int startBit, int endBit, int value) {
        while (startBit < endBit) {
            char bitValue = ((value & 1) == 1) ? '1' : '0';
            builder.setCharAt(endBit - 1, bitValue);
            value >>= 1;
            endBit--;
        }
    }

    // Get value from bitsString in range start (inclusive) -> end (exclusive)
    // bits String order must be:
    // least significant (index 0), to most significant (index last)
    private int getValueInStringBitsRange(int start, int end, String bits) {
        int result = 0;

        while (start < end) {
            if (bits.charAt(start) == '1') {
                result |= 1;
            } else {
                result |= 0;
            }

            if (start + 1 < end) {
                result <<= 1;
            }

            start++;
        }

        return result;
    }

    // Get value bits as String from offset to offest+formatLength
    // bits are parsed in order: LSO(least significant octet) ===> MSO (most significant octet)
    // String charAt(0) - least significant bit,
    // String charAt(last) most significant bit.
    private String getFieldValueAs_LSO_MSO_BitsString(int offset, int formatLength) {
        StringBuilder result = new StringBuilder();

        for (int i = offset; i < offset + formatLength; i++) {
            byte val = value[i];
            for (int j = 0; j < 8; j++) {
                if ((val & 0b0000_0001) == 0b0000_0001) {
                    result.append(1);
                } else {
                    result.append(0);
                }
                val >>= 1;
            }
        }

        return result.toString();
    }

    private StringBuilder fillStringBuilderWithZeros(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append('0');
        }
        return builder;
    }

    // Convert String of bits where bitsString charAt(0) is least significant
    // to byte array
    private byte[] bitsStringToByteArray(String bitsString, int length) {
        byte arr[] = new byte[length];
        for (int i = 0; i < length; i++) {
            int tmp = 0;
            for (int j = 8 * (i + 1) - 1; j >= i * 8; j--) {
                char bitChar = bitsString.charAt(j);
                if (bitChar == '1') {
                    tmp |= 1;
                } else {
                    tmp |= 0;
                }

                if (j > i * 8) {
                    tmp <<= 1;
                }
            }
            arr[i] = (byte) tmp;
        }

        return arr;
    }


    // Adds views related to bitfield value
    private void addBitfield(Field field) {

        if (field.getReference() == null) {

            final int formatLength = Engine.getInstance().getFormat(field.getFormat());
            final int bitsLength = formatLength * 8;
            int currentBit = 0;
            String valueBits = getFieldValueAs_LSO_MSO_BitsString(offset, formatLength);
            final StringBuilder builder = fillStringBuilderWithZeros(bitsLength);


            // Display read bitfields
            for (Bit bit : field.getBitfield().getBits()) {
                final ArrayList<String> enumerations = new ArrayList<>();

                // Bits in range startBitIndex to endBitIndex will be replaced with new value for given bitField
                final int startBitIndex = currentBit;
                final int endBitIndex = currentBit + bit.getSize();

                for (Enumeration enumeration : bit.getEnumerations()) {
                    enumerations.add(enumeration.getValue());
                }

                LinearLayout.LayoutParams nameAndValueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                nameAndValueParams.setMargins(0, defaultMargin, 0, 12 + defaultMargin / 2);
                LinearLayout nameAndValueContainer = new LinearLayout(context);
                nameAndValueContainer.setOrientation(LinearLayout.VERTICAL);
                nameAndValueContainer.setLayoutParams(nameAndValueParams);

                View valueText = addValueText(enumerations.get(getValueInStringBitsRange(startBitIndex, endBitIndex, valueBits)));
                View nameText = addFieldName(bit.getName());
                nameAndValueContainer.addView(valueText);
                nameAndValueContainer.addView(nameText);
                valuesLayout.addView(nameAndValueContainer);

                hidableViews.add(valueText);

                if (writeable || writeableWithoutResponse) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    params.setMargins(8, 0, 0, 0);

                    final Spinner spinner = new Spinner(context);
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(context, R.layout.enumeration_spinner_dropdown_item, enumerations);
                    spinner.setAdapter(spinnerArrayAdapter);
                    spinner.setLayoutParams(params);

                    final int off = offset;

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            // After each spinner selection bits are prepared for characteristic write - value array is updated with selected value
                            setStringBuilderBitsInRange(builder, startBitIndex, endBitIndex, position);
                            byte[] val = bitsStringToByteArray(builder.toString(), formatLength);
                            //intToByteArray(Integer.parseInt(builder.toString(), 2), formatLength);
                            setValue(off, val);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    View fieldName = addFieldName(bit.getName());
                    params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                    params.gravity = Gravity.CENTER_VERTICAL;
                    params.setMargins(0, 0, 8, 0);
                    fieldName.setLayoutParams(params);

                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);

                    linearLayout.addView(fieldName);
                    linearLayout.addView(spinner);

                    writableFieldsContainer.addView(linearLayout);

                }
                currentBit = currentBit + bit.getSize();
            }
            offset += formatLength;
        }
    }

    private void initWriteModeView(Dialog dialog) {
        final RadioButton writeWithResponseRB = dialog.findViewById(R.id.write_with_resp_radio_button);
        final RadioButton writeWithoutResponseRB = dialog.findViewById(R.id.write_without_resp_radio_button);
        final LinearLayout writeMethodLL = dialog.findViewById(R.id.write_method_linear_layout);

        if (writeable) {
            writeWithResponseRB.setChecked(true);
            writeWithResponseRB.setChecked(true);
            writeWithResponse = true;

            writeWithResponseRB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    writeWithResponse = true;
                }
            });

        } else {
            writeWithResponseRB.setEnabled(false);
            writeWithResponseRB.setChecked(false);
        }

        if (writeableWithoutResponse) {
            writeWithoutResponseRB.setEnabled(true);
            if (!writeWithResponseRB.isChecked()) {
                writeWithoutResponseRB.setChecked(true);
                writeWithResponse = false;
            }

            writeWithoutResponseRB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    writeWithResponse = false;
                }
            });

        } else {
            writeWithoutResponseRB.setEnabled(false);
            writeWithoutResponseRB.setChecked(false);
        }

        if (!writeableWithoutResponse && !writeable) {
            writeMethodLL.setVisibility(View.GONE);
        }


    }

    // Adds views related to enumeration value
    // Each enumeration is presented as Spinner view
    private void addEnumeration(final Field field) {
        if (field.getReference() == null) {

            final ArrayList<String> enumerationArray = new ArrayList<>();
            for (Enumeration en : field.getEnumerations()) {
                enumerationArray.add(en.getValue());
            }

            if (!parseProblem) {

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

                LinearLayout.LayoutParams nameAndValueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                nameAndValueParams.setMargins(0, defaultMargin, 0, 12 + defaultMargin / 2);
                LinearLayout nameAndValueContainer = new LinearLayout(context);
                nameAndValueContainer.setOrientation(LinearLayout.VERTICAL);
                nameAndValueContainer.setLayoutParams(nameAndValueParams);

                View valueText = addValueText(enumerationArray.get(pos));
                hidableViews.add(valueText);
                View nameText = addFieldName(field.getName());
                nameAndValueContainer.addView(valueText);
                nameAndValueContainer.addView(nameText);

                valuesLayout.addView(nameAndValueContainer);

                if (writeable || writeableWithoutResponse) {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
                    params.gravity = Gravity.CENTER_VERTICAL;

                    final int offset = getFieldOffset(field);

                    final Spinner spinner = new Spinner(context);
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(context, R.layout.enumeration_spinner_dropdown_item, enumerationArray);
                    spinner.setAdapter(spinnerArrayAdapter);
                    spinner.setLayoutParams(params);

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                            int key = field.getEnumerations().get(position).getKey();
                            int formatLength = Engine.getInstance().getFormat(field.getFormat());
                            byte[] val = intToByteArray(key, formatLength);
                            setValue(offset, val);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });

                    View fieldName = addFieldName(field.getName());
                    fieldName.setLayoutParams(params);

                    LinearLayout linearLayout = new LinearLayout(context);
                    linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);

                    linearLayout.addView(fieldName);
                    linearLayout.addView(spinner);

                    writableFieldsContainer.addView(linearLayout);
                }
            }
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

        //problemTextView.setText(getText(R.string.parse_problem));
        problemTextView.setText(parsingProblemInfo);
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

    private String getOtaSpecificCharacteristicName(String uuid) {
        uuid = uuid.toUpperCase();
        switch (uuid) {
            case "F7BF3564-FB6D-4E53-88A4-5E37E0326063":
                return "OTA Control Attribute";
            case "984227F3-34FC-4045-A5D0-2C581F81A153":
                return "OTA Data Attribute";
            case "4F4A2368-8CCA-451E-BFFF-CF0E2EE23E9F":
                return "AppLoader version";
            case "4CC07BCF-0868-4B32-9DAD-BA4CC41E5316":
                return "OTA version";
            case "25F05C0A-E917-46E9-B2A5-AA2BE1245AFE":
                return "Gecko Bootloader version";
            case "0D77CC11-4AC1-49F2-BFA9-CD96AC7A92F8":
                return "Application version";
            default:
                return getString(R.string.unknown_characteristic_label);
        }

    }

}


