package com.borax12.mg_project;

/*
BLE 2nd Activity Manual - Connection to the Java SSP server installed in Latte Panda
                        - Android Studio (Client) - Latte Panda (Server)
*/

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BLE_2_Activity <permission_list> extends AppCompatActivity
{
    // Sharing between adjacent activity functions
    public static Context mContext;

    // Sharing between adjacent activity variable
    public static String Remain;
    public static String User_info;
    public static String Power;
    public static String Stamp_start;
    public static String Stamp_end;
    public static String Expected;

    // Sharing between adjacent MG_Main_Activity
    public static Boolean check_REBOL = false;

    private final int REQUEST_BLUETOOTH_ENABLE = 100;

    private TextView mConnectionStatus;
    private EditText mInputEditText;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_2_activity);

        mContext = this;

        Button sendButton = (Button)findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                String sendMessage = mInputEditText.getText().toString();
                if ( sendMessage.length() > 0 )
                {
                    sendMessage(sendMessage);
                }
            }
        });

        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);
        mInputEditText = (EditText)findViewById(R.id.input_string_edittext);
        ListView mMessageListview = (ListView) findViewById(R.id.message_listview);

        mConversationArrayAdapter = new ArrayAdapter<>(this, R.layout.fontsize_listview);

        mMessageListview.setAdapter(mConversationArrayAdapter);

        Log.d( TAG, "Initalizing Bluetooth adapter...");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
        {
            showErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialisation successful.");
            showPairedDevicesListDialog();
        }
    }

    @Override
    protected void onDestroy() { super.onDestroy();}


    private class ConnectTask extends AsyncTask<Void, Void, Boolean>
    {
        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice)
        {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP protocol
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try
            {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e)
            {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }

            mConnectionStatus.setText("기다려주세요...");
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try
            {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e)
            {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2)
                {
                    Log.e(TAG, "unable to close() " + " socket during connection failure", e2);
                }
                return false;
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean isSucess)
        {
            if ( isSucess )
            {
                connected(mBluetoothSocket);
            }
            else
            {
                isConnectionError = true;
                Log.d( TAG,  "해당 디바이스와 연결할 수 없습니다.");
                showErrorDialog("해당 디바이스와 연결할 수 없습니다.");
            }
        }
    }


    public void connected( BluetoothSocket socket )
    {
        mConnectedTask = new ConnectedTask(socket);
        mConnectedTask.execute();
    }



    private class ConnectedTask extends AsyncTask<Void, String, Boolean>
    {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket)
        {

            mBluetoothSocket = socket;
            try
            {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e)
            {
                Log.e(TAG, "socket not created", e );
            }

            Log.d( TAG, mConnectedDeviceName);
            mConnectionStatus.setText(mConnectedDeviceName);
        }


        @Override
        protected Boolean doInBackground(Void... params)
        {

            byte [] readBuffer = new byte[8*1024]; // 2* (logger_max_buffer)
            int readBufferPosition = 0;
            String POWER_sum = "";
            String EXPECT_sum = "";

            while (true)
            {
                if ( isCancelled() ) return false;

                try
                {

                    int bytesAvailable = mInputStream.available();

                    if(bytesAvailable > 0)
                    {

                        byte[] packetBytes = new byte[bytesAvailable];

                        mInputStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++)
                        {

                            byte b = packetBytes[i];
                            if(b == '\n')
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);

                                // run(1), get update value and 'R: Remain_Power'
                                if (recvMessage.contains("R:"))
                                {
                                    String temp_remain = recvMessage;
                                    temp_remain = temp_remain.replaceAll("R:", "");
                                    Remain = temp_remain;
                                }

                                // run(1), get update value and 'U: User_data'
                                else if (recvMessage.contains("U:"))
                                {
                                    String temp_user_info = recvMessage;
                                    temp_user_info = temp_user_info.replaceAll("U:", "");
                                    User_info = temp_user_info;
                                }

                                // run(slice_size) get update value and 'XS:' -> start timestamp index
                                else if (recvMessage.contains("XS:"))
                                {
                                    String temp_start = recvMessage;
                                    temp_start = temp_start.replaceAll("XS:", "");
                                    Stamp_start = temp_start;
                                }

                                // run(slice_size) get update value and 'XE:' -> end timestamp index
                                else if (recvMessage.contains("XE:"))
                                {
                                    String temp_end = recvMessage;
                                    temp_end = temp_end.replaceAll("XE:", "");
                                    Stamp_end = temp_end;
                                }

                                // run(slice_size) get update value and 'P:' -> Power value in PowerUsage_data
                                else if (recvMessage.contains("P:"))
                                {
                                    String temp_power = recvMessage;
                                    temp_power = temp_power.replaceAll("P:", "");
                                    if(!recvMessage.contains(" "))
                                    {
                                        POWER_sum += temp_power;
                                    }
                                    Power = POWER_sum;
                                }

                                // run(silce_size) get update value and 'EX' -> Expected(User selected) value in PowerUsage_data
                                else if (recvMessage.contains("EX:"))
                                {
                                    String temp_expected = recvMessage;
                                    temp_expected = temp_expected.replaceAll("EX:", "");
                                    if(!recvMessage.contains(" "))
                                    {
                                        EXPECT_sum += temp_expected;
                                    }
                                    Expected = EXPECT_sum;
                                }
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e)
                {
                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... recvMessage)
        {
            mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);
        }

        @Override
        protected void onPostExecute(Boolean isSucess)
        {
            super.onPostExecute(isSucess);
            if ( !isSucess )
            {
                closeSocket();
                Log.d(TAG, "디바이스와 연결이 끊어졌습니다");
                isConnectionError = true;
                showErrorDialog("디바이스와 연결이 끊어졌습니다");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean)
        {
            super.onCancelled(aBoolean);
            closeSocket();
        }

        void closeSocket()
        {
            try {

                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2)
            {
                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg)
        {
            msg += "\n";

            try
            {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e)
            {
                Log.e(TAG, "Exception during send", e );
            }

            mInputEditText.setText(" ");
        }
    }


    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 )
        {
            showQuitDialog( "페어링된 디바이스가 없습니다.\n" + "다른 기기와 페어링을 연결 해주세요");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++)
        {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("페어링된 디바이스");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                if (isConnectionError)
                {
                    isConnectionError = false;
                    finish();
                }
            }
        });
        builder.create().show();
    }

    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    void sendMessage(String msg)
    {
        if ( mConnectedTask != null )
        {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
            mConversationArrayAdapter.insert("Me:  " + msg, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_BLUETOOTH_ENABLE)
        {
            if (resultCode == RESULT_OK)
            {
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if (resultCode == RESULT_CANCELED)
            {
                showQuitDialog("블루투스를 활성화해야 합니다.");
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        // BLE ready ? checker
        if(check_REBOL == false) { check_REBOL = true; }
        Intent intent = new Intent(getApplicationContext(), MG_Main_Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
