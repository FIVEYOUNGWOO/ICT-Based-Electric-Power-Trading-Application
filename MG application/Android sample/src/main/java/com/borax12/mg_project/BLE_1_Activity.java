package com.borax12.mg_project;

// import android
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

// import java
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BLE_1_Activity<permission_list> extends AppCompatActivity
{

    //BluetoothAdapter
    BluetoothAdapter mBluetoothAdapter;

    //bluetooth activity code
    final static int BLUETOOTH_REQUEST_CODE = 100;

    //set layout
    TextView txtState;
    Button btnSearch;
    CheckBox chkFindme;
    ListView listPaired;
    ListView listDevice;
    Button blueconnect;

    //Adapter
    SimpleAdapter adapterPaired;
    SimpleAdapter adapterDevice;

    //bluetooth list - Device
    List<Map<String,String>> dataPaired;
    List<Map<String,String>> dataDevice;
    List<BluetoothDevice> bluetoothDevices;
    int selectDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ble_1_activity);

        // Control of SDK version
        if(Build.VERSION.SDK_INT>=23)
        {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED)
            {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION))
                {
                    Toast.makeText(this, "ble_need", Toast.LENGTH_SHORT).show();
                }
                ActivityCompat.requestPermissions(this ,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
                return;
            }
        }

        //UI
        txtState = (TextView)findViewById(R.id.txtState);
        chkFindme = (CheckBox)findViewById(R.id.chkFindme);
        btnSearch = (Button)findViewById(R.id.btnSearch);
        listPaired = (ListView)findViewById(R.id.listPaired);
        listDevice = (ListView)findViewById(R.id.listDevice);
        blueconnect = findViewById(R.id.blueconnect);

        //Adapter1
        dataPaired = new ArrayList<>();
        adapterPaired = new SimpleAdapter(this, dataPaired, android.R.layout.simple_list_item_2, new String[]{"name","address"}, new int[]{android.R.id.text1, android.R.id.text2});
        listPaired.setAdapter(adapterPaired);

        //Adapter2
        dataDevice = new ArrayList<>();
        adapterDevice = new SimpleAdapter(this, dataDevice, android.R.layout.simple_list_item_2, new String[]{"name","address"}, new int[]{android.R.id.text1, android.R.id.text2});
        listDevice.setAdapter(adapterDevice);

        //searched bluetooth devices
        bluetoothDevices = new ArrayList<>();

        // not select device list
        selectDevice = -1;

        //access bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "블루투스를 지원하지 않는 단말기 입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // receiver 1
        IntentFilter stateFilter = new IntentFilter();
        stateFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStateReceiver, stateFilter);

        // receiver 2
        IntentFilter searchFilter = new IntentFilter();
        searchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        searchFilter.addAction(BluetoothDevice.ACTION_FOUND);
        searchFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        searchFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBluetoothSearchReceiver, searchFilter);

        // receiver 3
        IntentFilter scanmodeFilter = new IntentFilter();
        scanmodeFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBluetoothScanmodeReceiver, scanmodeFilter);

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH_REQUEST_CODE);
        }
        else
        {
            GetListPairedDevice();
        }

        blueconnect.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), BLE_2_Activity.class);
                startActivity(intent);
            }
        });


        listDevice.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                BluetoothDevice device = bluetoothDevices.get(position);

                try
                {
                    //선택한 디바이스 페어링 요청
                    Method method = device.getClass().getMethod("createBond", (Class[]) null);
                    method.invoke(device, (Object[]) null);
                    selectDevice = position;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }



    //블루투스 상태변화 BroadcastReceiver
    BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //BluetoothAdapter.EXTRA_STATE : 블루투스의 현재상태 변화
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

            if(state == BluetoothAdapter.STATE_ON)
            {
                txtState.setText("블루투스 활성화");
            }
            else if(state == BluetoothAdapter.STATE_TURNING_ON)
            {
                txtState.setText("블루투스 활성화 중...");
            }
            else if(state == BluetoothAdapter.STATE_OFF)
            {
                txtState.setText("블루투스 비활성화");
            }
            else if(state == BluetoothAdapter.STATE_TURNING_OFF)
            {
                txtState.setText("블루투스 비활성화 중...");
            }
        }
    };

    //블루투스 검색결과 BroadcastReceiver
    BroadcastReceiver mBluetoothSearchReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            switch(action)
            {
                //블루투스 디바이스 검색 종료
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    dataDevice.clear();
                    bluetoothDevices.clear();
                    Toast.makeText(BLE_1_Activity.this, "블루투스 검색 시작", Toast.LENGTH_SHORT).show();
                    break;
                //블루투스 디바이스 찾음
                case BluetoothDevice.ACTION_FOUND:
                    //검색한 블루투스 디바이스의 객체를 구한다
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //데이터 저장
                    Map map = new HashMap();
                    map.put("name", device.getName()); //device.getName() : 블루투스 디바이스의 이름
                    map.put("address", device.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                    dataDevice.add(map);
                    //리스트 목록갱신
                    adapterDevice.notifyDataSetChanged();

                    //블루투스 디바이스 저장
                    bluetoothDevices.add(device);
                    break;
                //블루투스 디바이스 검색 종료
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Toast.makeText(BLE_1_Activity.this, "블루투스 검색 종료", Toast.LENGTH_SHORT).show();
                    btnSearch.setEnabled(true);
                    break;
                //블루투스 디바이스 페어링 상태 변화
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    BluetoothDevice paired = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(paired.getBondState()==BluetoothDevice.BOND_BONDED)
                    {
                        //데이터 저장
                        Map map2 = new HashMap();
                        map2.put("name", paired.getName()); //device.getName() : 블루투스 디바이스의 이름
                        map2.put("address", paired.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                        dataPaired.add(map2);
                        //리스트 목록갱신
                        adapterPaired.notifyDataSetChanged();

                        //검색된 목록
                        if(selectDevice != -1)
                        {
                            bluetoothDevices.remove(selectDevice);

                            dataDevice.remove(selectDevice);
                            adapterDevice.notifyDataSetChanged();
                            selectDevice = -1;
                        }
                    }
                    break;
            }
        }
    };

    //블루투스 검색응답 모드 BroadcastReceiver
    BroadcastReceiver mBluetoothScanmodeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
            switch (state)
            {
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                case BluetoothAdapter.SCAN_MODE_NONE:
                    chkFindme.setChecked(false);
                    chkFindme.setEnabled(true);
                    Toast.makeText(BLE_1_Activity.this, "검색응답 모드 종료", Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    Toast.makeText(BLE_1_Activity.this, "다른 블루투스 기기에서 내 휴대폰을 찾을 수 있습니다.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };


    //블루투스 검색 버튼 클릭
    public void mOnBluetoothSearch(View v){
        //검색버튼 비활성화
        btnSearch.setEnabled(false);
        //mBluetoothAdapter.isDiscovering() : 블루투스 검색중인지 여부 확인
        //mBluetoothAdapter.cancelDiscovery() : 블루투스 검색 취소
        if(mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
        }
        //mBluetoothAdapter.startDiscovery() : 블루투스 검색 시작
        mBluetoothAdapter.startDiscovery();
    }


    //검색응답 모드 - 블루투스가 외부 블루투스의 요청에 답변하는 슬레이브 상태
    //BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE : 검색응답 모드 활성화 + 페이지 모드 활성화
    //BluetoothAdapter.SCAN_MODE_CONNECTABLE : 검색응답 모드 비활성화 + 페이지 모드 활성화
    //BluetoothAdapter.SCAN_MODE_NONE : 검색응답 모드 비활성화 + 페이지 모드 비활성화
    //검색응답 체크박스 클릭
    public void mOnChkFindme(View v)
    {
        //검색응답 체크
        if(chkFindme.isChecked())
        {
            if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
            { //검색응답 모드가 활성화이면 하지 않음
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);  //60초 동안 상대방이 나를 검색할 수 있도록한다
                startActivity(intent);
            }
        }
    }

    //이미 페어링된 목록 가져오기
    public void GetListPairedDevice()
    {
        Set<BluetoothDevice> pairedDevice = mBluetoothAdapter.getBondedDevices();

        dataPaired.clear();
        if(pairedDevice.size() > 0)
        {
            for(BluetoothDevice device : pairedDevice)
            {
                //데이터 저장
                Map map = new HashMap();
                map.put("name", device.getName()); //device.getName() : 블루투스 디바이스의 이름
                map.put("address", device.getAddress()); //device.getAddress() : 블루투스 디바이스의 MAC 주소
                dataPaired.add(map);
            }
        }
        //리스트 목록갱신
        adapterPaired.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case BLUETOOTH_REQUEST_CODE:
                //블루투스 활성화 승인
                if (resultCode == Activity.RESULT_OK)
                {
                    GetListPairedDevice();
                }
                //블루투스 활성화 거절
                else {
                    Toast.makeText(this, "블루투스를 활성화해야 합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(mBluetoothStateReceiver);
        unregisterReceiver(mBluetoothSearchReceiver);
        unregisterReceiver(mBluetoothScanmodeReceiver);
        super.onDestroy();
    }
}


