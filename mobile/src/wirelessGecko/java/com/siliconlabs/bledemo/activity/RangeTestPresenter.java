package com.siliconlabs.bledemo.activity;

import android.os.Looper;
import android.support.annotation.UiThread;
import com.siliconlabs.bledemo.rangetest.RangeTestMode;
import com.siliconlabs.bledemo.rangetest.TxPower;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Comarch S.A.
 */

public class RangeTestPresenter {

    public interface Controller {

        void setView(RangeTestView view);

        void initTestMode(RangeTestMode mode);

        void cancelTestMode();

        void updateTxPower(int power);

        void updatePayloadLength(int length);

        void updateMaWindowSize(int size);

        void updateChannel(int channel);

        void updatePacketCount(int count);

        void updateRemoteId(int id);

        void updateSelfId(int id);

        void updateUartLogEnabled(boolean enabled);

        void toggleRunningState();

        void updatePhyConfig(int id);
    }

    public interface RangeTestView {

        void runOnUiThread(Runnable runnable);

        @UiThread
        void showDeviceName(String name);

        @UiThread
        void showModelNumber(String number);

        @UiThread
        void showTxPower(TxPower power, List<TxPower> values);

        @UiThread
        void showPayloadLength(int length, List<Integer> values);

        @UiThread
        void showMaWindowSize(int size, List<Integer> values);

        @UiThread
        void showChannelNumber(int number);

        @UiThread
        void showPacketCountRepeat(boolean enabled);

        @UiThread
        void showPacketRequired(int required);

        @UiThread
        void showPacketSent(int sent);

        @UiThread
        void showPer(float per);

        @UiThread
        void showMa(float ma);

        @UiThread
        void showRemoteId(int id);

        @UiThread
        void showSelfId(int id);

        @UiThread
        void showUartLogEnabled(boolean enabled);

        @UiThread
        void showRunningState(boolean running);

        @UiThread
        void showTestRssi(int rssi);

        @UiThread
        void showTestRx(int received, int required);

        @UiThread
        void showPhy(int phy, LinkedHashMap<Integer, String> values);

        @UiThread
        void clearTestResults();
    }

    private static final int MA_WINDOW_MAX = 128;
    private static final int MA_WINDOW_DEFAULT = 32;

    private RangeTestView view;

    private String deviceName;
    private String modelNumber;

    private TxPower txPower;
    private List<TxPower> txPowerValues;
    private Integer payloadLength;
    private List<Integer> payloadLengthValues;
    private Integer maWindowSize;
    private List<Integer> maWindowSizeValues;

    private Integer channelNumber;
    private Boolean packetCountRepeat;
    private Integer packetRequired;
    private Integer packetReceived;
    private Integer packetSent;
    private Integer packetCount;
    private Float per;
    private Float ma;
    private Integer remoteId;
    private Integer selfId;
    private Boolean uartLogEnabled;
    private Boolean running;

    private Integer phy;
    private LinkedHashMap<Integer, String> phyMap;

    private int lastPacketCount = -1;
    private int lastPacketLoss = 0;
    private int[] maBuffer = new int[MA_WINDOW_MAX];
    private int maBufferPtr;

    @UiThread
    public void setView(RangeTestView view) {
        this.view = view;

        if (view == null) {
            return;
        }

        if (deviceName != null) view.showDeviceName(deviceName);
        if (modelNumber != null) view.showModelNumber(modelNumber);
        if (txPower != null && txPowerValues != null) view.showTxPower(txPower, txPowerValues);
        if (payloadLength != null && payloadLengthValues != null) view.showPayloadLength(payloadLength, payloadLengthValues);
        if (maWindowSize != null && maWindowSizeValues != null) view.showMaWindowSize(maWindowSize, maWindowSizeValues);
        if (channelNumber != null) view.showChannelNumber(channelNumber);
        if (packetCountRepeat != null) view.showPacketCountRepeat(packetCountRepeat);
        if (packetRequired != null) view.showPacketRequired(packetRequired);
        if (packetSent != null) view.showPacketSent(packetSent);
        if (remoteId != null) view.showRemoteId(remoteId);
        if (selfId != null) view.showSelfId(selfId);
        if (uartLogEnabled != null) view.showUartLogEnabled(uartLogEnabled);
        if (running != null) view.showRunningState(running);
        if (phy != null && phyMap != null) view.showPhy(phy, phyMap);

        view.showMa(ma == null ? 0 : ma);
        view.showPer(per == null ? 0 : per);

        if (packetReceived == null || packetCount == null) {
            view.showTestRx(0, 0);
        } else {
            view.showTestRx(packetReceived, packetCount);
        }
    }

    // controller -> view

