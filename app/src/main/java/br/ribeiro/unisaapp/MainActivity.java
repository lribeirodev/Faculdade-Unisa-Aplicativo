package br.ribeiro.unisaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.CertificateUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private static final double version_app = 2.5;

    Button fazerLogin;
    TextInputEditText login, password;
    TextView textViewContato, textViewVersao;

    // Criando cliente para solicitações HTTP
    OkHttpClient client;

    Toast toast;
    TextView text_toast;
    View layout_toast;
    String data[] = {null};

    SharedPreferences.Editor editor;

    List<Certificate> cert_aceitos;
    ClearableCookieJar cookieJar;
    CookieCache cookieCache;
    SharedPrefsCookiePersistor prefCookie;

    // GOOGLE FIREBASE SERVICES
    FirebaseAnalytics analytics;
    FirebaseUser userAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editor = getSharedPreferences("sessao_arquivada",MODE_PRIVATE).edit();

        // Injeção de dependencia
        fazerLogin = (Button) findViewById(R.id.buttonLogin);
        login = (TextInputEditText)  findViewById(R.id.login);
        password = (TextInputEditText) findViewById(R.id.password);
        textViewContato = findViewById(R.id.textViewContato);
        textViewVersao = findViewById(R.id.textViewVersao);

        // ATIVAR ANALITYCS GOOGLE
        analytics = FirebaseAnalytics.getInstance(this);

        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    userAuth = task.getResult().getUser();
                }
            }
        });

        textViewVersao.setText("Versão: " + version_app);

        // Toast View Personalizado
        LayoutInflater inflater = getLayoutInflater();
        layout_toast = inflater.inflate(R.layout.toast,(ViewGroup) findViewById(R.id.toast_layout_root));
        text_toast = (TextView) layout_toast.findViewById(R.id.text_layout);
        layout_toast.setBackgroundColor(Color.parseColor("#FFFF0003"));

        // Toast Objeto
        toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL,0,0);
        toast.setView(layout_toast);

        int t_pad = 10;
        text_toast.setPadding(t_pad,t_pad,t_pad,t_pad);


        OkHttpClient client_check_version = new OkHttpClient.Builder()
                .build();

        Request check_request = new Request.Builder()
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")
                .url("https://backendunisaapp.blogspot.com/p/checarversao.html")
                .build();

        client_check_version.newCall(check_request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody body = response.body();

                if(!response.isSuccessful()){
                    throw new IOException("ERRO CHECAR VERSÃO");
                }

                String bodyStr = body.string();

                int version_index = bodyStr.indexOf("<version>");
                int version_end = bodyStr.indexOf("</version>");
                String version = bodyStr.substring(version_index,version_end).replace("<version>","");
                Double version_check = Double.valueOf(version);

                if(version_app < version_check){
                    Intent i = new Intent(getApplicationContext(),VersaoActivity.class);
                    i.putExtra("br.ribeiro.unisaapp.VersaoAtual",String.valueOf(version_app));
                    i.putExtra("br.ribeiro.unisaapp.VersaoNova",String.valueOf(version_check));
                    startActivity(i);
                    finish();
                } else {

                }

                body.close();
            }
        });

        textViewContato.setOnClickListener(v->{
            Intent i = new Intent(this,ContatoActivity.class);
            startActivity(i);
            finish();
        });

        fazerLogin.setEnabled(false);
        fazerLogin.setText("CARREGANDO PLATAFORMA");

        // CRIANDO ARQUIVO CERTIFICADO
        String lines = "Bag Attributes: <Empty Attributes>\n" +
                "subject=C = BR, ST = Sao Paulo, L = Sao Paulo, O = Obras Sociais e Educacionais de Luz, CN = w3.unisa.br\n" +
                "\n" +
                "issuer=C = US, O = DigiCert Inc, CN = DigiCert SHA2 Secure Server CA\n" +
                "\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIGpjCCBY6gAwIBAgIQCRfumaJHRMW5NRwrR8crITANBgkqhkiG9w0BAQsFADBN\n" +
                "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMScwJQYDVQQDEx5E\n" +
                "aWdpQ2VydCBTSEEyIFNlY3VyZSBTZXJ2ZXIgQ0EwHhcNMjAwNjE1MDAwMDAwWhcN\n" +
                "MjIwNjIwMTIwMDAwWjB5MQswCQYDVQQGEwJCUjESMBAGA1UECBMJU2FvIFBhdWxv\n" +
                "MRIwEAYDVQQHEwlTYW8gUGF1bG8xLDAqBgNVBAoTI09icmFzIFNvY2lhaXMgZSBF\n" +
                "ZHVjYWNpb25haXMgZGUgTHV6MRQwEgYDVQQDEwt3My51bmlzYS5icjCCASIwDQYJ\n" +
                "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAPMhPaBjW6CH5SkD8V1shs0PMnhD4QUj\n" +
                "9zyVuIYA6/kVXZgw3UEJNM/+PKfcn+LYdzbB7VTombkyYz1myz/Ubv+HFFE9En3n\n" +
                "mCnH2y8imt8rARmtyQGhPbVi3kBGelAm+hlx5TqQgVtRHvVklaqVXQWypjrVVC1z\n" +
                "6c0t/S8FcI6lGpgLkSpGEZqkixOzc3+zNbfOvohjvrN1ORpKjZLwMgncsaEvHyUX\n" +
                "9sf/sFGNw+5KlqPIrfNMNmXQ/Hw/hixAqc2eIhUFPkY2DUNfqfNS31ZLTnXt9fHY\n" +
                "B7YBD+z/H63t60gLWKMIHmPYcvIavda3mIWoZHHc4IHzZ0IRzXoE3JsCAwEAAaOC\n" +
                "A1QwggNQMB8GA1UdIwQYMBaAFA+AYRyCMWHVLyjnjUY4tCzhxtniMB0GA1UdDgQW\n" +
                "BBTcBEi5PU+ira/xMRMnV19GqwkfkDAWBgNVHREEDzANggt3My51bmlzYS5icjAO\n" +
                "BgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMGsG\n" +
                "A1UdHwRkMGIwL6AtoCuGKWh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9zc2NhLXNo\n" +
                "YTItZzYuY3JsMC+gLaArhilodHRwOi8vY3JsNC5kaWdpY2VydC5jb20vc3NjYS1z\n" +
                "aGEyLWc2LmNybDBMBgNVHSAERTBDMDcGCWCGSAGG/WwBATAqMCgGCCsGAQUFBwIB\n" +
                "FhxodHRwczovL3d3dy5kaWdpY2VydC5jb20vQ1BTMAgGBmeBDAECAjB8BggrBgEF\n" +
                "BQcBAQRwMG4wJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmRpZ2ljZXJ0LmNvbTBG\n" +
                "BggrBgEFBQcwAoY6aHR0cDovL2NhY2VydHMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0\n" +
                "U0hBMlNlY3VyZVNlcnZlckNBLmNydDAMBgNVHRMBAf8EAjAAMIIBfgYKKwYBBAHW\n" +
                "eQIEAgSCAW4EggFqAWgAdgApeb7wnjk5IfBWc59jpXflvld9nGAK+PlNXSZcJV3H\n" +
                "hAAAAXK5RUUSAAAEAwBHMEUCIQDvOp4bfQUlRc9DZmHo22277w4ReZGS5W2bt5rf\n" +
                "MC6oPgIgPCegERyW7p6n0ls95Ox0MpyvZKkB08sg4UbyV+9zb1cAdQAiRUUHWVUk\n" +
                "VpY/oS/x922G4CMmY63AS39dxoNcbuIPAgAAAXK5RUUzAAAEAwBGMEQCIEI5yqCv\n" +
                "nURnPq0rY+FjWxUYFvQN6XZ688PPHrVWMUOqAiBHoXTguFRH8FQQU8gF0Ulyq0AJ\n" +
                "xz/GmXnujLijzmDZdQB3AFGjsPX9AXmcVm24N3iPDKR6zBsny/eeiEKaDf7UiwXl\n" +
                "AAABcrlFRYEAAAQDAEgwRgIhAPdNi5SSTjdccTGoR3gDDQovHLcgM3SUwW39pqnU\n" +
                "3jTmAiEA5jyqnodC+CTn7F+VD1BReYOabBgqnQvX/ZMH5jGyQKQwDQYJKoZIhvcN\n" +
                "AQELBQADggEBAJDsP905yWwIxSYyVCKwocUnabA/h5rbofwtCpHz/JaVvX2RzmO+\n" +
                "2K+t87ZbMiSGbtKINqMyCveZ5/HEGvJ/LcdL7a2iCub8dI8f2OZjBxn+JRF2fd6H\n" +
                "8TmSqp1qDn8WfR4X8xTEYSxyPGpslUxLKm+Bk1aDpI10quXuYDoXssLqaq2nYVIh\n" +
                "egn0BOYQDveVNbt3hDbWxqWClWmAAGLL7VBZxFQ3XrAeY03KISeSDz7avIYQdbkT\n" +
                "qPEzorya29Rb+781VNuAIOoG9KfXCpwwoqCxHzpDCo/mzp3Trs19GCWqVLNfA9hG\n" +
                "vZVC1comEhMr+c9u/FTdYpth9KorGcV3buM=\n" +
                "-----END CERTIFICATE-----\n" +
                "Bag Attributes: <Empty Attributes>\n" +
                "subject=C = US, O = DigiCert Inc, CN = DigiCert SHA2 Secure Server CA\n" +
                "\n" +
                "issuer=C = US, O = DigiCert Inc, OU = www.digicert.com, CN = DigiCert Global Root CA\n" +
                "\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIElDCCA3ygAwIBAgIQAf2j627KdciIQ4tyS8+8kTANBgkqhkiG9w0BAQsFADBh\n" +
                "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
                "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n" +
                "QTAeFw0xMzAzMDgxMjAwMDBaFw0yMzAzMDgxMjAwMDBaME0xCzAJBgNVBAYTAlVT\n" +
                "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxJzAlBgNVBAMTHkRpZ2lDZXJ0IFNIQTIg\n" +
                "U2VjdXJlIFNlcnZlciBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "ANyuWJBNwcQwFZA1W248ghX1LFy949v/cUP6ZCWA1O4Yok3wZtAKc24RmDYXZK83\n" +
                "nf36QYSvx6+M/hpzTc8zl5CilodTgyu5pnVILR1WN3vaMTIa16yrBvSqXUu3R0bd\n" +
                "KpPDkC55gIDvEwRqFDu1m5K+wgdlTvza/P96rtxcflUxDOg5B6TXvi/TC2rSsd9f\n" +
                "/ld0Uzs1gN2ujkSYs58O09rg1/RrKatEp0tYhG2SS4HD2nOLEpdIkARFdRrdNzGX\n" +
                "kujNVA075ME/OV4uuPNcfhCOhkEAjUVmR7ChZc6gqikJTvOX6+guqw9ypzAO+sf0\n" +
                "/RR3w6RbKFfCs/mC/bdFWJsCAwEAAaOCAVowggFWMBIGA1UdEwEB/wQIMAYBAf8C\n" +
                "AQAwDgYDVR0PAQH/BAQDAgGGMDQGCCsGAQUFBwEBBCgwJjAkBggrBgEFBQcwAYYY\n" +
                "aHR0cDovL29jc3AuZGlnaWNlcnQuY29tMHsGA1UdHwR0MHIwN6A1oDOGMWh0dHA6\n" +
                "Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEdsb2JhbFJvb3RDQS5jcmwwN6A1\n" +
                "oDOGMWh0dHA6Ly9jcmw0LmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEdsb2JhbFJvb3RD\n" +
                "QS5jcmwwPQYDVR0gBDYwNDAyBgRVHSAAMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8v\n" +
                "d3d3LmRpZ2ljZXJ0LmNvbS9DUFMwHQYDVR0OBBYEFA+AYRyCMWHVLyjnjUY4tCzh\n" +
                "xtniMB8GA1UdIwQYMBaAFAPeUDVW0Uy7ZvCj4hsbw5eyPdFVMA0GCSqGSIb3DQEB\n" +
                "CwUAA4IBAQAjPt9L0jFCpbZ+QlwaRMxp0Wi0XUvgBCFsS+JtzLHgl4+mUwnNqipl\n" +
                "5TlPHoOlblyYoiQm5vuh7ZPHLgLGTUq/sELfeNqzqPlt/yGFUzZgTHbO7Djc1lGA\n" +
                "8MXW5dRNJ2Srm8c+cftIl7gzbckTB+6WohsYFfZcTEDts8Ls/3HB40f/1LkAtDdC\n" +
                "2iDJ6m6K7hQGrn2iWZiIqBtvLfTyyRRfJs8sjX7tN8Cp1Tm5gr8ZDOo0rwAhaPit\n" +
                "c+LJMto4JQtV05od8GiG7S5BNO98pVAdvzr508EIDObtHopYJeS4d60tbvVS3bR0\n" +
                "j6tJLp07kzQoH3jOlOrHvdPJbRzeXDLz\n" +
                "-----END CERTIFICATE-----\n" +
                "Bag Attributes: <Empty Attributes>\n" +
                "subject=C = US, O = DigiCert Inc, OU = www.digicert.com, CN = DigiCert Global Root CA\n" +
                "\n" +
                "issuer=C = US, O = DigiCert Inc, OU = www.digicert.com, CN = DigiCert Global Root CA\n" +
                "\n" +
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh\n" +
                "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
                "d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD\n" +
                "QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT\n" +
                "MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j\n" +
                "b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG\n" +
                "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB\n" +
                "CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97\n" +
                "nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt\n" +
                "43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P\n" +
                "T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4\n" +
                "gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO\n" +
                "BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR\n" +
                "TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw\n" +
                "DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr\n" +
                "hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg\n" +
                "06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF\n" +
                "PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls\n" +
                "YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk\n" +
                "CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=\n" +
                "-----END CERTIFICATE-----\n";

        cert_aceitos = CertificateUtils.parsePemCertificate(lines);

        SSLFactory ssl = SSLFactory.builder()
                .withUnsafeTrustMaterial()
                .withTrustMaterial(cert_aceitos)
                .build();

        SSLSocketFactory so = ssl.getSslSocketFactory();
        X509ExtendedTrustManager trust = ssl.getTrustManager().get();

        // CRIANDO SERVIÇO HTTP
        cookieCache = new SetCookieCache();
        prefCookie = new SharedPrefsCookiePersistor(getApplicationContext());

        cookieJar =
                new PersistentCookieJar(cookieCache,prefCookie);
        client = new OkHttpClient.Builder()
                .addInterceptor(new LoggingOK())
                .sslSocketFactory(so,trust)
                .cookieJar(cookieJar)
                .build();

        primeiro_acesso_portal();

        fazerLogin.setOnClickListener(e -> {

            String login_str = login.getText().toString();
            String pass_str = password.getText().toString();

            if(login_str.length() > 0 && pass_str.length() > 0) {

                try {

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            fazerLogin.setText(" ACESSANDO... ");
                            fazerLogin.setBackgroundColor(Color.parseColor("#FFFFDE1E"));
                            fazerLogin.setEnabled(false);

                            ExecutorService service = Executors.newFixedThreadPool(1);
                            service.execute(() -> segundo_acesso_portal());
                        }
                    });

                } catch (Exception e1){
                    e1.printStackTrace();
                }


            } else {

            text_toast.setText("CAMPO INVÁLIDO");
            toast.show();

        }

        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = getSharedPreferences("sessao_arquivada",MODE_PRIVATE);

        String login_backup = pref.getString("login","sem_login");
        String pass_backup = pref.getString("pass","sem_pass");

        login = (TextInputEditText)  findViewById(R.id.login);
        password = (TextInputEditText) findViewById(R.id.password);

        if(!login_backup.contains("sem_login")){
            login.setText(login_backup);
            password.setText(pass_backup);
        }
    }

    private void primeiro_acesso_portal(){

        Request request = new Request.Builder()
                .url("https://portalaluno.unisa.br/login")
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try{
                    ResponseBody bodyToken = response.body();

                    if(!response.isSuccessful()){
                        throw new IOException("ERRO ETAPA 1> " + response);
                    }

                    String token = bodyToken.string().substring(1300,1390).trim();
                    token = token.substring(41,token.length()-2);
                    data[0] = token;

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fazerLogin.setEnabled(true);
                            fazerLogin.setText("FAZER LOGIN");
                        }
                    });

                    response.close();

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void segundo_acesso_portal(){

        JsonObject postData = new JsonObject();
        postData.addProperty("_token", data[0]);
        postData.addProperty("identity",login.getText().toString());
        postData.addProperty("password",password.getText().toString());

        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody postBody = RequestBody.create(JSON,postData.toString());
        Request request_portal = new Request.Builder()
                .url("https://portalaluno.unisa.br/login")
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")
                .post(postBody)
                .build();

        String[][] data_portal = new String[4][2];

        client.newCall(request_portal).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if(!response.isSuccessful()){
                    throw new IOException("ERRO ETAPA 2> " + response);
                }

                String body = responseBody.string();

                boolean vefCred = body.contains("Essas credenciais não foram encontradas em nossos registros.");
                if(vefCred){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            primeiro_acesso_portal();

                            fazerLogin.setText("Tentar Novamente");
                            fazerLogin.setBackgroundColor(Color.parseColor("#FF0068A0"));
                            fazerLogin.setEnabled(true);

                            text_toast.setText(" Essas credenciais não foram encontradas em nossos registros. ");
                            toast.show();

                        }
                    });
                }else{

                    editor.putString("login",login.getText().toString());
                    editor.putString("pass",password.getText().toString());
                    editor.commit();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            text_toast.setText(" ACESSO LIBERADO ");
                            layout_toast.setBackgroundColor(Color.parseColor("#FF61BD8C"));
                            toast.show();

                            login.setEnabled(false);
                            password.setEnabled(false);
                            fazerLogin.setVisibility(View.INVISIBLE);
                        }
                    });



                    // Web Scrap Filtrando os cookies
                    int cookie_inicio = body.indexOf("let url = \"");
                    int cookie_fim = body.indexOf("window.location.replace(url);");
                    String scrap_cookie = body.substring(cookie_inicio,cookie_fim);

                    // URL DO PORTAL
                    int portal_inicio = scrap_cookie.indexOf("\"");
                    int portal_fim = scrap_cookie.indexOf("\";");
                    data_portal[0][0] = "url_portal";
                    data_portal[0][1] = scrap_cookie.substring(portal_inicio,portal_fim).replace("\"","");

                    // SID DO ALUNO
                    int sid_inicio = scrap_cookie.indexOf("SID=");
                    int sid_fim = scrap_cookie.indexOf("; path=/; domain=unisa.br; samesite=lax\";");
                    data_portal[1][0] = "SID";
                    data_portal[1][1] = scrap_cookie.substring(sid_inicio,sid_fim).replace("SID=","");

                    // NOME DO ALUNO
                    String nome_line = scrap_cookie.substring(
                        scrap_cookie.indexOf("nome=")
                    );
                    int nome_inicio = nome_line.indexOf("nome=");
                    int nome_fim = nome_line.indexOf(";");
                    data_portal[2][0] = "nome";
                    data_portal[2][1] = nome_line.substring(nome_inicio,nome_fim).replace("nome=","");

                    // LOGIN ALUNO
                    String login_line = scrap_cookie.substring(
                        scrap_cookie.indexOf("login="));
                    int login_inicio = login_line.indexOf("login=");
                    int login_fim = login_line.indexOf(";");
                    data_portal[3][0] = "login";
                    data_portal[3][1] = login_line.substring(login_inicio,login_fim).replace("login=","");

                    ExecutorService service = Executors.newFixedThreadPool(1);
                    service.execute(() -> terceiro_acesso_portal(data_portal));
                }

            }
        });
    }

    private void terceiro_acesso_portal(String[][] data_portal){

        JsonObject postData = new JsonObject();
        List<Cookie> lista_cookies = new ArrayList<>();

        for(String[] data : data_portal){
            String name = data[0];
            String value = data[1];
            lista_cookies.add(new Cookie.Builder()
                .name(name)
                .value(value)
                .domain("unisa.br")
                .path("/")
                .build()
            );
        }

        lista_cookies.remove(0);

        cookieCache.clear();
        cookieCache.addAll(lista_cookies);

        Request request_portal = new Request.Builder()
                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")
                .url(data_portal[0][1])
                .build();

        client.newCall(request_portal).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if(!response.isSuccessful()){
                    throw  new IOException("ERRO ETAPA 3 > " + response);
                }

                ExecutorService service = Executors.newFixedThreadPool(1);
                service.execute(() -> {
                    try {
                        quarto_acesso_portal(responseBody.string());
                        response.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

     }

private void quarto_acesso_portal(String body){

    Request request = new Request.Builder()
            .url("https://digital.unisa.br/login/index.php?testsession=92744")
            .build();

    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {

        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent acesso = new Intent(getApplicationContext(),AcessoActivity.class);
                    acesso.putExtra("body",body);
                    acesso.putExtra("br.ribeiro.unisaapp.moodle",response.header("set-cookie"));

                    ClientHTTP.getInstance().setCookieCache(cookieCache);
                    ClientHTTP.getInstance().setCookieJar(cookieJar);
                    ClientHTTP.getInstance().setPrefCookie(prefCookie);
                    ClientHTTP.getInstance().setClient(client);

                    startActivity(acesso);
                    finish();

                }
            });
        }
    });

}

}