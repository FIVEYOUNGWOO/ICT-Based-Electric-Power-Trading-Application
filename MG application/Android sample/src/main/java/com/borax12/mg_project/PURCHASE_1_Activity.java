package com.borax12.mg_project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PURCHASE_1_Activity<permission_list> extends AppCompatActivity
{


    //UI Button parameters
    Button Cash_Payment;
    Button Account_Transfer;
    Button Credit_Card;
    Button Purchase;
    Button Purchased;
    Button Spread;

    // cal of Purchase cost (input * 200)
    EditText Remaining;
    EditText Purchase_Power;
    EditText Payment;
    EditText Payment2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.purchase_1_activity);

        //layout UI
        Cash_Payment = (Button)findViewById(R.id.Cash_Payment);
        Account_Transfer = (Button)findViewById(R.id.Account_Transfer);
        Credit_Card = (Button)findViewById(R.id.Credit_Card);
        Purchase = (Button)findViewById(R.id.Purchase);
        Purchased = (Button)findViewById(R.id.Purchased);

        //layout UI (text)
        Remaining = (EditText)findViewById(R.id.Remaining);
        Purchase_Power = (EditText)findViewById(R.id.Purchase_Power);
        Payment = (EditText) findViewById(R.id.Payment);
        Payment2 = (EditText) findViewById(R.id.Payment2);
        Spread = (Button)findViewById(R.id.Spread);

        // 구매하고자 하는 전력량을 무조건 숫자만 받도록 설계 (자동으로 키패드 제한됨)
        try
        {
            String str = Purchase_Power.getText().toString().trim();
            int num = Integer.parseInt(str);
            Toast.makeText(this, "NUM : "+num, Toast.LENGTH_SHORT).show();
        }
        catch (NumberFormatException e)
        {
            Toast.makeText(this,"숫자만 입력하세요", Toast.LENGTH_SHORT).show();
        }

        // SPP 서버에서 잔여 전력을 Return 해주면, String listener가 받아서 저장
        String listener = ((BLE_2_Activity)BLE_2_Activity.mContext).Remain;
        // 잔여 전력이 소수점 뒤에 모든 값이 출력되므로 형 변환을 거쳐서, 잘라냄
        double Con_str = Double.parseDouble(listener);
        // 소수점 자리수 제거
        listener = String.format("%.0f", Con_str);
        // 단위 추가
        String standard = "kWh";
        // 값과 단위 결합
        listener = (listener + standard);
        // 제거된 값을 Remaining 값에 덮어씌움.
        Remaining.setText(listener);

        Spread.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //temp를 이용하여, 입력된 값을 임시 저장
                String temp = Purchase_Power.getText().toString();

                // 문자열 값을 정수로 변환하고, 200원을 곱하여, int형으로 전달
                int result = (Integer.parseInt(temp) * 200);

                // 사용자 편리를 위해 금액 구분자(콤마) 생성하고, 다시 문자열로 반환
                // 사용하면 쿼리 작성할때, 콤마 다시 제거해줘야함
                DecimalFormat myFormatter = new DecimalFormat("###,###");
                String formattedStringPrice = myFormatter.format(result);

                // 계산된 두 값은 동일
                Payment.setText(formattedStringPrice);
                Payment2.setText(formattedStringPrice);

            }
        });

        Purchased.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(getApplicationContext(),fetchdata.class));
            }
        });
    }

    //현금, 계좌이체, 신용카드 버튼 클릭 이벤트
    public void Cash_Payment(View v)
    {
        Cash_Payment.setEnabled(false);
        Toast.makeText(this, "현금 결제를 선택하셨습니다.", Toast.LENGTH_SHORT).show();
    }

    public void Account_Transfer(View v)
    {
        Account_Transfer.setEnabled(false);
        Toast.makeText(this, "계좌이체를 선택하셨습니다.", Toast.LENGTH_SHORT).show();
    }

    public void Credit_Card(View v)
    {
        Credit_Card.setEnabled(false);
        Toast.makeText(this, "신용카드 결제를 선택하셨습니다.", Toast.LENGTH_SHORT).show();
    }

    // **important func()**
    public void Purchase(View v)
    {
        /* Transmission format (Only use Purchase_command) */
        // get TIMESTAMP
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = date.format(new Date());

        // get Device_ID
        String Device_ID = "A0001";

        // get Name
        String Name = "SFlab";

        // get Credit (user input == Purchase_Power)
        String Credit = Purchase_Power.getText().toString();
        Credit = Credit.replaceAll(",", ""); // DB값에 들어갈때는 콤마 제거

        double Con_str = Double.parseDouble(Credit);
        Con_str = (Con_str * 1000);
        Credit = String.format("%.0f", Con_str);

        // get Used
        String Used = "N";

        // get Used_TIMESTAMP
        String Used_TIMESTAMP = "N";

        //  insert into charge_log
        String purchase_command = ("insert into charge_log(TIMESTAMP, Device_ID, Name, Credit, Used, Used_TIMESTAMP) " +
                "values(" + "'" + timestamp + "'" + "," + "'" + Device_ID + "'" + "," + "'" + Name + "'"
                + "," + "'" + Credit + "'" + "," + "'" + Used + "'" + "," + "'" + Used_TIMESTAMP + "'" + ")" + ";");

        // generated String transmit to SPP java server
        // call-back send func() in BLE_2_Activity
        ((BLE_2_Activity)BLE_2_Activity.mContext).sendMessage(purchase_command); // 사용자가 구매한 전력량 및 금액 등을 SPP 서버에 전달.

        Purchase.setEnabled(true);
        Toast.makeText(this, "결제에 성공하였습니다.", Toast.LENGTH_SHORT).show();
        processinsert(Remaining.getText().toString(),Purchase_Power.getText().toString(),Payment2.getText().toString());
    }

    public void Purchased(View v)
    {
        Purchased.setEnabled(true);
    }

    private void processinsert(String r, String b, String p)
    {
        String res=new dbmanager(this).addrecord(r,b,p);
        Remaining.setText(""); // acq. remain MG
        Purchase_Power.setText(""); // acq. purchase MG
        Payment2.setText(""); // acq. payment
        Toast.makeText(getApplicationContext(),res,Toast.LENGTH_SHORT).show();
    }

}
