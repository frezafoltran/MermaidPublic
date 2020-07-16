package com.foltran.mermaid.ui.login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.foltran.mermaid.MainActivity;
import com.foltran.mermaid.R;
import com.foltran.mermaid.database.requests.UpdateUserInfo;

import java.util.concurrent.ExecutionException;

public class UsernameActivity extends AppCompatActivity {


    TextView nextBtn;
    ImageView nextBtnIcon;
    EditText curUsername;
    ProgressBar progressBar;

    String warningText = "You must type a valid username";
    String errorMsg = "An error occured, please try again";
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);

        nextBtn = findViewById(R.id.go_to_next);
        nextBtnIcon = findViewById(R.id.go_to_next_icon);

        curUsername = findViewById(R.id.cur_username);
        progressBar = findViewById(R.id.progress_bar);
        context = this;


        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (checkUserName(curUsername.getText().toString())){
                    nextBtn.setVisibility(View.GONE);
                    nextBtnIcon.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);

                    UpdateUserInfo updateUserInfoRequest = new UpdateUserInfo(null, null, null);

                    String out;
                    try {
                        out = updateUserInfoRequest.execute(curUsername.getText().toString()).get();
                        Intent intent = new Intent(context, MainActivity.class);
                        startActivity(intent);

                    } catch (ExecutionException | InterruptedException e) {

                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        nextBtn.setVisibility(View.VISIBLE);
                        nextBtnIcon.setVisibility(View.VISIBLE);
                    }
                }
                else{
                    Toast.makeText(context, warningText, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private Boolean checkUserName(String username){
        return username.length() > 0;
    }
}
