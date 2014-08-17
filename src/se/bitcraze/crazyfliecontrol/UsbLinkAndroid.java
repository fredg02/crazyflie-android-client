package se.bitcraze.crazyfliecontrol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import se.bitcraze.crazyflielib.CrazyradioLink;
import se.bitcraze.crazyflielib.IUsbLink;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

public class UsbLinkAndroid implements IUsbLink{

    private static final String LOG_TAG = "UsbLink";

    private static int TRANSFER_TIMEOUT = 100;

    private UsbManager mUsbManager;
    private final UsbDevice mUsbDevice;
    private UsbInterface mIntf;
    private UsbEndpoint mEpIn;
    private UsbEndpoint mEpOut;
    private UsbDeviceConnection mConnection;

    private static PendingIntent mPermissionIntent;


    public UsbLinkAndroid(Context context) throws IOException {
        this.mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.mUsbDevice = searchForCrazyradio(context, mUsbManager);
        if (mUsbManager == null || mUsbDevice == null) {
            throw new IllegalArgumentException("USB manager and device must not be null");
        }
        initDevice();
    }

    /**
     * Initialize the USB device. Determines endpoints and prepares communication.
     * 
     * @throws IOException if the device cannot be opened
     */
    private void initDevice() throws IOException {
        Log.d(LOG_TAG, "setDevice " + this.mUsbDevice);
        // find interface
        if (this.mUsbDevice.getInterfaceCount() != 1) {
            Log.e(LOG_TAG, "Could not find interface");
            return;
        }
        mIntf = this.mUsbDevice.getInterface(0);
        // device should have two endpoints
        if (mIntf.getEndpointCount() != 2) {
            Log.e(LOG_TAG, "Could not find endpoints");
            return;
        }
        // endpoints should be of type bulk
        UsbEndpoint ep = mIntf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) {
            Log.e(LOG_TAG, "Endpoint is not of type bulk");
            return;
        }
        // check endpoint direction
        if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
            mEpIn = mIntf.getEndpoint(0);
            mEpOut = mIntf.getEndpoint(1);
        } else {
            mEpIn = mIntf.getEndpoint(1);
            mEpOut = mIntf.getEndpoint(0);
        }

        UsbDeviceConnection connection = mUsbManager.openDevice(mUsbDevice);
        if (connection != null && connection.claimInterface(mIntf, true)) {
            Log.d(LOG_TAG, "open SUCCESS");
            mConnection = connection;
        } else {
            Log.d(LOG_TAG, "open FAIL");
            throw new IOException("could not open usb connection");
        }
    }

    public int sendControlTransfer(int requestType, int request, int value, int index, byte[] data){
        if(mConnection != null){
            int dataLength = (data == null) ? 0 : data.length;
            return mConnection.controlTransfer(requestType, request, value, index, data, dataLength, TRANSFER_TIMEOUT);
        }
        return -1;
    }

    public int sendBulkTransfer(byte[] data, byte[] receiveData){
        int returnCode = -1;
        if(mConnection != null){
            mConnection.bulkTransfer(mEpOut, data, data.length, TRANSFER_TIMEOUT);
            returnCode = mConnection.bulkTransfer(mEpIn, receiveData, receiveData.length, TRANSFER_TIMEOUT);
        }
        return returnCode;
    }

    public UsbDeviceConnection getConnection(){
        return mConnection;
    }


    //TODO: should searchForCrazyRadio and isCrazyradio be generalized??

    /**
     * Iterate over all attached USB devices and look for Crazyradio.
     * If Crazyradio is found, request permission.
     */
    private static UsbDevice searchForCrazyradio(Context context, UsbManager usbManager) {
        UsbDevice device = null;
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        // Iterate over USB devices
        for (Entry<String, UsbDevice> e : deviceList.entrySet()) {
            Log.i(LOG_TAG, "String: " + e.getKey() + " " + e.getValue().getVendorId() + " " + e.getValue().getProductId());
            if (isCrazyradio(e.getValue())) {
                device = e.getValue();
                break; // stop after first matching device is found
            }
        }
        if (device != null && !usbManager.hasPermission(device)) {
            Log.d(LOG_TAG, "Request permission");
            mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(context.getPackageName()+".USB_PERMISSION"), 0);
            usbManager.requestPermission(device, mPermissionIntent);
        } else if (device != null && usbManager.hasPermission(device)) {
            Log.d(LOG_TAG, "Has permission");
        } else {
            Log.d(LOG_TAG, "device == null");
        }
        return device;
    }

    public static boolean isCrazyradio(UsbDevice device){
        return device.getVendorId() == CrazyradioLink.VENDOR_ID &&
               device.getProductId() == CrazyradioLink.PRODUCT_ID;
    }

    @Override
    public void releaseInterface() {
        if (mConnection != null && mIntf != null){
            mConnection.releaseInterface(mIntf);
            mConnection = null;
            mIntf = null;
        }
    }

    @Override
    public boolean isUsbConnected() {
        return mUsbDevice != null && mConnection != null;
    }

}
