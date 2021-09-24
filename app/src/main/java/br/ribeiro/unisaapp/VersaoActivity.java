package br.ribeiro.unisaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class VersaoActivity extends AppCompatActivity {

    private TextView textViewVersaoAtual,textViewContato3;
    private Button buttonAtualizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_versao);

        textViewVersaoAtual = findViewById(R.id.textViewVersaoAtual);
        buttonAtualizar = findViewById(R.id.buttonAtualizar);
        textViewContato3 = findViewById(R.id.textViewContato3);

        String atual = getIntent().getStringExtra("br.ribeiro.unisaapp.VersaoAtual");
        String nova = getIntent().getStringExtra("br.ribeiro.unisaapp.VersaoNova");

        textViewVersaoAtual.setText("VERSÃO ATUAL: " + atual + " VERSÃO NOVA: " + nova);

        textViewContato3.setOnClickListener(v->{
            Intent i = new Intent(this,ContatoActivity.class);
            startActivity(i);
            finish();
        });

        buttonAtualizar.setOnClickListener(v->{
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://drive.google.com/drive/u/0/folders/1vw0X9xtj1OoelNvn0jDofltiB-rVLQdH"));
            startActivity(browserIntent);
            finish();
        });

    }
}