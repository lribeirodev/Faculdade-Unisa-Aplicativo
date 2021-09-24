package br.ribeiro.unisaapp;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.cache.CookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import okhttp3.OkHttpClient;

public class ClientHTTP {

    public static ClientHTTP instance;
    private ClearableCookieJar cookieJar;
    private CookieCache cookieCache;
    private SharedPrefsCookiePersistor prefCookie;
    private OkHttpClient client;

    private ClientHTTP(){

    }

    public static ClientHTTP getInstance() {

        if(instance == null){
            instance = new ClientHTTP();
        }

        return instance;
    }

    public OkHttpClient getClient() {
        return client;
    }

    public ClearableCookieJar getCookieJar() {
        return cookieJar;
    }

    public CookieCache getCookieCache() {
        return cookieCache;
    }

    public SharedPrefsCookiePersistor getPrefCookie() {
        return prefCookie;
    }

    public void setCookieCache(CookieCache cookieCache) {
        this.cookieCache = cookieCache;
    }

    public void setCookieJar(ClearableCookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    public void setPrefCookie(SharedPrefsCookiePersistor prefCookie) {this.prefCookie = prefCookie;}

    public void setClient(OkHttpClient client) {
        this.client = client;
    }
}
