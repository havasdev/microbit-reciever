package com.inov.semina.microbitreciever.ui;
/*
 * Author: Martin Woolley
 * Twitter: @bluetooth_mdw
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.inov.semina.microbitreciever.Constants;
import com.inov.semina.microbitreciever.MicroBit;
import com.inov.semina.microbitreciever.R;
import com.inov.semina.microbitreciever.Utility;
import com.inov.semina.microbitreciever.bluetooth.BleAdapterService;
import com.inov.semina.microbitreciever.bluetooth.ConnectionStatusListener;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class UartAvmActivity extends AppCompatActivity implements ConnectionStatusListener {
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private BleAdapterService bluetooth_le_adapter;

    private boolean exiting=false;
    private boolean indications_on=false;
    private int guess_count=0;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(Constants.TAG, "onServiceConnected");
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(mMessageHandler);
            connectToDevice();


        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_uart_avm);
        getSupportActionBar().setTitle(R.string.screen_title_UART_AVM);

        // read intent data
        final Intent intent = getIntent();
        MicroBit.getInstance().setMicrobit_name(intent.getStringExtra(EXTRA_NAME));
        MicroBit.getInstance().setMicrobit_address(intent.getStringExtra(EXTRA_ID));
        MicroBit.getInstance().setConnection_status_listener(this);

        // connect to the Bluetooth smart service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Temp)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Temp)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_TempSym)).setVisibility(View.GONE);
        ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Humi)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Humi)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_HumiSym)).setVisibility(View.GONE);
        ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Pres)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Pres)).setVisibility(View.GONE);
        ((TextView) UartAvmActivity.this.findViewById(R.id.textView_PresSym)).setVisibility(View.GONE);
        ((Button)UartAvmActivity.this.findViewById(R.id.buttonEnd)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }



    @Override
    protected void onDestroy() {
        Log.d(Constants.TAG, "onDestroy");
        super.onDestroy();
        if (indications_on) {
            exiting = true;
            bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), false);
        }
        exiting=true;
        if (MicroBit.getInstance().isMicrobit_connected()) {
            bluetooth_le_adapter.disconnect();
        }

        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection);
        } catch (Exception e) {
        }
    }

    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        if (MicroBit.getInstance().isMicrobit_connected() && indications_on) {
            exiting = true;
            bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), false);
            bluetooth_le_adapter.disconnect();
        }
        exiting=true;

        try {
            // may already have unbound. No API to check state so....
            unbindService(mServiceConnection);
        } catch (Exception e) {
        }

        finish();
        exiting=true;
    }

    private void connectToDevice() {
        //showMsg(Utility.htmlColorBlue("Connecting to micro:bit"));
        showMsg(Utility.htmlColorBlue("micro:bitに接続中…"));
        if (bluetooth_le_adapter.connect(MicroBit.getInstance().getMicrobit_address())) {
        } else {
            //showMsg(Utility.htmlColorRed("onConnect: failed to connect"));
            showMsg(Utility.htmlColorRed("onConnect: 接続に失敗しました"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_uart_avm, menu);
        return true;
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.menu_uart_avm_new_game) {
            onNewGame();
            return true;
        }

        if (id == R.id.menu_uart_avm_help) {
            Intent intent = new Intent(UartAvmActivity.this, HelpActivity.class);
            intent.putExtra(Constants.URI, Constants.UART_AVM_HELP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
     */

    // Service message handler�//////////////////
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            String descriptor_uuid = "";
            byte[] b = null;
            TextView value_text = null;

            switch (msg.what) {
                case BleAdapterService.GATT_CONNECTED:
                    //showMsg(Utility.htmlColorGreen("Connected"));
                    //showMsg(Utility.htmlColorGreen("Discovering services..."));
                    showMsg(Utility.htmlColorGreen("接続しました"));
                    showMsg(Utility.htmlColorGreen("サービスを探しています..."));
                    bluetooth_le_adapter.discoverServices();
                    break;
                case BleAdapterService.GATT_DISCONNECT:
                    //showMsg(Utility.htmlColorRed("Disconnected"));
                    showMsg(Utility.htmlColorRed("接続が切れました"));
                    onBackPressed();
                    //((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    Log.d(Constants.TAG, "XXXX Services discovered");
                    //showMsg(Utility.htmlColorGreen("Ready"));
                    showMsg(Utility.htmlColorGreen("通信待ち"));
                    //((LinearLayout) MenuActivity.this.findViewById(R.id.menu_items_area)).setVisibility(View.VISIBLE);
                    List<BluetoothGattService> slist = bluetooth_le_adapter.getSupportedGattServices();
                    for (BluetoothGattService svc : slist) {
                        Log.d(Constants.TAG, "UUID=" + svc.getUuid().toString().toUpperCase() + " INSTANCE=" + svc.getInstanceId());
                        MicroBit.getInstance().addService(svc);
                    }
                    MicroBit.getInstance().setMicrobit_services_discovered(true);
                    if (bluetooth_le_adapter.setIndicationsState(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID), true)) {
                        //showMsg(Utility.htmlColorGreen("UART TX indications ON"));
                        showMsg(Utility.htmlColorGreen("UART受信準備完了"));
                    } else {
                        //showMsg(Utility.htmlColorRed("Failed to set UART TX indications ON"));
                        showMsg(Utility.htmlColorRed("UART受信準備に失敗しました"));
                    }
                    break;
                case BleAdapterService.GATT_CHARACTERISTIC_WRITTEN:
                    Log.d(Constants.TAG, "Handler received characteristic written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    Log.d(Constants.TAG, "characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
                    //showMsg(Utility.htmlColorGreen("Ready"));
                    showMsg(Utility.htmlColorGreen("通信待ち"));
                    break;
                case BleAdapterService.GATT_DESCRIPTOR_WRITTEN:
                    Log.d(Constants.TAG, "Handler received descriptor written result");
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    descriptor_uuid = bundle.getString(BleAdapterService.PARCEL_DESCRIPTOR_UUID);
                    Log.d(Constants.TAG, "descriptor " + descriptor_uuid + " of characteristic " + characteristic_uuid + " of service " + service_uuid + " written OK");
                    if (!exiting) {
                        //showMsg(Utility.htmlColorGreen("UART TX indications ON"));
                        showMsg(Utility.htmlColorGreen("UART受信ON"));
                        indications_on=true;
                    } else {
                        //showMsg(Utility.htmlColorGreen("UART TX indications OFF"));
                        showMsg(Utility.htmlColorGreen("UART受信OFF"));
                        indications_on=false;
                        finish();
                    }
                    break;

                case BleAdapterService.NOTIFICATION_OR_INDICATION_RECEIVED:
                    bundle = msg.getData();
                    service_uuid = bundle.getString(BleAdapterService.PARCEL_SERVICE_UUID);
                    characteristic_uuid = bundle.getString(BleAdapterService.PARCEL_CHARACTERISTIC_UUID);
                    b = bundle.getByteArray(BleAdapterService.PARCEL_VALUE);
                    Log.d(Constants.TAG, "Value=" + Utility.byteArrayAsHexString(b));
                    if (characteristic_uuid.equalsIgnoreCase((Utility.normaliseUUID(BleAdapterService.UART_TX_CHARACTERISTIC_UUID)))) {
                        String text="";
                        Log.d(Constants.TAG, "UART TX received");
                        try {
                            //ascii = new String(b,"US-ASCII");
                            text = Utility.getStringWithEncode(b);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            //showMsg(Utility.htmlColorGreen("Could not convert TX data to ASCII"));
                            showMsg(Utility.htmlColorGreen("受信したデータのエンコードに失敗しました"));
                            return;
                        }
                        Log.d(Constants.TAG, "micro:bit answer: " + text);
                        if(text.startsWith("t:") || text.startsWith("h:") || text.startsWith("p:")){
                            setData(text);
                        }else {
                            showAnswer(text);
                        }
                        /*
                        if (!text.equals(Constants.AVM_CORRECT_RESPONSE)) {
                            showAnswer(text);
                        } else {
                            showAnswer(text+" You only needed "+guess_count+" guesses!");
                        }
                        */
                    }
                    break;
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    showMsg(Utility.htmlColorRed(text));
            }
        }
    };

    private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) UartAvmActivity.this.findViewById(R.id.message)).setText(Html.fromHtml(msg));
            }
        });
    }

    private void setData(final String data){
        Log.d(Constants.TAG, data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(data.startsWith("t:")) {
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Temp)).setText(data.substring(2));
                    ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Temp)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Temp)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_TempSym)).setVisibility(View.VISIBLE);
                }
                if(data.startsWith("h:")) {
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Humi)).setText(data.substring(2));
                    ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Humi)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Humi)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_HumiSym)).setVisibility(View.VISIBLE);
                }
                if(data.startsWith("p:")) {
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Pres)).setText(data.substring(2));
                    ((ImageView) UartAvmActivity.this.findViewById(R.id.imageView_Pres)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_Pres)).setVisibility(View.VISIBLE);
                    ((TextView) UartAvmActivity.this.findViewById(R.id.textView_PresSym)).setVisibility(View.VISIBLE);
                }
            }
        });
    }


    private void showAnswer(String answer) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("通知");
        builder.setMessage(answer);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }
