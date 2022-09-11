package com.borax12.mg_project;

// import android
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// import java
import java.util.ArrayList;
import java.util.List;

public class USERINFO_1_Activity <permission_list> extends AppCompatActivity
{
    //UI Button parameters
    Button Update;
    EditText TIMESTAMP;
    EditText Device_ID;
    EditText Name;
    EditText Option1;
    EditText Option2;
    EditText Option3;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userinfo_1_activity);

        //connected to Layout(UI)
        Update = (Button)findViewById(R.id.Update);
        TIMESTAMP = (EditText)findViewById(R.id.U_timestamp);
        Device_ID = (EditText)findViewById(R.id.U_device_id);
        Name = (EditText)findViewById(R.id.U_name);
        Option1 = (EditText)findViewById(R.id.U_option1);
        Option2 = (EditText)findViewById(R.id.U_option2);
        Option3 = (EditText)findViewById(R.id.U_option3);

        // get Userinfo data
        String userinfo = ((BLE_2_Activity)BLE_2_Activity.mContext).User_info;

        // set split data by using ","
        String[] userinfoArray = userinfo.split(",");

        // generate array list
        List<String> userinfoArrayList = new ArrayList<>();

        for (int i = 0; i < userinfoArray.length; i++)
        {
            // add the extract DB value(user_info)
            userinfoArrayList.add(userinfoArray[i]);
        }

        // set layout value
        TIMESTAMP.setText(userinfoArray[0]);
        Device_ID.setText(userinfoArray[1]);
        Name.setText(userinfoArray[2]);
        Option1.setText(userinfoArray[3]);
        Option2.setText(userinfoArray[4]);
        Option3.setText(userinfoArray[5]);
    }

    //SQLite update
    public void Update(View v)
    {
        // for insert qry.
        String S_TIMESTAMP = TIMESTAMP.getText().toString();
        String S_Device_ID = Device_ID.getText().toString();
        String S_Name = Name.getText().toString();
        String S_Option1 = Option1.getText().toString();
        String S_Option2 = Option2.getText().toString();
        String S_Option3 = Option3.getText().toString();

        // send(update command)
        String user_update_command = ("UPDATE user_data SET TIMESTAMP = " + "'" + S_TIMESTAMP + "'" + "," + " Device_ID = " + "'" + S_Device_ID + "'" + ","
                + " Name = " + "'" + S_Name + "'" + "," + " Option1 = " + "'" + S_Option1 + "'" + "," + " Option2 = " + "'" + S_Option2 + "'" + ","
                + " Option3 = " + "'" + S_Option3 + "'" + ";");

        // generated String transmit to SPP java server
        // call-back send func() in BLE_2_Activity
        ((BLE_2_Activity)BLE_2_Activity.mContext).sendMessage(user_update_command);

        Update.setEnabled(true);

        Toast.makeText(getApplicationContext(), "변경된 사용자 정보를 갱신하였습니다.", Toast.LENGTH_SHORT).show();
    }
}
