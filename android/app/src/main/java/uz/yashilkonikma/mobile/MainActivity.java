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
        if (!android.animation.ValueAnimator.areAnimatorsEnabled()) return;
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        LaunchView splash = new LaunchView(this);
        addContentView(splash, new ViewGroup.LayoutParams(-1, -1));
        splash.start();
    }

    /** Falling leaf transforms into the pixels of a two-line environmental message. */
    private class LaunchView extends View {
        private final android.graphics.Paint paint = new android.graphics.Paint(3);
        private final java.util.ArrayList<float[]> dots = new java.util.ArrayList<>();
        private final android.graphics.Path leaf = new android.graphics.Path();
        private android.animation.ValueAnimator animator;
        private float phase;
        private float textSize;
        private final String[] words = {"Har bir yashil qadam —", "kelajak uchun"};
        LaunchView(Context context) {
            super(context);
            setClickable(true);
            setContentDescription("Barg tushib, Har bir yashil qadam — kelajak uchun yozuviga aylanadi");
            setOnClickListener(v -> dismiss());
            leaf.moveTo(-42, 36);
            leaf.cubicTo(-65, -18, -10, -55, 48, -42);
            leaf.cubicTo(52, 14, 8, 58, -42, 36);
            leaf.close();
        }
        void start() {
            animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(3400);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(a -> { phase = (float)a.getAnimatedValue(); invalidate(); });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) { dismiss(); }
            });
            animator.start();
        }
        private void dismiss() {
            if (animator != null) { animator.removeAllListeners(); animator.cancel(); }
            if (getParent() instanceof ViewGroup) ((ViewGroup)getParent()).removeView(this);
        }
        @Override protected void onDetachedFromWindow() {
            if (animator != null) { animator.removeAllListeners(); animator.cancel(); }
            super.onDetachedFromWindow();
        }
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
            dots.clear();
            textSize = Math.min(dp(25), w / 12.8f);
            android.graphics.Bitmap mask = android.graphics.Bitmap.createBitmap(w, Math.max(1,dp(100)),android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(mask);
            paint.reset(); paint.setAntiAlias(true); paint.setColor(android.graphics.Color.WHITE);
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
            paint.setTextSize(textSize); paint.setTextAlign(android.graphics.Paint.Align.CENTER);
            c.drawText(words[0],w/2f,dp(36),paint); c.drawText(words[1],w/2f,dp(70),paint);
            int step = Math.max(3,dp(3));
            for(int y=0;y<mask.getHeight();y+=step) for(int x=0;x<w;x+=step)
                if(android.graphics.Color.alpha(mask.getPixel(x,y))>100) dots.add(new float[]{x,y});
            mask.recycle();
        }
        private float clamp(float x) { return Math.max(0f,Math.min(1f,x)); }
        private float smooth(float x) { x=clamp(x);return x*x*(3-2*x); }
        @Override protected void onDraw(android.graphics.Canvas c) {
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*0.43f;
            if(w<=0 || h<=0) return;
            setAlpha(1f-smooth((phase-0.92f)/0.08f));
            paint.setShader(new android.graphics.LinearGradient(0,0,w,h,0xfff3fcf2,0xffd9f4e5,android.graphics.Shader.TileMode.CLAMP));
            paint.setAlpha(255);c.drawRect(0,0,w,h,paint);paint.setShader(null);
            paint.setColor(0x186bbf81);c.drawCircle(w*0.9f,h*0.1f,dp(150),paint);
            c.drawCircle(w*0.05f,h*0.88f,dp(180),paint);
            float fall=clamp(phase/0.42f), morph=smooth((phase-0.42f)/0.30f);
            float lx=cx+(float)Math.sin(fall*Math.PI*3)*dp(45)*(1-fall);
            float ly=-dp(90)+(cy+dp(90))*smooth(fall);
            if(phase<0.51f) {
                c.save();c.translate(lx,ly);c.rotate((float)Math.sin(fall*Math.PI*3)*24-20*(1-fall));
                float scale=dp(1); c.scale(scale,scale);
                paint.setColor(0xff13875a);paint.setAlpha((int)(255*(1-smooth((phase-0.42f)/0.09f))));
                c.drawPath(leaf,paint);
                paint.setColor(0xff9ee2ad);paint.setAlpha((int)(240*(1-smooth((phase-0.42f)/0.09f))));
                paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(2);
                c.drawLine(-40,35,35,-30,paint);c.drawLine(-12,10,-29,-14,paint);c.drawLine(9,-9,31,0,paint);
                paint.setStyle(android.graphics.Paint.Style.FILL);c.restore();
            }
            float textTop=cy-dp(48),solid=smooth((phase-0.71f)/0.08f);
            if(phase>=0.42f && solid<1f) {
                paint.setColor(0xff13875a);paint.setAlpha((int)(255*(1-solid)));
                for(int i=0;i<dots.size();i++) {
                    float[] d=dots.get(i);double angle=i*2.399963;
                    float radius=(float)Math.sqrt((i+0.5)/Math.max(1,dots.size()))*dp(45);
                    float sx=cx+(float)Math.cos(angle)*radius,sy=cy+(float)Math.sin(angle)*radius*0.65f;
                    float arc=(float)Math.sin(morph*Math.PI)*dp(22);
                    c.drawCircle(sx+(d[0]-sx)*morph,sy+(textTop+d[1]-sy)*morph-arc,Math.max(1,dp(1)),paint);
                }
            }
            paint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));
            paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(textSize);
            paint.setColor(0xff075b40);paint.setAlpha((int)(255*solid));
            c.drawText(words[0],cx,textTop+dp(36),paint);c.drawText(words[1],cx,textTop+dp(70),paint);
            paint.setTextSize(dp(14));paint.setColor(0xff388b68);paint.setAlpha((int)(255*smooth((phase-0.75f)/0.1f)));
            c.drawText("YASHIL KO‘NIKMA",cx,cy+dp(76),paint);
            paint.setAlpha(255);
        }
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private void configure() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(24), dp(48), dp(24), dp(24));
        TextView title = new TextView(this); title.setText("🌿 Yashil Ko‘nikma"); title.setTextSize(28); box.addView(title);
        TextView info = new TextView(this); info.setText("Sayt manzilini bir marta kiriting. Keyingi safar ilova avtomatik ochiladi."); box.addView(info);
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