/*
    private void showGuessCount() {
        final int gc = guess_count;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) UartAvmActivity.this.findViewById(R.id.avm_guess_count)).setText("Guesses: "+Integer.toString(gc));
            }
        });
    }
*/
    @Override
    public void connectionStatusChanged(boolean connected) {
        if (connected) {
            //showMsg(Utility.htmlColorGreen("Connected"));
            showMsg(Utility.htmlColorGreen("接続しました"));
        } else {
            //showMsg(Utility.htmlColorRed("Disconnected"));
            showMsg(Utility.htmlColorRed("接続が切れました"));
        }
    }

    @Override
    public void serviceDiscoveryStatusChanged(boolean new_state) {
    }
/*
    public void onSendText(View view) {
        Log.d(Constants.TAG, "onSendText");
        EditText text = (EditText) UartAvmActivity.this.findViewById(R.id.avm_question_text);
        Log.d(Constants.TAG, "onSendText: " + text.getText().toString());
        try {
            String question = text.getText().toString() + ":";
            byte[] ascii_bytes = question.getBytes("US-ASCII");
            Log.d(Constants.TAG, "ASCII bytes: 0x" + Utility.byteArrayAsHexString(ascii_bytes));
            bluetooth_le_adapter.writeCharacteristic(Utility.normaliseUUID(BleAdapterService.UARTSERVICE_SERVICE_UUID), Utility.normaliseUUID(BleAdapterService.UART_RX_CHARACTERISTIC_UUID), ascii_bytes);
            guess_count++;
            showGuessCount();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            showMsg("Unable to convert text to ASCII bytes");
        }
    }

    public void onNewGame() {
        Log.d(Constants.TAG, "onNewGame");
        guess_count = 0;
        showGuessCount();
    }

 */

}