    void onDeviceNameUpdated(final String deviceName) {
        this.deviceName = deviceName;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showDeviceName(deviceName);
            }
        });
    }

    void onModelNumberUpdated(final String number) {
        this.modelNumber = number;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showModelNumber(number);
            }
        });
    }

    void onTxPowerUpdated(final int power) {
        this.txPower = new TxPower(power);
        if (txPowerValues != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showTxPower(txPower, txPowerValues);
                }
            });
        }
    }

    void onPayloadLengthUpdated(final int length) {
        this.payloadLength = length;
        if (payloadLengthValues != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPayloadLength(length, payloadLengthValues);
                }
            });
        }
    }

    void onMaWindowSizeUpdated(final int size) {
        this.maWindowSize = size;
        if (maWindowSizeValues != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showMaWindowSize(size, maWindowSizeValues);
                }
            });
        }
    }

    void onChannelNumberUpdated(final int number) {
        this.channelNumber = number;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showChannelNumber(number);
            }
        });
    }

    void onPacketCountRepeatUpdated(final boolean enabled) {
        this.packetCountRepeat = enabled;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showPacketCountRepeat(enabled);
            }
        });
    }

    void onPacketRequiredUpdated(final int required) {
        this.packetRequired = required;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showPacketRequired(required);
            }
        });
    }

    void onPacketReceivedUpdated(final int received) {
        this.packetReceived = received;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                if (RangeTestPresenter.this.packetCount != null) {
                    view.showTestRx(received, RangeTestPresenter.this.packetCount);
                }
            }
        });
    }

    void onPacketSentUpdated(final int sent) {
        if (sent <= packetRequired) {
            this.packetSent = sent;
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPacketSent(sent);
                }
            });
        }
    }

    void onPacketCountUpdated(final int packetCount) {
        this.packetCount = packetCount;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                if (RangeTestPresenter.this.packetReceived != null) {
                    view.showTestRx(RangeTestPresenter.this.packetReceived, packetCount);
                }
            }
        });
    }

    void onMaUpdated(final float ma) {
        this.ma = ma;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showMa(ma);
            }
        });
    }

    void onPerUpdated(final float per) {
        this.per = per;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showPer(per);
            }
        });
    }

    void onRemoteIdUpdated(final int id) {
        this.remoteId = id;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showRemoteId(id);
            }
        });
    }

    void onSelfIdUpdated(final int id) {
        this.selfId = id;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showSelfId(id);
            }
        });
    }

    void onUartLogEnabledUpdated(final boolean enabled) {
        this.uartLogEnabled = enabled;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showUartLogEnabled(enabled);
            }
        });
    }

    void onRunningStateUpdated(final boolean running) {
        this.running = running;
        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showRunningState(running);
            }
        });
    }

    void onPhyConfigUpdated(final int phy) {
        this.phy = phy;
        if (phyMap != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPhy(phy, phyMap);
                }
            });
        }
    }

    void onPhyMapUpdated(final LinkedHashMap<Integer, String> phyMap) {
        this.phyMap = phyMap;
        if (phy != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPhy(phy, phyMap);
                }
            });
        }
    }

    void onTxPowerRangeUpdated(final int from, final int to) {
        List<TxPower> values = new ArrayList<>();
        for (int value = from; value <= to; value = value + 5) {
            values.add(new TxPower(value));
        }
        txPowerValues = values;

        if (txPower != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showTxPower(txPower, txPowerValues);
                }
            });
        }
    }

    void onPayloadLengthRangeUpdated(final int from, final int to) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < to - from + 1; ++i) {
            values.add(from + i);
        }
        payloadLengthValues = values;

        if (payloadLength != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPayloadLength(payloadLength, payloadLengthValues);
                }
            });
        }
    }

    void onMaWindowSizeRangeUpdated(final int from, final int to) {
        List<Integer> values = new ArrayList<>();
        for (int i = from; i <= to; i = i * 2) {
            values.add(i);
        }
        maWindowSizeValues = values;

        if (maWindowSize != null) {
            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showMaWindowSize(maWindowSize, maWindowSizeValues);
                }
            });
        }
    }

    void onTestDataReceived(final int rssi, final int packetCount, final int packetReceived) {
        if (packetCount < this.lastPacketCount) {
            clearMaBuffer();
            lastPacketLoss = 0;

            onView(new ViewAction() {
                @Override
                public void run(RangeTestView view) {
                    view.showPer(0);
                    view.showMa(0);
                    view.clearTestResults();
                }
            });
        }

        this.lastPacketCount = packetCount;

//        this.packetRequired = packetCount;
        this.packetReceived = packetReceived;

        int totalPacketLoss = packetCount - packetReceived;
        int currentPacketLoss = totalPacketLoss - lastPacketLoss;
        lastPacketLoss = totalPacketLoss;

        final float per = totalPacketLoss * 100f / (float) packetCount;
        final float ma = updateMa(currentPacketLoss);

        onView(new ViewAction() {
            @Override
            public void run(RangeTestView view) {
                view.showTestRx(packetReceived, packetCount);
                view.showPer(per);
                view.showMa(ma);
                view.showTestRssi(rssi);
            }
        });
    }

    private void onView(final ViewAction action) {
        final RangeTestView view = this.view;

        if (view == null) {
            return;
        }

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            action.run(view);
        } else {
            view.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    action.run(view);
                }
            });
        }
    }

    private float updateMa(int packetsLost) {
        maBuffer[maBufferPtr++] = packetsLost;

        if (maBufferPtr >= maBuffer.length) {
            maBufferPtr = 0;
        }

        int window = getMaWindow();
        return sumMaBuffer(window) * 100f / (float) window;
    }

    private int sumMaBuffer(int window) {
        int sum = 0;

        for (int i = 1; i <= window; ++i) {
            int location = maBufferPtr - i;
            if (location < 0) {
                location = maBuffer.length - i;
            }

            int loss = maBuffer[location];
            if (loss >= 0) {
                sum += loss;
            } else {
                break;
            }
        }

        return sum;
    }

    private int getMaWindow() {
        Integer window = maWindowSize;
        return window != null && window > 0 && window <= MA_WINDOW_MAX ? window : MA_WINDOW_DEFAULT;
    }

    private void clearMaBuffer() {
        Arrays.fill(maBuffer, -1);
        maBufferPtr = 0;
    }

    private interface ViewAction {

        void run(RangeTestView view);
    }
}
