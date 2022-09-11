package com.borax12.mg_project;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// import java
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// get graph library
import im.dacer.androidcharts.LineView;

public class TRACKER_1_Activity extends AppCompatActivity
{
    // display remain power
    TextView remain;

    // display multiple graph
    private LineView lineView;

    // temp_store_mg_main_activity values
    private String s1 = "";
    private String s2 = "";
    private String startDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tracker_1_activity);

        // connect to layout(textview)
        remain = (TextView) findViewById(R.id.remain);
        lineView = (LineView) findViewById(R.id.line_view);

        // get MG_main_activity boolean
        Boolean DAY = MG_Main_Activity.check_DAYBOL;
        Boolean TIME = MG_Main_Activity.check_TIMEBOL;

        // get stamp start-end value
        s1 = BLE_2_Activity.Stamp_start;
        s2 = BLE_2_Activity.Stamp_end;

        // ","-based get stamp range
        String [] s1_arr = s1.split(",");
        String [] s2_arr = s2.split(",");

        // generate the Array list
        ArrayList<String> start_list = new ArrayList<>();
        ArrayList<String> end_list = new ArrayList<>();

        // generate main simple date format (String convert to date type)
        DateFormat dataToformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // generate new date format
        DateFormat DateToFormat;
        DateFormat TimeToFormat;

        // user-defined date and time format
        DateToFormat = new SimpleDateFormat("MM/dd");
        TimeToFormat = new SimpleDateFormat("HH:mm");

        if (DAY == true)
        {
            try
            {
                for (int i = 0; i< s2_arr.length; i++)
                {
                    // add time stamp range by using index(i)
                    start_list.add(s1_arr[i]);
                    end_list.add(s2_arr[i]);

                    // string convert to date type
                    Date d1 = dataToformat.parse(s1_arr[i]);

                    // cumulative timestamp range
                    startDate += (DateToFormat.format(d1) + ",");
                }
            } catch (ParseException e) { e.printStackTrace(); }

        }

        else if (TIME == true)
        {
            try
            {
                for (int i = 0; i< s2_arr.length; i++)
                {
                    // add time stamp range by using index(i)
                    start_list.add(s1_arr[i]);
                    end_list.add(s2_arr[i]);

                    // string convert to date type
                    Date d1 = dataToformat.parse(s1_arr[i]);

                    // cumulative timestamp range
                    startDate += (TimeToFormat.format(d1) + ",");
                }
            } catch (ParseException e) { e.printStackTrace(); }

        }

        // split the each value
        String[] ST_ARR = startDate.split(",");
        String[] LP_ARR = BLE_2_Activity.Power.split(",");
        String[] EX_ARR = BLE_2_Activity.Expected.split(",");

        // x-axis label
        ArrayList<String> TIME_ARRLIST = new ArrayList<String>();

        // 2 col. data sets
        ArrayList<Integer> POWER = new ArrayList<>();
        ArrayList<Integer> EXPECTED = new ArrayList<>();

        for (int i = 0; i < ST_ARR.length; i++)
        {
            TIME_ARRLIST.add(ST_ARR[i]);
            POWER.add(Integer.parseInt(LP_ARR[i]));
            EXPECTED.add(Integer.parseInt(EX_ARR[i]));
        }
        // put data sets into data list
        ArrayList<ArrayList<Integer>> dataLists = new ArrayList<>();
        dataLists.add(POWER);
        dataLists.add(EXPECTED);

        // display x-axis (Time range)
        lineView.setBottomTextList(TIME_ARRLIST);
        // display y-axis (Power, Expected Power)
        lineView.setDataList(dataLists);
        // draw line graph
        lineView.setDrawDotLine(true);
        // setting a color
        lineView.setColorArray(new int[]{Color.parseColor("#1a76b9"), Color.parseColor("#2DA7AD")});
        // display marker
        lineView.setShowPopup(LineView.SHOW_POPUPS_All);

        // for update remain_purchased power (current value is 330kWh)
        String remain_listen = BLE_2_Activity.Remain;
        double Con_str = Double.parseDouble(remain_listen);
        remain_listen = String.format("%.0f", Con_str);
        String standard = "kWh";
        remain_listen = (remain_listen + standard);
        remain.setText(remain_listen);
    }

    @Override
    public void onBackPressed()
    {
        Intent intent = new Intent(getApplicationContext(), MG_Main_Activity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}