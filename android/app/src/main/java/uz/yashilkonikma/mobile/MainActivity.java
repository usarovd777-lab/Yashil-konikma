package uz.yashilkonikma.mobile;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import android.print.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Same-origin online client: original React app and API stay on the existing server. */
public class MainActivity extends Activity {
    private WebView web;
    private LinearLayout root;
    private ProgressBar progress;
    private TextView status;
    private String site;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingBytes;
    private static final int PICK = 10, SAVE = 11;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        site = getPreferences(0).getString("site", "");
        if (site.isEmpty()) configure(); else openSite(state);
        if (state == null) showLaunchAnimation();
    }

    private void showLaunchAnimation() {
        android.widget.FrameLayout splash = new android.widget.FrameLayout(this);
        splash.setBackgroundColor(android.graphics.Color.rgb(240, 253, 244));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        ImageView leaf = new ImageView(this);
        leaf.setImageResource(uz.yashilkonikma.mobile.R.drawable.ic_leaf);
        content.addView(leaf, new LinearLayout.LayoutParams(dp(96), dp(96)));
        TextView name = new TextView(this);
        name.setText("Yashil Ko‘nikma");
        name.setTextSize(28);
        name.setTextColor(android.graphics.Color.rgb(6, 95, 70));
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(20), 0, dp(8));
        content.addView(name);
        TextView subtitle = new TextView(this);
        subtitle.setText("Yashil kelajak sari");
        subtitle.setTextSize(16);
        subtitle.setTextColor(android.graphics.Color.rgb(4, 120, 87));
        content.addView(subtitle);
        splash.addView(content, new android.widget.FrameLayout.LayoutParams(-1, -1));
        addContentView(splash, new android.view.ViewGroup.LayoutParams(-1, -1));
        boolean animate = android.animation.ValueAnimator.areAnimatorsEnabled();
        content.setAlpha(animate ? 0f : 1f);
        content.setScaleX(animate ? 0.88f : 1f);
        content.setScaleY(animate ? 0.88f : 1f);
        if (animate) content.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500)
            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
        splash.postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            splash.animate().alpha(0f).setDuration(animate ? 250 : 0).withEndAction(() -> {
                android.view.ViewParent parent = splash.getParent();
                if (parent instanceof android.view.ViewGroup) ((android.view.ViewGroup) parent).removeView(splash);
            }).start();
        }, animate ? 1100 : 0);
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private void configure() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(24), dp(48), dp(24), dp(24));
        TextView title = new TextView(this); title.setText("🌿 Yashil Ko‘nikma"); title.setTextSize(28); box.addView(title);
        TextView info = new TextView(this); info.setText("Mavjud saytingizning HTTPS manzilini kiriting. Ilova shu saytdagi hisob, o‘yin va ma’lumotlardan foydalanadi."); box.addView(info);
        EditText url = new EditText(this); url.setSingleLine(true); url.setInputType(17); url.setHint("https://saytingiz.uz"); url.setText(site); box.addView(url);
        Button go = new Button(this); go.setText("Ilovani ochish"); box.addView(go);
        go.setOnClickListener(v -> {
            String value = url.getText().toString().trim();
            Uri u = Uri.parse(value);
            if (!"https".equals(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null) { url.setError("To‘g‘ri HTTPS manzil kiriting"); return; }
            site = value; getPreferences(0).edit().putString("site", site).apply(); openSite(null);
        });
        setContentView(box);
    }
    private boolean trusted(String url) {
        if (url == null) return false;
        Uri a = Uri.parse(site), b = Uri.parse(url);
        return "https".equals(b.getScheme()) && a.getHost().equalsIgnoreCase(b.getHost()==null?"":b.getHost())
            && (a.getPort()==-1?443:a.getPort()) == (b.getPort()==-1?443:b.getPort());
    }
    private void openSite(Bundle state) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setOnApplyWindowInsetsListener((v,i) -> { v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom()); return i; });
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        status = new TextView(this); status.setPadding(dp(12),dp(8),dp(12),dp(8)); status.setVisibility(View.GONE); root.addView(status);
        web = new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if (!r.isForMainFrame()) return false;
                String target=r.getUrl().toString();
                if (trusted(target)) return false;
                if ("yashil-print".equals(r.getUrl().getScheme()) && trusted(v.getUrl())) { printPage(); return true; }
                if ("https".equals(r.getUrl().getScheme()) || "mailto".equals(r.getUrl().getScheme()) || "tel".equals(r.getUrl().getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,r.getUrl())); } catch(ActivityNotFoundException e) { message("Havolani ochuvchi ilova topilmadi"); }
                }
                return true;
            }
            @Override public void onPageFinished(WebView v,String url) {
                CookieManager.getInstance().flush();
                if (trusted(url)) v.evaluateJavascript("window.print=function(){location.href='yashil-print://page'};",null);
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e) {
                if(r.isForMainFrame()) { status.setText("Sahifa yuklanmadi. Internet va sayt manzilini tekshiring. Qayta urinish uchun shu yerga bosing."); status.setVisibility(View.VISIBLE); }
            }
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response) {
                if(r.isForMainFrame()) { status.setText("Server xatosi: "+response.getStatusCode()+". Qayta urinish uchun bosing."); status.setVisibility(View.VISIBLE); }
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v,int n) { progress.setProgress(n); progress.setVisibility(n==100?View.GONE:View.VISIBLE); }
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if(fileCallback!=null) fileCallback.onReceiveValue(null); fileCallback=callback;
                try { startActivityForResult(params.createIntent(),PICK); }
                catch(ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback=null; message("Fayl tanlash ilovasi topilmadi"); }
                return true;
            }
            @Override public boolean onJsAlert(WebView v,String url,String text,JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setMessage(text).setPositiveButton("OK",(d,w)->result.confirm()).setOnCancelListener(d->result.cancel()).show(); return true;
            }
            @Override public boolean onJsConfirm(WebView v,String url,String text,JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setMessage(text).setPositiveButton("Ha",(d,w)->result.confirm()).setNegativeButton("Yo‘q",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show(); return true;
            }
        });
        web.setDownloadListener((url,ua,disposition,mime,length)->download(url,disposition,mime));
        status.setOnClickListener(v->{status.setVisibility(View.GONE);web.loadUrl(site);});
        status.setOnLongClickListener(v->{web.destroy();web=null;configure();return true;});
        if(state==null || web.restoreState(state)==null) web.loadUrl(site);
    }
    private void message(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private void printPage() { if(web!=null) ((PrintManager)getSystemService(PRINT_SERVICE)).print("Yashil-Kon ikma".replace(" ",""),web.createPrintDocumentAdapter("Sertifikat"),new PrintAttributes.Builder().build()); }
    private void download(String url,String disposition,String mime) {
        if(pendingBytes!=null) {message("Avval oldingi faylni saqlang");return;}
        if(url.startsWith("data:")) { saveData(url); return; }
        if(url.startsWith("blob:") && trusted(web.getUrl())) {
            // Read blob in its original document, without exposing a native JS bridge.
            String key="__mobileDownload";
            web.evaluateJavascript("window."+key+"=null;fetch("+JSONObject.quote(url)+").then(r=>r.blob()).then(b=>{if(b.size>20000000)throw Error();let r=new FileReader();r.onload=()=>window."+key+"=r.result;r.readAsDataURL(b)}).catch(()=>window."+key+"='error');",v->pollBlob(0)); return;
        }
        if(!trusted(url)) {message("Yuklash uchun saytning o‘z havolasi kerak");return;}
        try {
            DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));
            String cookies=CookieManager.getInstance().getCookie(url); if(cookies!=null)r.addRequestHeader("Cookie",cookies);
            r.addRequestHeader("User-Agent",web.getSettings().getUserAgentString());
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalFilesDir(this,android.os.Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,disposition,mime));
            ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r); message("Yuklash boshlandi");
        } catch(Exception e) { message("Faylni yuklab bo‘lmadi"); }
    }
    private void pollBlob(int n) {
        if(web==null)return;
        web.evaluateJavascript("window.__mobileDownload",raw->{
            try {
                Object value=new JSONTokener(raw).nextValue();
                if(value instanceof String) { if("error".equals(value))message("Faylni o‘qib bo‘lmadi");else saveData((String)value); web.evaluateJavascript("delete window.__mobileDownload",null); }
                else if(n<50)web.postDelayed(()->pollBlob(n+1),100);
                else message("Faylni o‘qish vaqti tugadi");
            } catch(Exception e){message("Faylni o‘qib bo‘lmadi");}
        });
    }
    private void saveData(String data) {
        try {
            if(data.length()>28000000)throw new IOException();
            int comma=data.indexOf(','); String header=data.substring(5,comma);
            String mime=header.split(";")[0]; if(mime.isEmpty())mime="application/octet-stream";
            pendingBytes=header.contains(";base64")?android.util.Base64.decode(data.substring(comma+1),android.util.Base64.DEFAULT):Uri.decode(data.substring(comma+1)).getBytes(StandardCharsets.UTF_8);
            String ext=mime.contains("png")?"png":mime.contains("csv")?"csv":"bin";
            Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,"YashilKonikma-"+System.currentTimeMillis()+"."+ext);
            startActivityForResult(intent,SAVE);
        } catch(Exception e){pendingBytes=null;message("Faylni saqlash imkoni bo‘lmadi");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==PICK && fileCallback!=null) {fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));fileCallback=null;}
        if(request==SAVE) {
            if(result==RESULT_OK && data!=null && data.getData()!=null && pendingBytes!=null) {
                try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {if(out==null)throw new IOException();out.write(pendingBytes);message("Fayl saqlandi");}
                catch(Exception e){message("Saqlashda xatolik");}
            } pendingBytes=null;
        }
    }
    @Override public void onBackPressed() {if(web!=null && web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("Ilovadan chiqasizmi?").setPositiveButton("Chiqish",(d,w)->finish()).setNegativeButton("Qolish",null).show();}
    @Override protected void onSaveInstanceState(Bundle out) {super.onSaveInstanceState(out);if(web!=null)web.saveState(out);}
    @Override protected void onPause() {super.onPause();CookieManager.getInstance().flush();if(web!=null)web.onPause();}
    @Override protected void onResume() {super.onResume();if(web!=null)web.onResume();}
    @Override protected void onDestroy() {if(fileCallback!=null)fileCallback.onReceiveValue(null);if(web!=null)web.destroy();super.onDestroy();}
}
