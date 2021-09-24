package br.ribeiro.unisaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

public class ContatoActivity extends AppCompatActivity {

    private WebView webContato;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contato);

        webContato = findViewById(R.id.webContato);

        LayoutInflater inflater = getLayoutInflater();
        View layout_toast = inflater.inflate(R.layout.toast,(ViewGroup) findViewById(R.id.toast_layout_root));
        TextView text_toast = layout_toast.findViewById(R.id.text_layout);
        layout_toast.setBackgroundColor(Color.parseColor("#FF0071B2"));

        toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL,0,0);
        toast.setView(layout_toast);

        text_toast.setText("OBRIGADO PELO SEU CONTATO");
        text_toast.setTextColor(Color.WHITE);
        text_toast.setTextSize(23);

        webContato.getSettings().setJavaScriptEnabled(true);

        webContato.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                String url = request.getUrl().toString();

                view.loadUrl(url);

                return false;
            }
        });
        webContato.loadUrl("https://docs.google.com/forms/d/e/1FAIpQLSfcF8G381tDzHJDtOPt6E4aopqKHnYjGjPbQW-aPSfk2X0ewg/viewform?usp=sf_link");


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK:

                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);
                    toast.show();
                    finish();

                    return true;
            }
        }


        return super.onKeyDown(keyCode, event);
    }


}