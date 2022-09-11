package com.borax12.mg_project;

// import android
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Message;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.DialogInterface;
import android.util.Log;

// import external library (date picker)
import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.borax12.materialdaterangepicker.time.RadialPickerLayout;
import com.borax12.materialdaterangepicker.time.TimePickerDialog;

// import java
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class MG_Main_Activity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener
{

    // Sharing between adjacent activity functions
    public static Context pContext;

    // load the DB data (display status)
    private ProgressDialog pBar;

    //for alert Thread-based progress bar
    private ThreadEx threadEx;

    // for check the day or hour
    public static Boolean check_DAYBOL = false;
    public static Boolean check_TIMEBOL = false;

    // Make sure to use the FloatingActionButton
    FloatingActionButton mAddBook, mAddTimebase, mAddMonthbase;

    // to check whether sub FAB buttons are visible or not.
    Boolean isAllFabsVisible;

    // for selection of user selected time or date range
    //date-base
    String date_query; // 'Start_date' |and| 'End_date'
    // time-base
    String time_query; // 'Start_time' *and* 'End_time'

    // composition of main_activity => UI (User to App interaction)
    Button Bluetooth;
    Button Purchase;
    Button RefreshDB;
    Button Userinfo;
    TextView DB_value;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mg_main_activity);

        // generate pointer (sharing value other activitys)
        pContext = this;

        // display status of Load the DB values
        pBar = new ProgressDialog(MG_Main_Activity.this);

        // set pBar
        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pBar.setMessage("Loading...");
        pBar.setCancelable(false);
        pBar.setCanceledOnTouchOutside(false);

        // connected to Layout(UI)
        Bluetooth = (Button) findViewById(R.id.Bluetooth);
        Purchase = (Button) findViewById(R.id.Purchase);
        RefreshDB = (Button) findViewById(R.id.RefreshDB);
        Userinfo = (Button) findViewById(R.id.Userinfo);
        DB_value = (TextView) findViewById(R.id.DB_value);

        // This float action button is the Parent
        mAddBook = findViewById(R.id.mAddBook);

        // Select range time || month button
        mAddTimebase = findViewById(R.id.mAddTimebase);
        mAddMonthbase = findViewById(R.id.mAddMonthbase);

        // texts as GONE
        mAddTimebase.setVisibility(View.GONE);
        mAddMonthbase.setVisibility(View.GONE);

        // make the boolean variable as false, as all the
        isAllFabsVisible = false;

        // click the button
        mAddBook.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // display UI
                if (!isAllFabsVisible) { mAddTimebase.show(); mAddMonthbase.show(); isAllFabsVisible = true; }
                else { mAddTimebase.hide(); mAddMonthbase.hide(); isAllFabsVisible = false; }
            }
        });

        mAddTimebase.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // alert the toast message
                Toast.makeText(getApplicationContext(), "입력하신 시간 기준으로 값을 시각화합니다.", Toast.LENGTH_SHORT).show();

                // get calendar instance
                Calendar now = Calendar.getInstance();

                // set the timepicker dialog
                TimePickerDialog tpd = TimePickerDialog.newInstance
                        (
                                MG_Main_Activity.this, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), false
                        );

                // set dialog listener
                tpd.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) { Log.d("TimePicker", "Dialog was cancelled"); }
                });

                // show this fragments
                tpd.show(getFragmentManager(), "Timepickerdialog");
            }
        });

        mAddMonthbase.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // alert the toast message
                Toast.makeText(getApplicationContext(), "입력하신 날짜 기준으로 값을 시각화합니다.", Toast.LENGTH_SHORT).show();

                // get calendar instance
                Calendar now = Calendar.getInstance();

                // set the datepicker dialog
                DatePickerDialog dpd = DatePickerDialog.newInstance
                        (
                                MG_Main_Activity.this, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)
                        );

                // show this fragments
                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        // click the bluetooth button
        Bluetooth.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {   // Go to BLE_1_Activity
                Intent intent = new Intent(getApplicationContext(), BLE_1_Activity.class); startActivity(intent);
            }
        });

        // click the purchase button
        Purchase.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Go to PURCHASE_1_Activity
                Intent intent = new Intent(getApplicationContext(), PURCHASE_1_Activity.class); startActivity(intent);
            }
        });

        // click the refresh image button
        RefreshDB.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // send message to SPP server
                ((BLE_2_Activity) BLE_2_Activity.mContext).sendMessage("refresh");

                // get remain_power value
                String listener = BLE_2_Activity.Remain;

                // convert double to string type
                double Con_str = Double.parseDouble(listener);

                // for display int type
                listener = String.format("%.0f", Con_str);

                // add the standard value
                String standard = "kWh";

                // combine value and standard
                listener = (listener + standard);

                // set DB_value
                DB_value.setText(listener);

                // alert the toast message
                Toast.makeText(getApplicationContext(), "남은 사용량을 갱신하였습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // Automatic update remain_power value (at first time)
        if (BLE_2_Activity.check_REBOL == true)
        {
            // send message to SPP server
            ((BLE_2_Activity) BLE_2_Activity.mContext).sendMessage("refresh");

            // get remain_power value
            String listener = BLE_2_Activity.Remain;

            // convert double to string type
            double Con_str = Double.parseDouble(listener);

            // for display int type
            listener = String.format("%.0f", Con_str);

            // add the standard value
            String standard = "kWh";

            // combine value and standard
            listener = (listener + standard);

            // set DB_value
            DB_value.setText(listener);
        }

        // click the Userinfo button
        Userinfo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // send message to SPP server
                ((BLE_2_Activity) BLE_2_Activity.mContext).sendMessage("MYUS");

                // Go to USERINFO_1_Activity
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Intent intent = new Intent(getApplicationContext(), USERINFO_1_Activity.class);
                        startActivity(intent);
                    }
                }, 500);
            }
        });
    }

    // set init picker dialog
    @Override
    public void onResume()
    {
        super.onResume();
        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");
        if (dpd != null) dpd.setOnDateSetListener(this);
    }

    // set Date picker
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth, int yearEnd, int monthOfYearEnd, int dayOfMonthEnd)
    {
        // boolean-checker
        if(check_DAYBOL == false) { check_DAYBOL = true; }

        // static value
        String fixstartDate = " 00:00:00";
        String fixendDate = " 23:59:59";

        // set start month, day
        String StrmonthOfYear = monthOfYear < 9 ? "0" + (++monthOfYear) : "" + (++monthOfYear);
        String StrdayOfYear = dayOfMonth < 9 ? "0" + (++dayOfMonth) : "" + (++dayOfMonth);

        // set end month, day
        String StrmonthOfYearEnd = monthOfYearEnd < 9 ? "0" + (++monthOfYearEnd) : "" + (++monthOfYearEnd);
        String StrdayOfYearEnd = dayOfMonthEnd < 9 ? "0" + (++dayOfMonthEnd) : "" + (++dayOfMonthEnd);

        // for Date-based select query
        String startDateString = (year + "-" + StrmonthOfYear + "-" + StrdayOfYear + fixstartDate);
        String endDateString = (yearEnd + "-" + StrmonthOfYearEnd + "-" + StrdayOfYearEnd + fixendDate);

        // send date-based query
        date_query = (startDateString + "|and|" + endDateString);

        // send message to SPP server
        ((BLE_2_Activity) BLE_2_Activity.mContext).sendMessage(date_query);

        // set progressbar
        pBar.show();threadEx = new ThreadEx(handler);threadEx.start();
    }

    // set Time picker
    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int hourOfDayEnd, int minuteEnd)
    {

        // boolean-checker
        if(check_TIMEBOL == false) { check_TIMEBOL = true; }

        // alert the toast message
        Toast.makeText(getApplicationContext(), "날짜는 오늘로 고정됩니다.", Toast.LENGTH_LONG).show();

        // get TIMESTAMP
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
        String timestamp = date.format(new Date());

        // get calculation of hour:minute:msec
        String hourString = hourOfDay < 10 ? "0" + hourOfDay : "" + hourOfDay;
        String minuteString = minute < 10 ? "0" + minute : "" + minute;
        String hourStringEnd = hourOfDayEnd < 10 ? "0" + hourOfDayEnd : "" + hourOfDayEnd;
        String minuteStringEnd = minuteEnd < 10 ? "0" + minuteEnd : "" + minuteEnd;
        String msecString = "00";

        // for use query
        String startTimeString = (timestamp + " " + hourString + ":" + minuteString + ":" + msecString);
        String endTimeString = (timestamp + " " + hourStringEnd + ":" + minuteStringEnd + ":" + msecString);

        // send time-based query
        time_query = (startTimeString + "*and*" + endTimeString);

        // SPP server return count(*)
        ((BLE_2_Activity) BLE_2_Activity.mContext).sendMessage(time_query);

        // set progressbar
        pBar.show();threadEx = new ThreadEx(handler); threadEx.start();
    }

    // Alert error dialog message
    void show()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("조회 기간을 다시 설정해주세요.");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                Toast.makeText(getApplicationContext(), "기존 화면으로 전환합니다.", Toast.LENGTH_LONG).show();
            }
        });
        builder.show();
    }

    // set progress bar handler
    Handler handler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            int value = msg.getData().getInt("value");
            pBar.setProgress(value);
            super.handleMessage(msg);
            pBar.setMessage("처리중입니다...");
            if (value >= 100)
            {
                pBar.dismiss();
                threadEx.setCheck(false);
                Intent intent = new Intent(getApplicationContext(), TRACKER_1_Activity.class);
                startActivity(intent);
            }
        }
    };

    @Override
    public void onBackPressed()
    {
        Toast.makeText(getApplicationContext(), "어플리케이션을 종료합니다.", Toast.LENGTH_LONG).show();
        ActivityCompat.finishAffinity(this);
        System.exit(0);
    }
}

