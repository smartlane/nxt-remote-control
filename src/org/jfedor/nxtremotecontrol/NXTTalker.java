/*
 * Copyright (c) 2010 Jacek Fedorynski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is derived from:
 * 
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChatService.html
 * 
 * Copyright (c) 2009 The Android Open Source Project
 */

package org.jfedor.nxtremotecontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;

import org.json.JSONObject;
import org.json.JSONArray;



public class NXTTalker {

    
private void sendHttp() {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("https://smartlane.io/api/action/datastore_upsert");
            post.setHeader("X-CKAN-API-Key",""); 
            JSONObject jsonObj = new JSONObject();
            try {
            jsonObj.put("resource_id", "6f985bb4-3875-4a84-a0cb-97a0a23df94d");
            jsonObj.put("force", true);
            JSONArray records = new JSONArray();
            for (int i=0;i<arrayPos;i++) {
                JSONObject record = new JSONObject();
                record.put("time", timevalues[i]);
                record.put("speed", speedvalues[i]);
                records.put(record);
            }
            arrayPos = 0;
            jsonObj.put("records", records);
            StringEntity entity = new StringEntity(jsonObj.toString(), HTTP.UTF_8);
            entity.setContentType("application/json");
            post.setEntity(entity);

            HttpResponse response = client.execute(post);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

public static Date GetUTCdatetimeAsDate()
{
    //note: doesn't check for null
    return StringDateToDate(GetUTCdatetimeAsString());
}

public static String GetUTCdatetimeAsString()
{
    final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    final String utcTime = sdf.format(new Date());

    return utcTime;
}

public static Date StringDateToDate(String StrDate)
{
    Date dateToReturn = null;
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

    try
    {
        dateToReturn = (Date)dateFormat.parse(StrDate);
    }
    catch (ParseException e)
    {
        e.printStackTrace();
    }

    return dateToReturn;
}

    public static final int STATE_NONE = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    
    private double[] speedvalues = new double[1024];
    private String[] timevalues = new String[1024];
    private int arrayPos = 0;
    
    
    private int mState;
    private Handler mHandler;
    private BluetoothAdapter mAdapter;
    
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    
    public NXTTalker(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        setState(STATE_NONE);
    }

    private synchronized void setState(int state) {
        mState = state;
        if (mHandler != null) {
            mHandler.obtainMessage(NXTRemoteControl.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        } else {
            //XXX
        }
    }
    
    public synchronized int getState() {
        return mState;
    }
    
    public synchronized void setHandler(Handler handler) {
        mHandler = handler;
    }
    
    private void toast(String text) {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(NXTRemoteControl.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(NXTRemoteControl.TOAST, text);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } else {
            //XXX
        }
    }

    public synchronized void connect(BluetoothDevice device) {
        //Log.i("NXT", "NXTTalker.connect()");
        
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }
    
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        //toast("Connected to " + device.getName());
        
        setState(STATE_CONNECTED);
    }
    
    public synchronized void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }
    
    private void connectionFailed() {
        setState(STATE_NONE);
        //toast("Connection failed");
    }
    
    private void connectionLost() {
        setState(STATE_NONE);
        //toast("Connection lost");
    }
    
    public void motors(byte l, byte r, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motors: " + Byte.toString(l) + ", " + Byte.toString(r));
        
        data[5] = l;
        data[19] = r;
        
        timevalues[arrayPos] = GetUTCdatetimeAsString();
        if (!(((int)l == 0) || ((int)r == 0))) { //Turning
            double speed = (int)l;
            speedvalues[arrayPos] = speed;
        }
        else speedvalues[arrayPos] = 0;
        Log.d("buffer", timevalues[arrayPos]);
        if (arrayPos<1024) arrayPos++;
        else {
            Log.e("ArrayOverflow", "No space left in array");
            arrayPos = 0;
        }
        
        
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }
    
    public void motor(int motor, byte power, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motor: " + Integer.toString(motor) + ", " + Byte.toString(power));
        
        if (motor == 0) {
            data[4] = 0x02;
        } else {
            data[4] = 0x01;
        }
        data[5] = power;
        if (speedReg) {
            data[7] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
        }
        write(data);
    }
    
    public void readSensor() {
        byte[] data = { 0x05, 0x00, (byte) 0x80, 0x05, 0x00, 0x01, 0x20};
        write(data);
        byte[] data2 = { 0x03, 0x00, (byte) 0x00, 0x07, 0x00};
        byte[] readdata = write(data2);
    }
    
    public void motors3(byte l, byte r, byte action, boolean speedReg, boolean motorSync) {
        byte[] data = { 0x0c, 0x00, (byte) 0x80, 0x04, 0x02, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x01, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
                        0x0c, 0x00, (byte) 0x80, 0x04, 0x00, 0x32, 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
        
        //Log.i("NXT", "motors3: " + Byte.toString(l) + ", " + Byte.toString(r) + ", " + Byte.toString(action));
        
        data[5] = l;
        data[19] = r;
        data[33] = action;
        if (speedReg) {
            data[7] |= 0x01;
            data[21] |= 0x01;
        }
        if (motorSync) {
            data[7] |= 0x02;
            data[21] |= 0x02;
        }
        write(data);
    }
    
    private byte[] write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return null;
            }
            r = mConnectedThread;
        }
        return r.write(out);
    }
    
    private byte[] read() {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return null;
            }
            r = mConnectedThread;
        }
        return r.read();
    }
    
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
        }
        
        public void run() {
            setName("ConnectThread");
            mAdapter.cancelDiscovery();
            
            try {
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    // This is a workaround that reportedly helps on some older devices like HTC Desire, where using
                    // the standard createRfcommSocketToServiceRecord() method always causes connect() to fail.
                    Method method = mmDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                    mmSocket = (BluetoothSocket) method.invoke(mmDevice, Integer.valueOf(1));
                    mmSocket.connect();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    connectionFailed();
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    return;
                }
            }
            
            synchronized (NXTTalker.this) {
                mConnectThread = null;
            }
            
            connected(mmSocket, mmDevice);
        }
        
        public void cancel() {
            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    sendHttp();
                    Log.d("buffer", "sending http");
                    if ((int)(buffer[12]) == -75) {
                        stop();
                    }
                    //toast(Integer.toString(bytes) + " bytes read from device");
                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                    break;
                }
            }
        }
        
        public byte[] write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                byte[] rbuffer = new byte[1024];
                /*if (buffer[2] == 0x00) {
                    try {
                        int bytes = mmInStream.read(rbuffer);
                        Log.d("available", Integer.toString(bytes));
                    }
                    catch (Exception e) {
                        Log.d("available", "Exception!");
                    }
                }
                */
                return rbuffer;
            } catch (IOException e) {
                e.printStackTrace();
                // XXX?
            }
            return null;
        }
        
        public byte[] read() {
            try {
                byte[] buffer = new byte[1024];
                mmInStream.read(buffer);
                return buffer;
            } catch (IOException e) {
                e.printStackTrace();
                // XXX?
            }
            return null;
        }
        
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
