package br.ribeiro.unisaapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.renderscript.ScriptGroup;
import android.service.chooser.ChooserTarget;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AcessoActivity extends AppCompatActivity {

    TextView textTituloCurso;
    TextView textBemVindo;
    TextView textCarregando;
    WebView webDiciplinas;


    String titulo_texto;
    String nome_usuario;
    String pass_usuario;

    String moodleCookie = "";
    String cookie = "";
    String moddle_url = "";

    Toast toast;
    TextView text_toast;
    View layout_toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acesso);

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

        // Injenção de Depedencia
        textTituloCurso = (TextView) findViewById(R.id.textTituloCurso);
        textBemVindo = (TextView) findViewById(R.id.textBemVindo);
        textCarregando = (TextView) findViewById(R.id.textCarregando);

        // WebView
        webDiciplinas = (WebView) findViewById(R.id.webDiciplinas);
        webDiciplinas.getSettings().setJavaScriptEnabled(true);
        webDiciplinas.setVisibility(View.INVISIBLE);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webDiciplinas,true);

        // Recebe os dados do HTML da janela anterior
        Intent intent = getIntent();
        String body = intent.getStringExtra("body");

        // Cookie Moodle Session
        moodleCookie = intent.getStringExtra("br.ribeiro.unisaapp.moodle").replace("path=/","");

        // Pegar nome do Curso
        int titulo_inicio = body.indexOf("selected>");
        String titulo_line = body.substring(titulo_inicio);
        int titulo_final = titulo_line.indexOf("</option>");
        titulo_texto = titulo_line.substring(0,titulo_final).replace("selected>","");

        //Pegar seu Usuario
        String find_user_text = "username";
        int usuario_inicio = body.indexOf(find_user_text);
        String usuario_line = body.substring(usuario_inicio);
        int usuario_fim = usuario_line.indexOf(">");
        nome_usuario = usuario_line.substring(0,usuario_fim).replace("username\" type=\"hidden\" value=","");

        // Pegar Senha Interna Usuario
        String find_pass_text = "password";
        int pass_inicio = body.indexOf(find_pass_text);
        String pass_line = body.substring(pass_inicio);
        int pass_fim = pass_line.indexOf(">");
        pass_usuario = pass_line.substring(0,pass_fim).replace("password\" type=\"hidden\" value=","");

        // Pegar URL do moddle
        String find_moddle_url = "<form target=\"_blank\" action=\"";
        int moddle_inicio = body.indexOf(find_moddle_url);
        String moddle_line = body.substring(moddle_inicio);
        int moddle_fim = moddle_line.indexOf(">");
        moddle_url = moddle_line.substring(0,moddle_fim).replace("<form target=\"_blank\" action=\"","");
        moddle_url = moddle_url.replace("\" method=\"post\"","");

        // Iniciar Processo de Login no Moddle
        RequestBody requestBody = new FormBody.Builder()
                .add("username",nome_usuario)
                .add("password",pass_usuario)
                .add("Submit","Acesso+ao+Moodle")
                .build();

        Request request_moddle = new Request.Builder()
                .url(moddle_url)
                .post(requestBody)
                .build();

        ClientHTTP.getInstance().getClient().newCall(request_moddle).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();

                if(!response.isSuccessful()){
                    throw new IOException("ERRO ACESSO CURSO > " + response);
                }

                ExecutorService service = Executors.newFixedThreadPool(1);
                service.execute(() -> {
                    try {
                        primeiro_acesso_moddle(responseBody.string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK:
                    if(webDiciplinas.canGoBack()){
                        webDiciplinas.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }
        }


        return super.onKeyDown(keyCode, event);
    }

    private void primeiro_acesso_moddle(String body){

        // Pegar nome do aluno
        int nome_aluno_inicio = body.indexOf("alt=\"Imagem de");
        String nome_aluno_line = body.substring(nome_aluno_inicio);
        int nome_aluno_fim = nome_aluno_line.indexOf("\" title");
        String nome_aluno = nome_aluno_line.substring(0,nome_aluno_fim).replace("alt=\"Imagem de","");

        // Pegar nome das Materias
        int materias_inicio = body.indexOf("<div id=\"frontpage-course-list\">");
        int materias_fim = body.indexOf("<span class=\"skip-block-to\" id=\"skipmycourses\"></span>");
        String materias_lines = body.substring(materias_inicio,materias_fim)
        .replace("<span class=\"skip-block-to\" id=\"skipmycourses\"></span>","");

        // Alterar Layout da View
        AcessoActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Colocar nome do Aluno e Mensagem de Boas Vindas
                textBemVindo.setText("Olá " + nome_aluno + ", Seja Bem Vindo(a) ao Curso de");

                // Colocar nome do curso na tela
                textTituloCurso.setText(titulo_texto);

                webDiciplinas.loadUrl(moddle_url);

                // ADICIONA A OPÇÃO DE TELA CHEIA AOS VIDEOS DA VIMEO
                webDiciplinas.setWebChromeClient(new WebChromeClient(){

                    private View mCustomView;
                    private WebChromeClient.CustomViewCallback mCustomViewCallback;
                    private int mOriginalOrientation;
                    private int mOriginalSystemUiVisibility;

                    public Bitmap getDefaultVideoPoster()
                    {
                        if (mCustomView == null) {
                            return null;
                        }
                        return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
                    }

                    public void onHideCustomView()
                    {
                        ((FrameLayout)getWindow().getDecorView()).removeView(this.mCustomView);
                        this.mCustomView = null;
                        getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
                        setRequestedOrientation(this.mOriginalOrientation);
                        this.mCustomViewCallback.onCustomViewHidden();
                        this.mCustomViewCallback = null;
                    }

                    public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback)
                    {
                        if (this.mCustomView != null)
                        {
                            onHideCustomView();
                            return;
                        }
                        this.mCustomView = paramView;
                        this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                        this.mOriginalOrientation = getRequestedOrientation();
                        this.mCustomViewCallback = paramCustomViewCallback;
                        ((FrameLayout)getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
                        getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                    }

                });


                webDiciplinas.getSettings().setLoadWithOverviewMode(true);
                webDiciplinas.getSettings().setUseWideViewPort(true);
                webDiciplinas.getSettings().setBuiltInZoomControls(true);
                webDiciplinas.getSettings().setDisplayZoomControls(false);
                webDiciplinas.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36");

                webDiciplinas.setWebViewClient(new WebViewClient(){

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                        String url = request.getUrl().toString();
                        if(!url.contains("unisa")){
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(request.getUrl());
                            startActivity(i);
                            return true;

                        } else if(url.contains(".pdf")) {

                            text_toast.setText(" Aguarde Carregando PDF ");
                            int t_pad = 10;
                            text_toast.setPadding(t_pad,t_pad,t_pad,t_pad);
                            toast.show();

                            Request r = new Request.Builder()
                                    .url(url)
                                    .build();

                            ClientHTTP.getInstance().getClient().newCall(r).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                    e.printStackTrace();
                                }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

                                    ResponseBody responseBody = response.body();

                                    InputStream is = responseBody.byteStream();

                                    File dir = new File(getApplicationContext().getExternalFilesDir("").getAbsolutePath(),"UnisaPDF");

                                    if(!dir.exists()){
                                        dir.mkdirs();
                                    }

                                    File file = new File(dir,"Leitura.pdf");

                                    BufferedInputStream in = new BufferedInputStream(is);
                                    OutputStream out = new FileOutputStream(file);

                                    byte[] data = new byte[1024];

                                    long total = 0;
                                    int count = 0;

                                    while((count = in.read(data)) != -1){
                                        total += count;
                                        out.write(data,0,count);
                                    }

                                    out.flush();
                                    out.close();
                                    in.close();

                                    AcessoActivity.this.runOnUiThread(() -> {
                                        text_toast.setText(" Abrindo Arquivo PDF ");
                                        int t_pad = 10;
                                        text_toast.setPadding(t_pad,t_pad,t_pad,t_pad);
                                        layout_toast.setBackgroundColor(Color.parseColor("#FF61BD8C"));
                                        toast.show();
                                    });

                                    String autorizacao = AcessoActivity.this.getApplicationContext().getPackageName() + ".fileprovider";
                                    Uri pathUriArquivo = FileProvider.getUriForFile(AcessoActivity.this,autorizacao,file);

                                    Intent arquivoCompartilhar = new Intent(Intent.ACTION_VIEW);
                                    arquivoCompartilhar.setDataAndType(pathUriArquivo,"application/pdf");
                                    arquivoCompartilhar.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                    startActivity(Intent.createChooser(arquivoCompartilhar,"ABRIR PDF DA FACULDADE!"));

                                }

                            });

                            return true;
                        }

                        return false;
                    }



                    @Override
                    public void onPageFinished(WebView view, String url) {

                        // ADAPTAÇÃO BOTÃO PARA PLAYER DE HTML5
                        if(url.contains("mod_resource/content")){

//                            view.evaluateJavascript("javascript:(function(){setInterval(function(){document.getElementsByClassName('playbarBigButton')[0].click();},100)})()", new ValueCallback<String>() {
//                                @Override
//                                public void onReceiveValue(String value) {
//
//                                }
//                            });

                            view.evaluateJavascript("javascript:(function(){idInterval = setInterval(function(){check = document.getElementsByClassName('playbarBigButton'); if(check.length > 0){container = document.getElementsByClassName('cpMainContainer')[0]; button = document.createElement('button'); button.onclick = function(){document.getElementsByClassName('playbarBigButton')[0].click(); if(button.textContent.includes('PLAY')){button.textContent = 'PAUSAR';}else{button.textContent = 'PLAY';}}; button.textContent = 'PLAY'; button.style.backgroundColor = '#D1E8EC'; button.style.border = 'none'; button.style.padding = '15px 32px'; button.style.textDecoration = 'none'; button.style.fontSize = '16px'; button.style.fontWeight = 'bold'; container.appendChild(button); button.style.position = 'relative'; button.style.zIndex = 100; button.style.left = '0px'; button.style.top = '100px'; clearInterval(idInterval);}},1000)})()", new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String value) {

                                }
                            });

                        }

                        // FORMATA O VIDEO VIMEO PARA UM PADRÃO MAIS VISIVEL
                        if(url.contains("page/view.php"))   {

                            view.evaluateJavascript("javascript:" +
                                            "(function(){" +
                                            "i = document.getElementsByTagName('iframe');" +
                                            "if(i.length > 0) {" +
                                            //"i[0].src = i[0].src + '?playsinline=true'" +
                                            "i[0].width = '350px';" +
                                            "}" +
                                            "})()"
                                    , new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {

                                        }
                                    });

                        }

                        // FORMATAÇÃO DOS CURSOS
                        if(url.contains("course/view.php")) {
                            view.evaluateJavascript
                                    ("javascript:(function(){step_0 = document.getElementsByTagName('a'); Object.keys(step_0).reduce((index_1,index_2) => {step_0[index_2].target = '_self';});step_1 = document.getElementById('inst4'); step_1.style.display = 'none'; try{step_3 = document.getElementById('mudar0'); step_3.style.width = '300px'; step_3.style.height = '60px';} catch(step3_err){}; step_4 = document.getElementsByTagName('a'); Object.keys(step_4).reduce((i,i2) => {step_4[i2].target = '_self';});step_5 = document.getElementsByClassName('labelTrilhaPaginacao')[0]; step_5.className = step_5.className.replace('labelTrilhaPaginacao','');step_5.style.position = ''; step_5.style.bottom = 0; step_5.style.margin = '0px 0px 0px 80px';step_6 = document.getElementsByClassName('trilhaImgContainer')[0]; step_6.style.marginLeft = '10px'; step_6.style.width = '250px'; step_6.style.height = '800px'; step_6.style.background = 'linear-gradient(#D1E8EC, #E0EFF2)'; step_6.style.border = '3px solid #3898AA' ; b_array = []; n_array = [];Object.keys(step_6.children).reduce((index,step) => {step_6.children[step].nodeName = 'A' ? b_array.push(step_6.children[step]) : '';});Object.keys(b_array).reduce((index,step) => {try{b_array[step].children[0].nodeName='IMG'? n_array.push(b_array[step].children[0]) : ''} catch(err_js){}});b_array[0].style.left = '170px';b_array[0].style.zIndex = '1';function adaptar(data){ m = 1; for(let i = 0 ; i < data.length ; i = i + 1){ try{ data[i].style.margin = m+'px 0px 3px 70px'; data[i].className = data[i].classList[1]; data[i].style.width = '100px'; data[i].style.display = 'block'; data[i].style.top = '0px'; data[i].style.left = '0px'; data[i].style.right = '0px'; data[i].style.bottom = '0px'; m=m+1; } catch(err_js){console.log('ERRO RENDERIZAÇÃO >>>> ' + err_js)};};}; adaptar(n_array);})()", new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {

                                        }
                                    });
                        }

                        if(url.contains("login/index.php")) {
                            view.evaluateJavascript
                                    ("javascript:" +
                                            "(function(){" +
                                            "try{" +
                                            "if(!document.getElementsByTagName('p')[0].innerText.includes('Você já está autenticado')){" +
                                            "form = document.getElementsByTagName('input');" +
                                            "form[0].value = '" + nome_usuario + "'; form[1].value = '" + pass_usuario + "';" +
                                            "document.getElementById('loginbtn').click(); } else {document.location.assign('https://digital.unisa.br');" +
                                            "}} catch(err_js) {document.location.assign('https://digital.unisa.br');}" +
                                            "})()", new ValueCallback<String>() {              @Override
                                        public void onReceiveValue(String value) {

                                        }
                                    });
                        } else if(url.contentEquals("https://digital.unisa.br/")) {
                            view.evaluateJavascript
                                    ("javascript:" +
                                            "(function(){diciplinas = document.getElementById('frontpage-course-list');" +
                                            "document.body.innerHTML = diciplinas.innerHTML;" +
                                            "})()", new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {

                                            AcessoActivity.this.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    textCarregando.setVisibility(View.INVISIBLE);
                                                    webDiciplinas.setVisibility(View.VISIBLE);
                                                }
                                            });

                                        }
                                    });
                        }

                    }

                });
            }
        });


    }

    private String carregar_html(String data){
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"https://digital.unisa.br/theme/styles.php/essential/1622467836_1622350490/all\">\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                data+ "\n" +
                "\n" +
                "</body>\n" +
                "\n" +
                "</html>";
    }
}