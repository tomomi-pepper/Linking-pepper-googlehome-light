# Pepper door

[Sizuku 6X](https://ssl.braveridge.com/store/html/products/detail.php?product_id=33)を使って加速度を感知し、PepperがGoogle Home Miniに話しかけ電気を付けるプログラムです。


# Android

Android以下のソースコードは[Project Linking](https://linkingiot.com/)で配布されているSDKのサンプルアプリを一部加工したものです。

Z軸の加速度の変化から、ドアが開いたことを検知。それをサーバーに通知します。追加したメソッドは下記の通り。過去10回分のZ軸の加速度を保存して、最新のZ軸の加速度がその平均値の3倍以上のときにドアが開いたと判定します。

```
public void onSensorData(String bd, int type, float x, float y, float z,
                         byte[] originalData, long time) {

    this.bd = bd;
    this.type = type;
    this.x = x;
    this.y = y;
    this.z = z;
    this.originalData = originalData;
    this.time = time;

    if(isDoorOpened(z)){
        Log.d(TAG,"Door opened");

        try {
            request_say_ok_google();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    setPastZ(z);


    if(DBG) Log.d(TAG, "[" + bd + "] のデータ[type:" + type + "]を受信 " +  "x:" + x + ", y : " + y +", z : " + z);

    mHandler.postDelayed(listUpdateTask, 0);
}
```


isDoorOpened()でドアが開いたことを検知して、request_say_ok_google()を利用して、サーバー経由でPepperに「ねぇ Google ライトつけて」と言わせます。

```
public static final int MAX_IDX_PAST_Z = 10;
float[] past_z = new float[10];
int idx4past_z = 0;

private void setPastZ(float z){
    past_z[idx4past_z++] = z;
    if(idx4past_z >= MAX_IDX_PAST_Z) {
        idx4past_z = 0;
    }
}

private float getAvgPastZ(){
    float total = 0;

    for(int i = 0; i < MAX_IDX_PAST_Z; i++){
        total += abs(past_z[i]);
    }
    return (total / (float)MAX_IDX_PAST_Z);
}

private boolean isDoorOpened(float z){
    if(getAvgPastZ() * 3.0 <  z){
        return true;
    }
    return false;
}

private void request_say_ok_google() throws IOException {
    //URL url = new URL("http://192.168.0.20/say_ok_google.html");
    new Thread(new Runnable() {
        @Override
        public void run() {
            
            try {

                URL url = new URL("http://192.168.0.20/say_ok_google.html");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.connect();
                int res = con.getResponseCode();
                Log.d(TAG,"HTTP" + res);
            } catch(Exception ex) {
                System.out.println(ex);
            }

        }
    }).start();
}
```

[Linking開発者サイト](https://linkingiot.com/developer/index.html)から
```
sdaiflib.jar
```
を取得して
```
Android/PepperFlowerApp/app/libs/
```
に設置してください。

また、
<pre>Android/PepperFlowerApp/app/src/main/java/com/sample/nttdocomo/android/linkingpairingdemo/pairing/SensorDemoActivity.java</pre>
の205行目のURLのIPアドレスを各自の環境に合わせて変更してください。
```
URL url = new URL("http://192.168.0.20/say_ok_google.html");
```

# Server

AndroidアプリからWebViewとしてsay_ok_google.htmlをリクエストすることで、qimessaging経由でPepperにsayを要求します。

```
<!DOCTYPE html>
<html lang="ja">
  <head>
    <meta charset="utf-8">
    <title>Knockin on your door</title>

    <script src="libs/qimessaging/1.0/qimessaging.js"></script>
    <script>


    function init(){
      var session = new QiSession("192.168.0.15");

      session.socket().on('connect', function() {
        console.log("connected!");
      });

      session.service("ALTextToSpeech").done(function(tts) {
	    tts.say("\\vct=140\\\\rspd=110\\友ちゃん、お帰りぃ。。。。。。。。");
	    tts.say("\\vct=110\\\\rspd=140\\ねぇグーグル。ライトつけて");
      }).fail(function(error) {
        console.log("Error!");
      });
    }
	init();
    </script>
```

