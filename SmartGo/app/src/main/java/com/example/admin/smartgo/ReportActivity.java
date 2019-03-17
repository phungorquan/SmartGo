package com.example.admin.smartgo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ReportActivity extends AppCompatActivity {

    Button btnReport;
    EditText edt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        edt = (EditText) findViewById(R.id.xxx);
        btnReport = (Button) findViewById(R.id.btnReport);



        btnReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = edt.getText().toString();
                String[] to = {"16520985@gm.uit.edu.vn"};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Report!!!");
                emailIntent.putExtra(Intent.EXTRA_TEXT, message);
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                    finish();
                }
                catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(ReportActivity.this, "Your device does not support email app", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
