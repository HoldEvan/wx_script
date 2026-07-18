// ==================== 配置区 ====================
// ✅ 音频保存目录
static final String AUDIO_DIR_NAME = "/tts_audio";
// 默认唤起面板命令
static final String DEFAULT_OPEN_COMMAND = "/TTS"
// 默认发送命令
static final String DEFAULT_SEND_COMMAND = "/tts"
// ================================================

// ====== 头像磁盘缓存 ======
static final String AVATAR_CACHE_DIR = "/storage/emulated/0/Android/data/com.tencent.mm/WeKit/tts_avatar";

// ====== 摸鱼日报 ======
static final String MOYU_API_URL = "https://apix.iqfk.top/api/moyuya";
static final String MOYU_FILE_PREFIX = "/myrb";

// ====== 历史今天 ======
static final String HISTORY_API_URL = "https://v2.xxapi.cn/api/historypic";
static final String HISTORY_FILE_PREFIX = "/history";

// ====== 小人举牌 ======
static final String SIGN_API_URL = "https://ffapi.cn/int/v1/xrjp?msg=";
static final String SIGN_FILE_PREFIX = "/sign";
static final String[] SIGN_PRESET_APP_IDS = {
    "wxe3ad19e142df87b3",
    "wx1ebb9c41ccbfb6d4",
    "wx6d8030a2f43b09a2",
    "wxb0eef1f67b7a2949",
    "wx115bcff956fd0905",
    "wx3f4266934f0e29fb",
    "wx315ce2808c20cb43",
    "wx322bb520817c18e7",
    "wx7395b7ea7ae1cab7",
    "wx281a70a3d390bdf2",
    "wxa0104328eeb70938",
    "wx92e3210df60c2e11",
    "wx274f9e94ca7302a1",
    "wxa43341eed288d77b",
    "wxefa60233f28c2955",
    "wxcdc5278445b04d39",
    "wx6321d27140be32de",
    "wx4fee0da9380b6608",
    "wx92398516de814096"
};
static final String DEFAULT_SIGN_COMMAND = "/举牌";

// ====== 合成表情 ======
static final String EMOJI_API_URL = "https://oiapi.net/api/EmojiMix?emoji1=";
static final String EMOJI_FILE_PREFIX = "/emoji";
static final String DEFAULT_EMOJI_COMMAND = "/合成";

// ====== TTS引擎 ======
static android.speech.tts.TextToSpeech[] ttsRef = new android.speech.tts.TextToSpeech[1];
static boolean ttsReady = false;

android.content.Context getAppContext() {
    try {
        var activity = getTopActivity();
        if (activity != null) return activity.getApplicationContext();
    } catch (Exception e) {}
    try {
        Class atClass = Class.forName("android.app.ActivityThread");
        Object at = atClass.getMethod("currentActivityThread").invoke(null);
        return (android.content.Context) atClass.getMethod("getApplication").invoke(at);
    } catch (Exception e) {
        log("[TTS] getAppContext 反射失败: " + e.toString());
    }
    return null;
}

void initTTS() {
    if (ttsRef[0] != null) return;
    try {
        var ctx = getAppContext();
        if (ctx == null) { log("[TTS] initTTS: ctx为null"); return; }
        ttsRef[0] = new android.speech.tts.TextToSpeech(ctx, null);
        ttsReady = true;
    } catch (Exception e) {
        log("[TTS] initTTS error: " + e.toString());
    }
}

void onUnload() {
    try {
        if (ttsRef[0] != null) {
            ttsRef[0].stop();
            ttsRef[0].shutdown();
            ttsRef[0] = null;
            ttsReady = false;
        }
    } catch (Exception e) {
        log("[TTS] onUnload error: " + e.toString());
    }
    log("[TTS] plugin unloaded");
}

void doSpeak(String text) {
    if (text == null || text.trim().isEmpty()) return;
    try {
        if (ttsRef[0] == null) initTTS();
        if (!ttsReady || ttsRef[0] == null) {
            new Thread(new Runnable() {
                public void run() {
                    try { Thread.sleep(500); } catch (Exception e) {}
                    if (ttsRef[0] == null) initTTS();
                    if (ttsRef[0] != null) {
                        try {
                            float speed = Float.parseFloat(getString("speed", "1.0"));
                            float pitch = Float.parseFloat(getString("pitch", "1.0"));
                            ttsRef[0].setSpeechRate(speed);
                            ttsRef[0].setPitch(pitch);
                            ttsRef[0].speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
                        } catch (Exception e) {
                            log("[TTS] 延迟朗读失败: " + e.toString());
                        }
                    }
                }
            }).start();
            return;
        }
        float speed = Float.parseFloat(getString("speed", "1.0"));
        float pitch = Float.parseFloat(getString("pitch", "1.0"));
        ttsRef[0].setSpeechRate(speed);
        ttsRef[0].setPitch(pitch);
        ttsRef[0].speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
        log("[TTS] 朗读: " + text.substring(0, Math.min(30, text.length())));
    } catch (Exception e) {
        log("[TTS] doSpeak error: " + e.toString());
    }
}

// ====== 文字转语音并发送 ======

// 裸PCM → MP3 编码
boolean encodePcmToMp3(String pcmPath, String mp3Path, int sampleRate, int channels) {
    java.io.FileInputStream fis = null;
    java.io.FileOutputStream fos = null;
    android.media.MediaCodec codec = null;
    try {
        log("[TTS] encodePcmToMp3: sampleRate=" + sampleRate + " channels=" + channels);
        
        java.io.File pcmFile = new java.io.File(pcmPath);
        long pcmSize = pcmFile.length();
        if (pcmSize == 0) {
            log("[TTS] encodePcmToMp3: PCM文件为空, 跳过");
            return false;
        }
        
        int bitRate = sampleRate * channels * 16;
        if (bitRate < 32000) bitRate = 32000;
        log("[TTS] pcmSize=" + pcmSize + " bitRate=" + bitRate);
        
        fis = new java.io.FileInputStream(pcmPath);
        codec = android.media.MediaCodec.createEncoderByType("audio/mpeg");
        android.media.MediaFormat format = android.media.MediaFormat.createAudioFormat(
            "audio/mpeg", sampleRate, channels);
        format.setInteger(android.media.MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE, 8192);
        codec.configure(format, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE);
        codec.start();
        
        fos = new java.io.FileOutputStream(mp3Path);
        android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
        byte[] pcmBuf = new byte[8192];
        int readLen;
        boolean inputDone = false;
        boolean eosReceived = false;
        int totalEncoded = 0;
        
        while (!eosReceived) {
            if (!inputDone) {
                int inIdx = codec.dequeueInputBuffer(10000);
                if (inIdx >= 0) {
                    java.nio.ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                    if (inBuf != null) {
                        inBuf.clear();
                        readLen = fis.read(pcmBuf);
                        if (readLen > 0) {
                            inBuf.put(pcmBuf, 0, readLen);
                            codec.queueInputBuffer(inIdx, 0, readLen, 0, 0);
                        } else {
                            codec.queueInputBuffer(inIdx, 0, 0, 0,
                                android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        }
                    }
                }
            }
            int outIdx = codec.dequeueOutputBuffer(info, 10000);
            if (outIdx >= 0) {
                java.nio.ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                if (outBuf != null) {
                    byte[] outData = new byte[info.size];
                    outBuf.get(outData);
                    outBuf.clear();
                    fos.write(outData);
                    totalEncoded += info.size;
                }
                codec.releaseOutputBuffer(outIdx, false);
                if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    eosReceived = true;
                }
            } else if (outIdx == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                log("[TTS] encodePcmToMp3: 输出格式变更");
            }
        }
        log("[TTS] PCM→MP3编码完成, totalEncoded=" + totalEncoded + " bytes");
        return true;
    } catch (Exception e) {
        log("[TTS] encodePcmToMp3 error: " + e.toString());
        return false;
    } finally {
        try { if (fis != null) fis.close(); } catch (Exception e) {}
        try { if (fos != null) fos.close(); } catch (Exception e) {}
        try { if (codec != null) { codec.stop(); codec.release(); } } catch (Exception e) {}
    }
}

// ====== PCM转WAV（添加44字节WAV头） ======
String pcmToWav(String pcmPath, int sampleRate, int channels, int bitsPerSample) {
    var fis = null;
    var fos = null;
    try {
        var pcmFile = new java.io.File(pcmPath);
        int dataSize = (int) pcmFile.length();
        if (dataSize == 0) return null;
        
        String wavPath = pcmPath.replace(".pcm", ".wav");
        fis = new java.io.FileInputStream(pcmPath);
        fos = new java.io.FileOutputStream(wavPath);
        
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        
        java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(44);
        header.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'R', 'I', 'F', 'F'});
        header.putInt(dataSize + 36);
        header.put(new byte[]{'W', 'A', 'V', 'E'});
        header.put(new byte[]{'f', 'm', 't', ' '});
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitsPerSample);
        header.put(new byte[]{'d', 'a', 't', 'a'});
        header.putInt(dataSize);
        
        fos.write(header.array());
        
        byte[] buf = new byte[4096];
        int len;
        while ((len = fis.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }
        
        log("[TTS] pcmToWav: " + dataSize + " bytes → " + wavPath);
        return wavPath;
    } catch (Exception e) {
        log("[TTS] pcmToWav error: " + e.toString());
        return null;
    } finally {
        try { if (fis != null) fis.close(); } catch (Exception e) {}
        try { if (fos != null) fos.close(); } catch (Exception e) {}
    }
}

void synthesizeAndSend(String text, String talker) {
    try {
        if (ttsRef[0] == null) initTTS();
        if (ttsRef[0] == null || !ttsReady) {
            try { Thread.sleep(1000); } catch (Exception e) {}
        }
        if (ttsRef[0] == null) {
            log("[TTS] synthesizeAndSend: TTS未就绪");
            return;
        }
        
        long ts = System.currentTimeMillis();
        String pcmPath = cacheDir + "/tts_temp_" + ts + ".pcm";
        String wavPath = cacheDir + "/tts_temp_" + ts + ".wav";
        String silkPath = cacheDir + "/tts_temp_" + ts + ".silk";
        
        try { ttsRef[0].setLanguage(java.util.Locale.CHINESE); } catch (Exception e) {}
        
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synth_" + ts);
        bundle.putString("sampleRateInHz", "16000");
        
        int sfR = ttsRef[0].synthesizeToFile(text, bundle, pcmPath);
        log("[TTS] synthesizeToFile result=" + sfR + " path=" + pcmPath);
        
        java.io.File pcmF = new java.io.File(pcmPath);
        if (pcmF.length() < 100) {
            log("[TTS] PCM文件无效(" + pcmF.length() + "字节)");
            return;
        }
        
        String wavResult = pcmToWav(pcmPath, 16000, 1, 16);
        if (wavResult == null) {
            log("[TTS] PCM→WAV转换失败");
            return;
        }
        
        Object convResult = wavToSilk(wavResult, silkPath);
        log("[TTS] wavToSilk result=" + convResult);
        boolean convOk;
        if (convResult instanceof Boolean) {
            convOk = (Boolean) convResult;
        } else if (convResult instanceof Number) {
            convOk = ((Number) convResult).intValue() == 0;
        } else {
            convOk = false;
        }
        
        String sendPath;
        if (convOk) {
            sendPath = silkPath;
            log("[TTS] WAV→Silk成功, 发送Silk文件");
        } else {
            sendPath = wavResult;
            log("[TTS] WAV→Silk失败, 回退发送WAV文件");
        }
        
        Object durObj = getDuration(sendPath);
        long durationMs = 0;
        if (durObj instanceof Number) {
            durationMs = ((Number) durObj).longValue();
        }
        int durationSec = (int) Math.ceil(durationMs / 1000.0);
        if (durationSec <= 0) durationSec = 1;
        sendVoice(talker, sendPath, durationSec);
        log("[TTS] 语音发送完成, duration=" + durationSec + "s, path=" + sendPath);
    } catch (Exception e) {
        log("[TTS] synthesizeAndSend error: " + e.toString());
    }
}

android.graphics.Bitmap loadCachedAvatar(String wxid) {
    String path = AVATAR_CACHE_DIR + "/" + wxid.replaceAll("[^a-zA-Z0-9_@]", "_") + ".jpg";
    return android.graphics.BitmapFactory.decodeFile(path);
}

void saveCachedAvatar(String wxid, android.graphics.Bitmap bm) {
    try {
        String path = AVATAR_CACHE_DIR + "/" + wxid.replaceAll("[^a-zA-Z0-9_@]", "_") + ".jpg";
        java.io.File dir = new java.io.File(AVATAR_CACHE_DIR);
        if (!dir.exists()) dir.mkdirs();
        java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
        bm.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos);
        fos.close();
    } catch (Exception e) {
        log("[TTS] 缓存保存失败: " + e.toString());
    }
}

// ====== 头像缓存 ======
final Map<String, android.graphics.Bitmap> avatarCache = new java.util.LinkedHashMap<String, android.graphics.Bitmap>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, android.graphics.Bitmap> eldest) {
        return size() > 80;
    }
};

android.graphics.Bitmap getCircularBitmap(android.graphics.Bitmap source) {
    int size = Math.min(source.getWidth(), source.getHeight());
    int x = (source.getWidth() - size) / 2;
    int y = (source.getHeight() - size) / 2;
    android.graphics.Bitmap squared = android.graphics.Bitmap.createBitmap(source, x, y, size, size);
    if (squared != source) source.recycle();
    android.graphics.Bitmap output = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
    android.graphics.Canvas canvas = new android.graphics.Canvas(output);
    android.graphics.Paint paint = new android.graphics.Paint();
    android.graphics.Rect rect = new android.graphics.Rect(0, 0, size, size);
    paint.setAntiAlias(true);
    canvas.drawARGB(0, 0, 0, 0);
    canvas.drawCircle(size / 2, size / 2, size / 2, paint);
    paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(squared, rect, rect, paint);
    squared.recycle();
    return output;
}

android.graphics.Bitmap getContactAvatar(Object contact) {
    try {
        java.lang.reflect.Method m = contact.getClass().getMethod("getAvatar");
        Object result = m.invoke(contact);
        if (result instanceof android.graphics.Bitmap) return (android.graphics.Bitmap) result;
    } catch (Exception e1) {
        try {
            java.lang.reflect.Method m = contact.getClass().getMethod("getAvatarBitmap");
            Object result = m.invoke(contact);
            if (result instanceof android.graphics.Bitmap) return (android.graphics.Bitmap) result;
        } catch (Exception e2) {}
    }
    return null;
}

android.graphics.Bitmap downloadAvatar(String wxid) {
    try {
        String avatarUrl = getAvatarUrl(wxid);
        log("[TTS] getAvatarUrl(" + wxid + ") = " + (avatarUrl != null ? avatarUrl.substring(0, Math.min(40, avatarUrl.length())) + "..." : "null"));
        if (avatarUrl == null || avatarUrl.isEmpty()) return null;
        java.net.URL url = new java.net.URL(avatarUrl);
        java.io.InputStream is = url.openStream();
        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(is);
        is.close();
        log("[TTS] downloadAvatar success: " + (bm != null));
        return bm;
    } catch (Exception e) {
        log("[TTS] downloadAvatar error: " + e.toString());
        return null;
    }
}

// ====== 判断当前是否为深色模式 ======
boolean isDarkMode(Context ctx) {
    int nightModeFlags = ctx.getResources().getConfiguration().uiMode 
        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
    return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
}

// ====== 打开主面板 (一级弹窗) - 插件分类入口 ======
void openMainPanel(){
    var ctx = getTopActivity()
    if (ctx == null) { toast("无法打开面板"); return; }
    boolean dark = isDarkMode(ctx);
    
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int versionColor = dark ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.parseColor("#95A5A6");
    int cardDividerColor = dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#E0E0E0");
    
    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentGreen = dark ? "#58D68D" : "#27AE60";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String accentPurple = dark ? "#BB8FCE" : "#8E44AD";
    String accentRed = dark ? "#EC7063" : "#E74C3C";
    String accentTeal = dark ? "#48C9B0" : "#1ABC9C";
    
    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenWidth = displayMetrics.widthPixels
    int screenHeight = displayMetrics.heightPixels
    
    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
    mainContainer.setBackgroundColor(bgColor)
    
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)
    
    var titleText = new TextView(ctx)
    titleText.setText("插件中心")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(20)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    titleLayout.addView(titleText)
    
    var versionText = new TextView(ctx)
    versionText.setText(pluginVersion)
    versionText.setTextColor(versionColor)
    versionText.setTextSize(12)
    versionText.setGravity(android.view.Gravity.RIGHT)
    versionText.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
    titleLayout.addView(versionText)
    
    mainContainer.addView(titleLayout)
    
    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)
    
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)
    
    // ====== 总开关 ======
    var masterRow = new LinearLayout(ctx)
    masterRow.setOrientation(LinearLayout.HORIZONTAL)
    masterRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    masterRow.setPadding(dpToPx(ctx, 12), dpToPx(ctx, 8), dpToPx(ctx, 12), dpToPx(ctx, 8))
    masterRow.setBackgroundColor(cardBgColor)
    var masterLP = new LinearLayout.LayoutParams(-1, -2)
    masterLP.setMargins(0, 0, 0, dpToPx(ctx, 8))
    masterRow.setLayoutParams(masterLP)
    
    var masterText = new TextView(ctx)
    masterText.setText("总开关")
    masterText.setTextColor(titleColor)
    masterText.setTextSize(18)
    masterText.setTypeface(null, android.graphics.Typeface.BOLD)
    masterText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f))
    masterRow.addView(masterText)
    
    var masterSw = new android.widget.Switch(ctx)
    boolean masterOn = getBoolean("masterEnabled", true)
    masterSw.setChecked(masterOn)
    masterSw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
            putBoolean("masterEnabled", isChecked)
            if (!isChecked) {
                putBoolean("sixtyEnabled", false)
                putBoolean("moyuEnabled", false)
                putBoolean("historyEnabled", false)
                putBoolean("emojiEnabled", false)
                putBoolean("signEnabled", false)
                putBoolean("ringtoneMsgEnabled", false)
                putBoolean("ringtoneCallEnabled", false)
                putBoolean("timedMsgEnabled", false)
                putBoolean("aiAutoReplyEnabled", false)
            }
        }
    })
    masterRow.addView(masterSw)
    mainLayout.addView(masterRow)
    
    // ====== 讯息类 ======
    var cardMsg = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardMsg.addView(createSection(ctx, "讯息类", titleColor, accentBlue));
    
    var msgSixty = createFeatureItem(ctx, "每日六十秒", "每天获取新闻简报", "sixtyEnabled", false, dark)
    cardMsg.addView(msgSixty)
    cardMsg.addView(createListDivider(ctx, cardDividerColor))
    var msgMoyu = createFeatureItem(ctx, "摸鱼日报", "每日摸鱼资讯", "moyuEnabled", false, dark)
    cardMsg.addView(msgMoyu)
    cardMsg.addView(createListDivider(ctx, cardDividerColor))
    var msgHistory = createFeatureItem(ctx, "历史今天", "历史上的今天", "historyEnabled", false, dark)
    cardMsg.addView(msgHistory)
    
    mainLayout.addView(cardMsg)
    
    // ====== 表情类 ======
    var cardEmoji = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardEmoji.addView(createSection(ctx, "表情类", titleColor, accentGreen));
    
    var emojiItem = createFeatureItem(ctx, "表情合成", "图片表情合成", "emojiEnabled", false, dark)
    cardEmoji.addView(emojiItem)
    cardEmoji.addView(createListDivider(ctx, cardDividerColor))
    var signItem = createFeatureItem(ctx, "小人举牌", "举牌文字生成", "signEnabled", false, dark)
    cardEmoji.addView(signItem)
    
    mainLayout.addView(cardEmoji)
    
    // ====== 语音类 ======
    var cardVoice = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardVoice.addView(createSection(ctx, "语音类", titleColor, accentPurple));
    
    var ttsItem = createFeatureItem(ctx, "TTS播报", "消息文字转语音朗读", "ttsEnabled", true, dark)
    cardVoice.addView(ttsItem)
    
    mainLayout.addView(cardVoice)
    
    // ====== AI类 ======
    var cardAi = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardAi.addView(createSection(ctx, "AI类", titleColor, accentOrange));
    
    var aiPlaceholder = createFeatureItem(ctx, "自动回复", "AI自动回复消息", "aiAutoReplyEnabled", false, dark)
    cardAi.addView(aiPlaceholder)
    
    mainLayout.addView(cardAi)
    
    // ====== 专属铃声 ======
    var cardRingtone = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardRingtone.addView(createSection(ctx, "专属铃声", titleColor, accentPurple));
    
    cardRingtone.addView(createFeatureItem(ctx, "消息铃声", "消息通知专属铃声", "ringtoneMsgEnabled", false, dark))
    cardRingtone.addView(createListDivider(ctx, cardDividerColor))
    cardRingtone.addView(createFeatureItem(ctx, "音视频铃声", "音视频来电专属铃声", "ringtoneCallEnabled", false, dark))
    
    mainLayout.addView(cardRingtone)
    
    // ====== 工具类 ======
    var cardTool = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardTool.addView(createSection(ctx, "工具类", titleColor, accentTeal));
    
    var toolP1 = createFeatureItem(ctx, "定时消息", "定时发送消息", "timedMsgEnabled", false, dark)
    cardTool.addView(toolP1)
    cardTool.addView(createListDivider(ctx, cardDividerColor))
    cardTool.addView(createFeatureItem(ctx, "天气预报", "实时天气查询", "weatherEnabled", false, dark))
    cardTool.addView(createListDivider(ctx, cardDividerColor))
    cardTool.addView(createFeatureItem(ctx, "实时金价", "黄金价格查询", "goldPriceEnabled", false, dark))
    cardTool.addView(createListDivider(ctx, cardDividerColor))
    cardTool.addView(createFeatureItem(ctx, "IP查询", "IP地址归属查询", "ipQueryEnabled", false, dark))
    
    mainLayout.addView(cardTool)
    
    // ====== 老色批 ======
    var cardLsp = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 4));
    cardLsp.addView(createSection(ctx, "老色批", titleColor, accentRed));
    
    cardLsp.addView(createFeatureItem(ctx, "看看腿", "", null, false, dark))
    cardLsp.addView(createListDivider(ctx, cardDividerColor))
    cardLsp.addView(createFeatureItem(ctx, "卖家秀", "", null, false, dark))
    
    mainLayout.addView(cardLsp)
    
    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)
    
    // ====== 底部按钮 ======
    var footDivider = new View(ctx)
    footDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    footDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(footDivider)
    
    var footLayout = new LinearLayout(ctx)
    footLayout.setOrientation(LinearLayout.HORIZONTAL)
    footLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 10), dpToPx(ctx, 16), dpToPx(ctx, 10))
    footLayout.setBackgroundColor(bgColor)
    footLayout.setGravity(android.view.Gravity.CENTER)
    footLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2))
    
    var reloadBtn = new Button(ctx)
    reloadBtn.setText("重载")
    reloadBtn.setTextSize(14)
    reloadBtn.setTextColor(android.graphics.Color.WHITE)
    reloadBtn.setPadding(0, 0, 0, 0)
    reloadBtn.setGravity(android.view.Gravity.CENTER)
    reloadBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var rlP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.45f)
    rlP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    reloadBtn.setLayoutParams(rlP)
    footLayout.addView(reloadBtn)
    
    var sponsorBtn = new Button(ctx)
    sponsorBtn.setText("助力")
    sponsorBtn.setTextSize(14)
    sponsorBtn.setTextColor(android.graphics.Color.WHITE)
    sponsorBtn.setPadding(0, 0, 0, 0)
    sponsorBtn.setGravity(android.view.Gravity.CENTER)
    sponsorBtn.setBackground(createRoundedDrawable("#E74C3C", 6))
    var spP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.45f)
    spP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    sponsorBtn.setLayoutParams(spP)
    footLayout.addView(sponsorBtn)
    
    mainContainer.addView(footLayout)
    
    // 点击事件
    msgSixty.setOnClickListener((view) -> {
        log("[TTS] 打开每日六十秒面板");
        open60sPanel(getTargetTalker());
    });
    msgMoyu.setOnClickListener((view) -> {
        log("[TTS] 打开摸鱼日报面板");
        openMoyuPanel(getTargetTalker());
    });
    msgHistory.setOnClickListener((view) -> {
        log("[TTS] 打开历史今天面板");
        openHistoryPanel(getTargetTalker());
    });
    emojiItem.setOnClickListener((view) -> {
        log("[TTS] 打开合成表情面板");
        openEmojiPanel(getTargetTalker());
    });
    signItem.setOnClickListener((view) -> {
        log("[TTS] 打开小人举牌面板");
        openSignPanel(getTargetTalker());
    });
    ttsItem.setOnClickListener((view) -> {
        log("[TTS] 打开TTS设置面板");
        openSettingsPanel();
    });
    reloadBtn.setOnClickListener((v_reload_main) -> {
        toast("正在重载插件...");
        reloadPlugin();
    });
    sponsorBtn.setOnClickListener((v_sp_main) -> {
        try {
            ctx.startActivity(new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.baidu.com")));
        } catch (Exception e) {
            toast("打开失败");
        }
    });
    aiPlaceholder.setOnClickListener((view) -> { toast("开发中"); });
    toolP1.setOnClickListener((view) -> { toast("开发中"); });

    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCanceledOnTouchOutside(true)
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.92), (int)(screenHeight * 0.75))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
}

// ====== 创建功能列表项（名称+副标题+Switch） ======
LinearLayout createFeatureItem(Context ctx, String name, String subtitle, String configKey, boolean defaultVal, boolean dark) {
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int subtitleColor = dark ? android.graphics.Color.parseColor("#999999") : android.graphics.Color.parseColor("#7F8C8D");
    
    var item = new LinearLayout(ctx)
    item.setOrientation(LinearLayout.HORIZONTAL)
    item.setGravity(android.view.Gravity.CENTER_VERTICAL)
    item.setPadding(dpToPx(ctx, 12), dpToPx(ctx, 8), dpToPx(ctx, 12), dpToPx(ctx, 8))
    item.setBackgroundColor(bgColor)
    item.setClickable(true)
    
    var textCol = new LinearLayout(ctx)
    textCol.setOrientation(LinearLayout.VERTICAL)
    var textParams = new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
    textCol.setLayoutParams(textParams)
    
    var nameView = new TextView(ctx)
    nameView.setText(name)
    nameView.setTextColor(titleColor)
    nameView.setTextSize(16)
    nameView.setTypeface(null, android.graphics.Typeface.BOLD)
    textCol.addView(nameView)
    
    var subView = new TextView(ctx)
    subView.setText(subtitle)
    subView.setTextColor(subtitleColor)
    subView.setTextSize(12)
    textCol.addView(subView)
    
    item.addView(textCol)
    
    if (configKey != null) {
        var sw = new android.widget.Switch(ctx)
        boolean saved = getBoolean(configKey, defaultVal)
        sw.setChecked(saved)
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                putBoolean(configKey, isChecked);
            }
        });
        item.addView(sw)
    }
    
    return item;
}

// ====== 列表分隔线 ======
View createListDivider(Context ctx, int color) {
    var divider = new View(ctx)
    var params = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 1))
    params.setMargins(dpToPx(ctx, 16), 0, dpToPx(ctx, 16), 0)
    divider.setLayoutParams(params)
    divider.setBackgroundColor(color)
    return divider;
}

// ====== 每日六十秒 ======
void send60s(String talker, boolean isImage) {
    try {
        String token = getString("60sToken", "");
        if (token == null || token.trim().isEmpty()) {
            toast("请先在每日六十秒设置中配置Token");
            return;
        }
        
        var headers = java.util.Map.of("Authorization", "Bearer " + token.trim());
        String api = "https://apix.iqfk.top/api/new?type=json&raw=false";
        
        if (isImage) {
            get(api, headers, respContent -> {
                try {
                    var jsonObj = new org.json.JSONObject(respContent.toString());
                    if (jsonObj.optInt("code") == 200) {
                        var data = jsonObj.optJSONObject("data");
                        String imgUrl = data.optString("image");
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            String path = cacheDir + "/60s_image_" + System.currentTimeMillis() + ".png";
                            download(imgUrl, path, headers, cacheFile -> {
                                sendImage(talker, cacheFile.getAbsolutePath());
                                delay(3000, () -> {
                                    try { cacheFile.delete(); } catch (Exception e) {}
                                });
                            });
                        }
                    }
                } catch (Exception e) {
                    log("[60s] sendImage error: " + e.toString());
                }
            });
        } else {
            get(api, headers, respContent -> {
                try {
                    var jsonObj = new org.json.JSONObject(respContent.toString());
                    if (jsonObj.optInt("code") == 200) {
                        var data = jsonObj.optJSONObject("data");
                        String date = data.optString("date", "");
                        String week = data.optString("week", "");
                        String weiyu = data.optString("weiyu", "");
                        var newsArr = data.optJSONArray("news");
                        
                        StringBuilder sb = new StringBuilder();
                        sb.append("每日60秒 ").append(date).append(" ").append(week).append("\n\n");
                        if (newsArr != null) {
                            for (int i = 0; i < newsArr.length(); i++) {
                                sb.append(newsArr.optString(i)).append("\n");
                            }
                        }
                        if (!weiyu.isEmpty()) {
                            sb.append("\n【微语】").append(weiyu);
                        }
                        sendText(talker, sb.toString());
                    }
                } catch (Exception e) {
                    log("[60s] sendText error: " + e.toString());
                }
            });
        }
    } catch (Exception e) {
        log("[60s] error: " + e.toString());
    }
}

// ====== 摸鱼日报 ======
void fetchMoyu(String talker) {
    String savePath = cacheDir + MOYU_FILE_PREFIX + "_" + System.currentTimeMillis() + ".jpg";
    try {
        log("[Moyu] API请求: url=" + MOYU_API_URL + ", savePath=" + savePath);
        download(MOYU_API_URL, savePath, null, file -> {
            log("[Moyu] 图片下载成功: filePath=" + file.getAbsolutePath());
            try {
                sendImage(talker, file.getAbsolutePath());
                log("[Moyu] 图片发送成功: talker=" + talker);
                delay(3000, () -> {
                    try { file.delete(); } catch (Exception e) {}
                });
            } catch (Exception e) {
                log("[Moyu] 发送图片失败: " + e.toString());
                sendText(talker, "摸鱼日报图片发送失败");
            }
        });
        log("[Moyu] 图片下载请求已发起");
    } catch (Throwable t) {
        log("[Moyu] 图片下载调用失败: " + t.toString());
        sendText(talker, "摸鱼日报图片获取失败：网络不可用");
    }
}

// ====== 历史今天 ======
void sendHistoryToday(String talker) {
    try {
        log("[History] API请求: url=" + HISTORY_API_URL);
        get(HISTORY_API_URL, null, respContent -> {
            try {
                var jsonObj = new org.json.JSONObject(respContent.toString());
                int code = jsonObj.optInt("code");
                if (code == 200) {
                    String url = jsonObj.optString("data");
                    if (url != null && !url.isEmpty()) {
                        String path = cacheDir + HISTORY_FILE_PREFIX + "_" + System.currentTimeMillis() + ".png";
                        download(url, path, null, cacheFile -> {
                            sendImage(talker, cacheFile.getAbsolutePath());
                            delay(3000, () -> {
                                try { cacheFile.delete(); } catch (Exception e) {}
                            });
                        });
                    } else {
                        sendText(talker, "历史今天图片获取失败");
                    }
                } else {
                    sendText(talker, "历史今天图片获取失败");
                }
            } catch (Exception e) {
                log("[History] 解析失败: " + e.toString());
                sendText(talker, "历史今天图片获取失败");
            }
        });
    } catch (Throwable t) {
        log("[History] 请求失败: " + t.toString());
        sendText(talker, "历史今天图片获取失败：网络不可用");
    }
}

// ====== 小人举牌 ======
void saveSignAppIds(LinearLayout cnt) {
    try {
        var idList = new org.json.JSONArray();
        int rowCount = cnt.getChildCount();
        for (int ri = 0; ri < rowCount; ri++) {
            View rowView = cnt.getChildAt(ri);
            if (rowView instanceof LinearLayout) {
                var rl = (LinearLayout) rowView;
                if (rl.getChildCount() >= 2) {
                    var idE = (EditText) rl.getChildAt(0);
                    var nmE = (EditText) rl.getChildAt(1);
                    String idTxt = idE.getText().toString().trim();
                    if (!idTxt.isEmpty()) {
                        var item = new org.json.JSONObject();
                        item.put("id", idTxt);
                        item.put("name", nmE.getText().toString().trim());
                        idList.put(item);
                    }
                }
            }
        }
        putString("signAppIds", idList.toString());
    } catch (Exception e) {
        log("[Sign] saveSignAppIds error: " + e.toString());
    }
}

void addSignAppIdRow(LinearLayout cnt, String idVal, String nameVal, Context ctx, String editBg, int txtColor) {
    var row = new LinearLayout(ctx)
    row.setOrientation(LinearLayout.HORIZONTAL)
    row.setGravity(android.view.Gravity.CENTER_VERTICAL)
    row.setPadding(0, dpToPx(ctx, 3), 0, dpToPx(ctx, 3))
    
    var idE = new EditText(ctx)
    idE.setText(idVal)
    idE.setHint("AppID")
    idE.setTextColor(txtColor)
    idE.setBackgroundColor(android.graphics.Color.parseColor(editBg))
    idE.setTextSize(11)
    idE.setSingleLine(true)
    var idP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.42f)
    idP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    idE.setLayoutParams(idP)
    row.addView(idE)
    
    var nmE = new EditText(ctx)
    nmE.setText(nameVal)
    nmE.setHint("名称")
    nmE.setTextColor(txtColor)
    nmE.setBackgroundColor(android.graphics.Color.parseColor(editBg))
    nmE.setTextSize(11)
    nmE.setSingleLine(true)
    var nP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.38f)
    nP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    nmE.setLayoutParams(nP)
    row.addView(nmE)
    
    var dB = new Button(ctx)
    dB.setText("X")
    dB.setTextSize(11)
    dB.setTextColor(android.graphics.Color.WHITE)
    dB.setPadding(0, 0, 0, 0)
    dB.setBackground(createRoundedDrawable("#E74C3C", 4))
    var dP = new LinearLayout.LayoutParams(dpToPx(ctx, 32), dpToPx(ctx, 28))
    dB.setLayoutParams(dP)
    dB.setTag(row);
    dB.setOnClickListener(new android.view.View.OnClickListener() {
        public void onClick(View v) {
            LinearLayout parentRow = (LinearLayout) v.getTag();
            cnt.removeView(parentRow);
            saveSignAppIds(cnt);
        }
    });
    row.addView(dB)
    
    cnt.addView(row);
}

String getRandomSignAppId() {
    String json = getString("signAppIds", "");
    if (json == null || json.isEmpty()) return null;
    try {
        var arr = new org.json.JSONArray(json);
        if (arr.length() == 0) return null;
        int idx = new java.util.Random().nextInt(arr.length());
        return arr.optJSONObject(idx).optString("id", "");
    } catch (Exception e) {
        return null;
    }
}

void fetchSign(String talker, String msg) {
    String savePath = cacheDir + SIGN_FILE_PREFIX + "_" + System.currentTimeMillis() + ".jpg";
    try {
        String encodedMsg = java.net.URLEncoder.encode(msg, "UTF-8");
        String apiUrl = SIGN_API_URL + encodedMsg;
        log("[Sign] API请求: url=" + apiUrl);
        download(apiUrl, savePath, null, file -> {
            log("[Sign] 下载成功: " + file.getAbsolutePath());
            try {
                boolean withAppId = getBoolean("signWithAppId", true);
                if (withAppId) {
                    String appId;
                    if (getBoolean("signRandomAppId", true)) {
                        appId = getRandomSignAppId();
                    } else {
                        appId = getString("signSelectedAppId", "");
                    }
                    if (appId != null && !appId.isEmpty()) {
                        sendImage(talker, file.getAbsolutePath(), appId);
                    } else {
                        sendImage(talker, file.getAbsolutePath());
                    }
                    toast("举牌AppID: " + (appId == null || appId.isEmpty() ? "无" : appId));
                } else {
                    sendImage(talker, file.getAbsolutePath());
                    toast("举牌: 未附带AppID");
                }
                delay(3000, () -> { try { file.delete(); } catch (Exception e) {} });
            } catch (Exception e) {
                log("[Sign] 发送失败: " + e.toString());
                sendText(talker, "举牌图片发送失败");
            }
        });
    } catch (Throwable t) {
        log("[Sign] 失败: " + t.toString());
        sendText(talker, "举牌图片获取失败：网络不可用");
    }
}
void fetchEmoji(String talker, String emoji1, String emoji2) {
    try {
        String enc1 = java.net.URLEncoder.encode(emoji1.trim(), "UTF-8");
        String enc2 = java.net.URLEncoder.encode(emoji2.trim(), "UTF-8");
        String api = EMOJI_API_URL + enc1 + "&emoji2=" + enc2;
        log("[Emoji] API: " + api);
        get(api, null, respContent -> {
            try {
                var jsonObj = new org.json.JSONObject(respContent.toString());
                if (jsonObj.optInt("code") == 1) {
                    var data = jsonObj.optJSONObject("data");
                    String url = data.optString("url");
                    if (url != null && !url.isEmpty()) {
                        String savePath = cacheDir + EMOJI_FILE_PREFIX + "_" + System.currentTimeMillis() + ".png";
                        download(url, savePath, null, file -> {
                            try {
                                sendEmoji(talker, file.getAbsolutePath());
                                delay(3000, () -> { try { file.delete(); } catch (Exception e) {} });
                            } catch (Exception e) {
                                log("[Emoji] send error: " + e.toString());
                                sendText(talker, "表情合成发送失败");
                            }
                        });
                    } else {
                        sendText(talker, "表情合成失败：未获取到图片URL");
                        toast("表情合成失败：未获取到图片URL");
                    }
                } else {
                    String msg = jsonObj.optString("message", "未知错误");
                    sendText(talker, "[表情合成]合成失败: " + msg);
                    toast("表情合成失败: " + msg);
                }
            } catch (Exception e) {
                log("[Emoji] parse error: " + e.toString());
                sendText(talker, "表情合成解析失败");
                toast("表情合成解析失败");
            }
        });
    } catch (Throwable t) {
        log("[Emoji] error: " + t.toString());
        sendText(talker, "表情合成请求失败：网络不可用");
    }
}
void saveImageToGallery(Context ctx, String imagePath, String name) {
    try {
        var bmp = android.graphics.BitmapFactory.decodeFile(imagePath);
        if (bmp == null) { toast("无法读取图片"); return; }
        var vals = new android.content.ContentValues();
        vals.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name);
        vals.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
        vals.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "DCIM/WeKit");
        vals.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
        var resolver = ctx.getContentResolver();
        var uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, vals);
        var os = resolver.openOutputStream(uri);
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, os);
        os.close();
        vals.clear(); vals.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(uri, vals, null, null);
        toast("已保存到相册");
    } catch (Exception e) { toast("保存失败: " + e.getMessage()); }
}
void openFullscreenImage(Context ctx, String imagePath, String displayName) {
    if (imagePath == null || imagePath.isEmpty()) { toast("无图片可查看"); return; }
    var bmp = android.graphics.BitmapFactory.decodeFile(imagePath);
    if (bmp == null) { toast("无法加载图片"); return; }
    var fullLayout = new LinearLayout(ctx);
    fullLayout.setOrientation(LinearLayout.VERTICAL);
    fullLayout.setBackgroundColor(android.graphics.Color.BLACK);
    
    var fImg = new ImageView(ctx);
    fImg.setImageBitmap(bmp);
    fImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    fImg.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
    fullLayout.addView(fImg);
    
    var btnRow = new LinearLayout(ctx);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    var brLP = new LinearLayout.LayoutParams(-1, dpToPx(ctx, 44));
    brLP.setMargins(dpToPx(ctx, 12), 0, dpToPx(ctx, 12), dpToPx(ctx, 12));
    btnRow.setLayoutParams(brLP);
    btnRow.setGravity(android.view.Gravity.CENTER);
    
    var saveBtn = new Button(ctx);
    saveBtn.setText("保存到相册");
    saveBtn.setTextSize(14);
    saveBtn.setTextColor(android.graphics.Color.WHITE);
    saveBtn.setGravity(android.view.Gravity.CENTER);
    saveBtn.setPadding(0, 0, 0, 0);
    saveBtn.setMinHeight(0);
    saveBtn.setMinimumHeight(0);
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6));
    var saveLP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 1.0f);
    saveLP.setMargins(0, 0, dpToPx(ctx, 4), 0);
    saveBtn.setLayoutParams(saveLP);
    saveBtn.setOnClickListener((v_save_gal) -> saveImageToGallery(ctx, imagePath, displayName));
    btnRow.addView(saveBtn);
    
    var closeBtn = new Button(ctx);
    closeBtn.setText("关闭");
    closeBtn.setTextSize(14);
    closeBtn.setTextColor(android.graphics.Color.WHITE);
    closeBtn.setGravity(android.view.Gravity.CENTER);
    closeBtn.setPadding(0, 0, 0, 0);
    closeBtn.setMinHeight(0);
    closeBtn.setMinimumHeight(0);
    closeBtn.setBackground(createRoundedDrawable("#E74C3C", 6));
    var closeLP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 1.0f);
    closeLP.setMargins(dpToPx(ctx, 4), 0, 0, 0);
    closeBtn.setLayoutParams(closeLP);
    btnRow.addView(closeBtn);
    
    fullLayout.addView(btnRow);
    
    var dlg = new android.app.Dialog(ctx);
    dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    dlg.setContentView(fullLayout);
    dlg.setCancelable(true);
    dlg.setCanceledOnTouchOutside(true);
    closeBtn.setOnClickListener((v_close_gal2) -> dlg.dismiss());
    fImg.setOnClickListener((v_img_tap2) -> dlg.dismiss());
    
    var win = dlg.getWindow();
    if (win != null) {
        win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK));
        var dm = new android.util.DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(dm);
        win.setLayout(dm.widthPixels, dm.heightPixels);
    }
    dlg.show();
}

// ====== 每日六十秒设置面板 ======
void open60sPanel(String talker) {
    log("[60s] open60sPanel 开始执行");
    var ctx = getTopActivity()
    if (ctx == null) {
        log("[60s] open60sPanel: ctx为null");
        return;
    }
    boolean dark = isDarkMode(ctx);
    
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");
    
    String accentTeal = dark ? "#48C9B0" : "#1ABC9C";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";
    String linkColor = dark ? "#5DADE2" : "#2980B9";
    
    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenHeight = displayMetrics.heightPixels
    int screenWidth = displayMetrics.widthPixels
    
    String savedToken = getString("60sToken", "");
    String savedTextCmd = getString("60sTextCommand", "/每日60秒");
    String savedImageCmd = getString("60sImageCommand", "/每日60秒图");
    
    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
    mainContainer.setBackgroundColor(bgColor)
    
    // 标题
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)
    
    var titleText = new TextView(ctx)
    titleText.setText("每日六十秒")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    titleLayout.addView(titleText)
    mainContainer.addView(titleLayout)
    
    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)
    
    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)
    
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)
    
    // ==== 卡片1: 指令 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "指令配置", titleColor, accentTeal));
    var textCmdLayout = createInputRow(ctx, "文字指令", savedTextCmd, labelColor, inputTextColor, editBgHex, dark);
    var textCmdEdit = (EditText) textCmdLayout.getTag();
    card1.addView(textCmdLayout);
    card1.addView(createDivider(ctx, dividerColor));
    var imageCmdLayout = createInputRow(ctx, "图片指令", savedImageCmd, labelColor, inputTextColor, editBgHex, dark);
    var imageCmdEdit = (EditText) imageCmdLayout.getTag();
    card1.addView(imageCmdLayout);
    mainLayout.addView(card1);
    
    // ==== 卡片2: API Token ====
    var card2 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card2.addView(createSection(ctx, "API Token", titleColor, accentTeal));
    var tokenLayout = createInputRow(ctx, "Token", savedToken, labelColor, inputTextColor, editBgHex, dark);
    var tokenEdit = (EditText) tokenLayout.getTag();
    tokenEdit.setHint("请输入API Token");
    card2.addView(tokenLayout);
    
    var linkRow = new LinearLayout(ctx)
    linkRow.setOrientation(LinearLayout.HORIZONTAL)
    linkRow.setPadding(0, dpToPx(ctx, 4), dpToPx(ctx, 14), dpToPx(ctx, 6))
    linkRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    
    var linkText = new TextView(ctx)
    linkText.setText("https://apix.iqfk.top")
    linkText.setTextColor(android.graphics.Color.parseColor(linkColor))
    linkText.setTextSize(12)
    linkText.setPaintFlags(linkText.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG)
    linkText.setClickable(true);
    linkText.setOnClickListener((v_link) -> {
        try {
            var intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://apix.iqfk.top"));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
        } catch (Exception e) {}
    });
    linkRow.addView(linkText)
    
    var copyBtn = new Button(ctx)
    copyBtn.setText("复制")
    copyBtn.setTextSize(12)
    copyBtn.setTextColor(android.graphics.Color.WHITE)
    copyBtn.setPadding(dpToPx(ctx, 8), dpToPx(ctx, 2), dpToPx(ctx, 8), dpToPx(ctx, 2))
    copyBtn.setMinHeight(0)
    copyBtn.setMinimumHeight(0)
    copyBtn.setBackground(createRoundedDrawable(linkColor, 4))
    var copyLP = new LinearLayout.LayoutParams(-2, -2)
    copyLP.setMargins(dpToPx(ctx, 8), 0, 0, 0)
    copyBtn.setLayoutParams(copyLP)
    copyBtn.setOnClickListener((v_copy) -> {
        try {
            var clipboard = (android.content.ClipboardManager) ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            var clip = android.content.ClipData.newPlainText("url", "https://apix.iqfk.top");
            clipboard.setPrimaryClip(clip);
            toast("已复制链接到剪贴板");
        } catch (Exception e) {}
    });
    linkRow.addView(copyBtn)
    card2.addView(linkRow)
    mainLayout.addView(card2);
    
    // ==== 卡片3: 预览 ====
    String stTk = (talker != null && !talker.isEmpty()) ? talker : "";
    var card3 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card3.addView(createSection(ctx, "预览", titleColor, accentTeal));
    
    var prevRow = new LinearLayout(ctx)
    prevRow.setOrientation(LinearLayout.HORIZONTAL)
    prevRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    
    var pvBtn = new Button(ctx)
    pvBtn.setText("预览")
    pvBtn.setTextSize(13)
    pvBtn.setTextColor(android.graphics.Color.WHITE)
    pvBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    pvBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    pvBtn.setMinHeight(0)
    pvBtn.setMinimumHeight(0)
    var pvBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    pvBP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    pvBtn.setLayoutParams(pvBP)
    prevRow.addView(pvBtn)
    
    var sendBtn = new Button(ctx)
    sendBtn.setText("发送")
    sendBtn.setTextSize(13)
    sendBtn.setTextColor(android.graphics.Color.WHITE)
    sendBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    sendBtn.setBackground(createRoundedDrawable(accentOrange, 6))
    sendBtn.setMinHeight(0)
    sendBtn.setMinimumHeight(0)
    var ssBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    ssBP.setMargins(dpToPx(ctx, 4), 0, 0, 0)
    sendBtn.setLayoutParams(ssBP)
    prevRow.addView(sendBtn)
    
    card3.addView(prevRow)
    
    var pvImg = new ImageView(ctx)
    pvImg.setBackgroundColor(dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#ECF0F1"));
    var pvIP = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 200))
    pvIP.setMargins(0, dpToPx(ctx, 8), 0, 0)
    pvImg.setLayoutParams(pvIP)
    pvImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    pvImg.setClickable(true);
    card3.addView(pvImg);
    mainLayout.addView(card3);
    
    String[] sPreviewPath = new String[1];
    String sCachePath = cacheDir + "/60s_preview.png";
    // 加载缓存
    try {
        var cachedBmp = android.graphics.BitmapFactory.decodeFile(sCachePath);
        if (cachedBmp != null) {
            pvImg.setImageBitmap(cachedBmp);
            sPreviewPath[0] = sCachePath;
        }
    } catch (Exception e) {}
    // 点击全屏
    pvImg.setOnClickListener((v_60s_full) -> {
        if (sPreviewPath[0] != null) {
            openFullscreenImage(ctx, sPreviewPath[0], "60s_preview.png");
        }
    })
    
    pvBtn.setOnClickListener((v_60s_prev) -> {
        String token = getString("60sToken", "");
        if (token == null || token.trim().isEmpty()) {
            toast("请先在 Token 配置中设置 API Token")
            return
        }
        toast("正在获取每日六十秒图片...")
        try {
            var headers = java.util.Map.of("Authorization", "Bearer " + token.trim());
            String api = "https://apix.iqfk.top/api/new?type=json&raw=false";
            sPreviewPath[0] = sCachePath;
            get(api, headers, respContent -> {
                try {
                    var jsonObj = new org.json.JSONObject(respContent.toString());
                    if (jsonObj.optInt("code") == 200) {
                        var data = jsonObj.optJSONObject("data");
                        String imgUrl = data.optString("image");
                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            download(imgUrl, sCachePath, headers, pf -> {
                                try {
                                    var bmp = android.graphics.BitmapFactory.decodeFile(pf.getAbsolutePath());
                                    if (bmp != null) {
                                        pvImg.post(new Runnable() {
                                            public void run() {
                                                pvImg.setImageBitmap(bmp);
                                            }
                                        });
                                    }
                                } catch (Exception e2) {
                                    log("[60s] preview decode error: " + e2.toString());
                                }
                            });
                        } else {
                            toast("未获取到图片URL");
                        }
                    } else {
                        toast("API返回异常");
                    }
                } catch (Exception e1) {
                    toast("预览失败：" + e1.getMessage());
                }
            });
        } catch (Exception e0) {
            toast("预览失败：" + e0.getMessage());
        }
    })
    
    sendBtn.setOnClickListener((v_60s_send) -> {
        if (stTk.isEmpty()) {
            toast("无法发送：未指定对话")
            return
        }
        String pfp = sPreviewPath[0];
        if (pfp == null || pfp.isEmpty()) {
            toast("请先预览图片")
            return
        }
        try {
            sendImage(stTk, pfp);
            toast("已发送")
        } catch (Exception e) {
            toast("发送失败：" + e.getMessage());
        }
    })
    
    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)
    
    // 底部按钮
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)
    
    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setGravity(android.view.Gravity.CENTER)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var resetParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    resetParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(resetParams)
    buttonLayout.addView(resetBtn)
    
    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setGravity(android.view.Gravity.CENTER)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var cancelParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    cancelParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(cancelParams)
    buttonLayout.addView(cancelBtn)
    
    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setGravity(android.view.Gravity.CENTER)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var saveParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    saveParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(saveParams)
    buttonLayout.addView(saveBtn)
    
    mainContainer.addView(buttonLayout)
    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.72))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    // ====== 事件绑定 ======
    saveBtn.setOnClickListener((v_save_60s) -> {
        try {
            String token = tokenEdit.getText().toString().trim()
            putString("60sToken", token)
            putString("60sTextCommand", textCmdEdit.getText().toString().trim())
            putString("60sImageCommand", imageCmdEdit.getText().toString().trim())
            toast("保存成功")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[60s] save error: " + e.toString());
        }
    })
    
    cancelBtn.setOnClickListener((v_cancel_60s) -> {
        dialog.dismiss()
    })
    
    resetBtn.setOnClickListener((v_reset_60s) -> {
        tokenEdit.setText("")
        textCmdEdit.setText(DEFAULT_OPEN_COMMAND)
        imageCmdEdit.setText(DEFAULT_SEND_COMMAND)
        pvImg.setImageBitmap(null)
        toast("已重置")
    })
}

// ====== 摸鱼日报设置面板 ======
void openMoyuPanel(String talker) {
    log("[Moyu] openMoyuPanel 开始执行");
    var ctx = getTopActivity()
    if (ctx == null) { log("[Moyu] ctx为null"); return; }
    boolean dark = isDarkMode(ctx);

    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");

    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String accentTeal = dark ? "#48C9B0" : "#1ABC9C";
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";

    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenHeight = displayMetrics.heightPixels
    int screenWidth = displayMetrics.widthPixels

    String savedCmd = getString("moyuCommand", "/摸鱼日报");

    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
    mainContainer.setBackgroundColor(bgColor)

    // 标题
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)

    var titleText = new TextView(ctx)
    titleText.setText("摸鱼日报")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    titleLayout.addView(titleText)
    mainContainer.addView(titleLayout)

    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)

    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)

    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)

    // ==== 卡片1: 指令配置 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "指令配置", titleColor, accentTeal));
    var cmdLayout = createInputRow(ctx, "指令", savedCmd, labelColor, inputTextColor, editBgHex, dark);
    var cmdEdit = (EditText) cmdLayout.getTag();
    card1.addView(cmdLayout);
    mainLayout.addView(card1);

    // ==== 卡片2: 预览 ====
    var card2 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card2.addView(createSection(ctx, "预览", titleColor, accentTeal));

    var prevRow = new LinearLayout(ctx)
    prevRow.setOrientation(LinearLayout.HORIZONTAL)
    prevRow.setGravity(android.view.Gravity.CENTER_VERTICAL)

    var pvBtn = new Button(ctx)
    pvBtn.setText("预览")
    pvBtn.setTextSize(13)
    pvBtn.setTextColor(android.graphics.Color.WHITE)
    pvBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    pvBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    pvBtn.setMinHeight(0)
    pvBtn.setMinimumHeight(0)
    var pvBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    pvBP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    pvBtn.setLayoutParams(pvBP)
    prevRow.addView(pvBtn)
    
    var sendBtn = new Button(ctx)
    sendBtn.setText("发送")
    sendBtn.setTextSize(13)
    sendBtn.setTextColor(android.graphics.Color.WHITE)
    sendBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    sendBtn.setBackground(createRoundedDrawable(accentOrange, 6))
    sendBtn.setMinHeight(0)
    sendBtn.setMinimumHeight(0)
    var ssBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    ssBP.setMargins(dpToPx(ctx, 4), 0, 0, 0)
    sendBtn.setLayoutParams(ssBP)
    prevRow.addView(sendBtn)

    card2.addView(prevRow)

    var pvImg = new ImageView(ctx)
    pvImg.setBackgroundColor(dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#ECF0F1"));
    var pvIP = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 200))
    pvIP.setMargins(0, dpToPx(ctx, 8), 0, 0)
    pvImg.setLayoutParams(pvIP)
    pvImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    pvImg.setClickable(true);
    card2.addView(pvImg);
    mainLayout.addView(card2);

    String[] previewPath_moyu = new String[1];
    String cachePath_moyu = cacheDir + "/myrb_preview.png";
    // 加载缓存
    try {
        var cachedBmp = android.graphics.BitmapFactory.decodeFile(cachePath_moyu);
        if (cachedBmp != null) {
            pvImg.setImageBitmap(cachedBmp);
            previewPath_moyu[0] = cachePath_moyu;
        }
    } catch (Exception e) {}
    // 点击全屏
    pvImg.setOnClickListener((v_moyu_full) -> {
        if (previewPath_moyu[0] != null) {
            openFullscreenImage(ctx, previewPath_moyu[0], "myrb_preview.png");
        }
    })

    pvBtn.setOnClickListener((v_moyu_prev) -> {
        toast("正在获取摸鱼日报...")
        try {
            previewPath_moyu[0] = cachePath_moyu;
            download(MOYU_API_URL, cachePath_moyu, null, pf -> {
                try {
                    var bmp = android.graphics.BitmapFactory.decodeFile(pf.getAbsolutePath());
                    if (bmp != null) {
                        pvImg.post(new Runnable() {
                            public void run() {
                                pvImg.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception e2) {
                    log("[Moyu] preview decode error: " + e2.toString());
                }
            });
        } catch (Exception e0) {
            toast("预览失败：" + e0.getMessage());
        }
    })

    sendBtn.setOnClickListener((v_moyu_send) -> {
        if (talker == null || talker.isEmpty()) {
            toast("无法发送：未指定对话")
            return
        }
        String pfp_moyu = previewPath_moyu[0];
        if (pfp_moyu == null || pfp_moyu.isEmpty()) {
            toast("请先预览图片")
            return
        }
        try {
            sendImage(talker, pfp_moyu);
            toast("已发送")
        } catch (Exception e) {
            toast("发送失败：" + e.getMessage());
        }
    })

    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)

    // 底部按钮
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)

    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))

    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setGravity(android.view.Gravity.CENTER)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var resetParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    resetParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(resetParams)
    buttonLayout.addView(resetBtn)

    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setGravity(android.view.Gravity.CENTER)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var cancelParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    cancelParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(cancelParams)
    buttonLayout.addView(cancelBtn)

    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setGravity(android.view.Gravity.CENTER)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var saveParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    saveParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(saveParams)
    buttonLayout.addView(saveBtn)

    mainContainer.addView(buttonLayout)
    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.58))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    // ====== 事件绑定 ======
    saveBtn.setOnClickListener((v_save_moyu) -> {
        try {
            String cmd = cmdEdit.getText().toString().trim()
            putString("moyuCommand", cmd)
            toast("保存成功")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[Moyu] save error: " + e.toString());
        }
    })

    cancelBtn.setOnClickListener((v_cancel_moyu) -> {
        dialog.dismiss()
    })

    resetBtn.setOnClickListener((v_reset_moyu) -> {
        cmdEdit.setText("/摸鱼日报")
        pvImg.setImageBitmap(null)
        previewPath_moyu[0] = null;
        toast("已重置")
    })
}

// ====== 历史今天设置面板 ======
void openHistoryPanel(String talker) {
    log("[History] openHistoryPanel 开始执行");
    var ctx = getTopActivity()
    if (ctx == null) { log("[History] ctx为null"); return; }
    boolean dark = isDarkMode(ctx);

    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");

    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String accentTeal = dark ? "#48C9B0" : "#1ABC9C";
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";

    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenHeight = displayMetrics.heightPixels
    int screenWidth = displayMetrics.widthPixels

    String savedCmd = getString("historyCommand", "/历史今天");

    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT))
    mainContainer.setBackgroundColor(bgColor)

    // 标题
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)

    var titleText = new TextView(ctx)
    titleText.setText("历史今天")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    titleLayout.addView(titleText)
    mainContainer.addView(titleLayout)

    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)

    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)

    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)

    // ==== 卡片1: 指令配置 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "指令配置", titleColor, accentTeal));
    var cmdLayout = createInputRow(ctx, "指令", savedCmd, labelColor, inputTextColor, editBgHex, dark);
    var cmdEdit = (EditText) cmdLayout.getTag();
    card1.addView(cmdLayout);
    mainLayout.addView(card1);

    // ==== 卡片2: 预览 ====
    var card2 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card2.addView(createSection(ctx, "预览", titleColor, accentTeal));

    var prevRow = new LinearLayout(ctx)
    prevRow.setOrientation(LinearLayout.HORIZONTAL)
    prevRow.setGravity(android.view.Gravity.CENTER_VERTICAL)

    var pvBtn = new Button(ctx)
    pvBtn.setText("预览")
    pvBtn.setTextSize(13)
    pvBtn.setTextColor(android.graphics.Color.WHITE)
    pvBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    pvBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    pvBtn.setMinHeight(0)
    pvBtn.setMinimumHeight(0)
    var pvBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    pvBP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    pvBtn.setLayoutParams(pvBP)
    prevRow.addView(pvBtn)

    var sendBtn = new Button(ctx)
    sendBtn.setText("发送")
    sendBtn.setTextSize(13)
    sendBtn.setTextColor(android.graphics.Color.WHITE)
    sendBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    sendBtn.setBackground(createRoundedDrawable(accentOrange, 6))
    sendBtn.setMinHeight(0)
    sendBtn.setMinimumHeight(0)
    var ssBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    ssBP.setMargins(dpToPx(ctx, 4), 0, 0, 0)
    sendBtn.setLayoutParams(ssBP)
    prevRow.addView(sendBtn)

    card2.addView(prevRow)

    var pvImg = new ImageView(ctx)
    pvImg.setBackgroundColor(dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#ECF0F1"));
    var pvIP = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 200))
    pvIP.setMargins(0, dpToPx(ctx, 8), 0, 0)
    pvImg.setLayoutParams(pvIP)
    pvImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    pvImg.setClickable(true);
    card2.addView(pvImg);
    mainLayout.addView(card2);

    String[] previewPath_history = new String[1];
    String cachePath_history = cacheDir + "/history_preview.png";
    // 加载缓存
    try {
        var cachedBmp = android.graphics.BitmapFactory.decodeFile(cachePath_history);
        if (cachedBmp != null) {
            pvImg.setImageBitmap(cachedBmp);
            previewPath_history[0] = cachePath_history;
        }
    } catch (Exception e) {}
    // 点击全屏
    pvImg.setOnClickListener((v_history_full) -> {
        if (previewPath_history[0] != null) {
            openFullscreenImage(ctx, previewPath_history[0], "history_preview.png");
        }
    })

    pvBtn.setOnClickListener((v_history_prev) -> {
        toast("正在获取历史今天图片...")
        try {
            previewPath_history[0] = cachePath_history;
            download(HISTORY_API_URL, cachePath_history, null, pf -> {
                try {
                    var bmp = android.graphics.BitmapFactory.decodeFile(pf.getAbsolutePath());
                    if (bmp != null) {
                        pvImg.post(new Runnable() {
                            public void run() {
                                pvImg.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception e2) {
                    log("[History] preview decode error: " + e2.toString());
                }
            });
        } catch (Exception e0) {
            toast("预览失败：" + e0.getMessage());
        }
    })

    sendBtn.setOnClickListener((v_history_send) -> {
        if (talker == null || talker.isEmpty()) {
            toast("无法发送：未指定对话")
            return
        }
        String pfp_history = previewPath_history[0];
        if (pfp_history == null || pfp_history.isEmpty()) {
            toast("请先预览图片")
            return
        }
        try {
            sendImage(talker, pfp_history);
            toast("已发送")
        } catch (Exception e) {
            toast("发送失败：" + e.getMessage());
        }
    })

    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)

    // 底部按钮
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)

    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))

    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setGravity(android.view.Gravity.CENTER)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var resetParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    resetParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(resetParams)
    buttonLayout.addView(resetBtn)

    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setGravity(android.view.Gravity.CENTER)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var cancelParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    cancelParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(cancelParams)
    buttonLayout.addView(cancelBtn)

    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setGravity(android.view.Gravity.CENTER)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var saveParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    saveParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(saveParams)
    buttonLayout.addView(saveBtn)

    mainContainer.addView(buttonLayout)
    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.62))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    // 事件绑定
    saveBtn.setOnClickListener((v_save_history) -> {
        try {
            String cmd = cmdEdit.getText().toString().trim()
            putString("historyCommand", cmd)
            toast("保存成功")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[History] save error: " + e.toString());
        }
    })

    cancelBtn.setOnClickListener((v_cancel_history) -> {
        dialog.dismiss()
    })

    resetBtn.setOnClickListener((v_reset_history) -> {
        cmdEdit.setText("/历史今天")
        pvImg.setImageBitmap(null)
        previewPath_history[0] = null;
        toast("已重置")
    })
}

// ====== 小人举牌设置面板 ======
void openSignPanel(String talker) {
    log("[Sign] openSignPanel 开始执行");
    var ctx = getTopActivity()
    if (ctx == null) { log("[Sign] ctx为null"); return; }
    boolean dark = isDarkMode(ctx);

    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");

    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";

    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenWidth = displayMetrics.widthPixels
    int screenHeight = displayMetrics.heightPixels

    String savedCmd = getString("signCommand", DEFAULT_SIGN_COMMAND);
    String sfTk = (talker != null && !talker.isEmpty()) ? talker : "";

    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1))
    mainContainer.setBackgroundColor(bgColor)

    // 标题
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)

    var titleText = new TextView(ctx)
    titleText.setText("小人举牌")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f))
    titleLayout.addView(titleText)
    mainContainer.addView(titleLayout)

    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(-1, 1))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)

    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(-1, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)

    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)

    // ==== 卡片1: 指令配置 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "指令配置", titleColor, accentBlue));
    var cmdLayout = createInputRow(ctx, "触发指令", savedCmd, labelColor, inputTextColor, editBgHex, dark);
    var cmdEdit = (EditText) cmdLayout.getTag();
    cmdEdit.setHint("输入 /举牌 触发");
    card1.addView(cmdLayout);
    mainLayout.addView(card1);

    // ==== 卡片2: AppID管理 ====
    var card2 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card2.addView(createSection(ctx, "AppID管理", titleColor, accentBlue));

    var appIdContainer = new LinearLayout(ctx)
    appIdContainer.setOrientation(LinearLayout.VERTICAL)
    appIdContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2))
    card2.addView(appIdContainer);

    // 加载已保存的AppID
    String savedIdsJson = getString("signAppIds", "");
    if (savedIdsJson != null && !savedIdsJson.isEmpty()) {
        try {
            var idArr = new org.json.JSONArray(savedIdsJson);
            for (int i = 0; i < idArr.length(); i++) {
                var item = idArr.optJSONObject(i);
                String idVal = item.optString("id", "");
                String nameVal = item.optString("name", "");
                addSignAppIdRow(appIdContainer, idVal, nameVal, ctx, editBgHex, inputTextColor);
            }
        } catch (Exception e) {
            log("[Sign] load AppIDs error: " + e.toString());
        }
    } else {
        // 默认加载预设
        for (int ri = 0; ri < SIGN_PRESET_APP_IDS.length; ri++) {
            addSignAppIdRow(appIdContainer, SIGN_PRESET_APP_IDS[ri], "", ctx, editBgHex, inputTextColor);
        }
    }

    var addBtn = new Button(ctx)
    addBtn.setText("+ 添加")
    addBtn.setTextSize(13)
    addBtn.setTextColor(android.graphics.Color.WHITE)
    addBtn.setPadding(dpToPx(ctx, 10), dpToPx(ctx, 4), dpToPx(ctx, 10), dpToPx(ctx, 4))
    addBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    addBtn.setMinHeight(0)
    addBtn.setMinimumHeight(0)
    addBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dpToPx(ctx, 34)))
    card2.addView(addBtn);
    mainLayout.addView(card2);

    // ==== 卡片3: 发送设置 ====
    var card3 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card3.addView(createSection(ctx, "发送设置", titleColor, accentBlue));

    boolean savedWithAppId = getBoolean("signWithAppId", true);
    boolean savedRandomAppId = getBoolean("signRandomAppId", true);

    var withAppIdRow = createSwitchRow(ctx, "附带AppID发送", "发送时附带AppID", savedWithAppId, dark);
    var withAppIdSwitch = (android.widget.Switch) withAppIdRow.getTag();
    card3.addView(withAppIdRow);
    card3.addView(createDivider(ctx, dividerColor));

    var randomAppIdRow = createSwitchRow(ctx, "随机AppID", "从列表中随机选取AppID", savedRandomAppId, dark);
    var randomAppIdSwitch = (android.widget.Switch) randomAppIdRow.getTag();
    card3.addView(randomAppIdRow);

    String savedSelectedId = getString("signSelectedAppId", "");
    String savedSelectedName = getString("signSelectedAppName", "");
    var selectAppIdRow = new LinearLayout(ctx)
    selectAppIdRow.setOrientation(LinearLayout.HORIZONTAL)
    selectAppIdRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    selectAppIdRow.setPadding(0, dpToPx(ctx, 8), 0, dpToPx(ctx, 4))

    var selectLabel = new TextView(ctx)
    selectLabel.setText("默认AppID: " + (savedSelectedId.isEmpty() ? "未选择" : savedSelectedId))
    selectLabel.setTextColor(labelColor)
    selectLabel.setTextSize(14)
    selectLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f))
    selectAppIdRow.addView(selectLabel)

    var selectBtn = new Button(ctx)
    selectBtn.setText("选择")
    selectBtn.setTextSize(12)
    selectBtn.setTextColor(android.graphics.Color.WHITE)
    selectBtn.setPadding(dpToPx(ctx, 8), 0, dpToPx(ctx, 8), 0)
    selectBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    selectBtn.setMinHeight(0)
    selectBtn.setMinimumHeight(0)
    selectBtn.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(ctx, 60), dpToPx(ctx, 30)))
    selectAppIdRow.addView(selectBtn)

    if (savedRandomAppId) {
        selectAppIdRow.setVisibility(android.view.View.GONE);
    }

    String[] selectedAppIdRef = new String[2];
    selectedAppIdRef[0] = savedSelectedId;
    selectedAppIdRef[1] = savedSelectedName;
    selectLabel.setTag(new Object[]{savedSelectedId, savedSelectedName});

    // 选择按钮点击 - 弹出子对话框
    selectBtn.setOnClickListener((v_select_sign) -> {
        var subLayout = new LinearLayout(ctx)
        subLayout.setOrientation(LinearLayout.VERTICAL)
        subLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 12), dpToPx(ctx, 16), dpToPx(ctx, 12))

        int subBg = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.WHITE;
        subLayout.setBackgroundColor(subBg)

        int rowCount = appIdContainer.getChildCount();
        String currentSel = selectedAppIdRef[0] != null ? selectedAppIdRef[0] : "";
        for (int si = 0; si < rowCount; si++) {
            View sv = appIdContainer.getChildAt(si);
            if (sv instanceof LinearLayout) {
                var srl = (LinearLayout) sv;
                if (srl.getChildCount() >= 2) {
                    var sidE = (EditText) srl.getChildAt(0);
                    var nmE = (EditText) srl.getChildAt(1);
                    String sid = sidE.getText().toString().trim();
                    String nm = nmE.getText().toString().trim();
                    if (!sid.isEmpty()) {
                        String displayText = sid;
                        if (!nm.isEmpty()) displayText = nm + " (" + sid + ")";
                        var optionRow = new LinearLayout(ctx)
                        optionRow.setOrientation(LinearLayout.HORIZONTAL)
                        optionRow.setPadding(dpToPx(ctx, 12), dpToPx(ctx, 10), dpToPx(ctx, 12), dpToPx(ctx, 10))
                        optionRow.setGravity(android.view.Gravity.CENTER_VERTICAL)

                        var textLabel = new TextView(ctx)
                        textLabel.setText(displayText)
                        textLabel.setTextSize(14)
                        textLabel.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f))
                        optionRow.addView(textLabel)

                        if (sid.equals(currentSel)) {
                            optionRow.setBackground(createRoundedDrawable("#3498DB", 6))
                            textLabel.setTextColor(android.graphics.Color.WHITE)
                            var checkLabel = new TextView(ctx)
                            checkLabel.setText("✓")
                            checkLabel.setTextColor(android.graphics.Color.WHITE)
                            checkLabel.setTextSize(16)
                            optionRow.addView(checkLabel)
                        } else {
                            textLabel.setTextColor(dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50"))
                        }

                        optionRow.setClickable(true);
                        String finalSid = sid;
                        optionRow.setOnClickListener((v_opt) -> {
                            selectedAppIdRef[0] = finalSid;
                            selectLabel.setText("默认AppID: " + finalSid)
                            selectLabel.setTag(new Object[]{finalSid, ""})
                            putString("signSelectedAppId", finalSid)
                            // 关闭子对话框
                            try {
                                var parentDlg = (android.app.Dialog) v_opt.getTag();
                                if (parentDlg != null) parentDlg.dismiss();
                            } catch (Exception ex) {}
                        })

                        subLayout.addView(optionRow)
                    }
                }
            }
        }

        var subDialog = new AlertDialog.Builder(ctx)
            .setTitle("选择 AppID")
            .setView(subLayout).create()
        subDialog.setCancelable(true)
        subDialog.setCanceledOnTouchOutside(true)

        // 为每个选项设置父对话框引用
        for (int ci = 0; ci < subLayout.getChildCount(); ci++) {
            View cv = subLayout.getChildAt(ci);
            if (cv instanceof LinearLayout) {
                cv.setTag(subDialog);
            }
        }

        var subWindow = subDialog.getWindow()
        if (subWindow != null) {
            subWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(subBg))
            subWindow.setLayout((int)(screenWidth * 0.7), -2)
        }
        subDialog.show()
    })

    card3.addView(selectAppIdRow);
    mainLayout.addView(card3);

    // 随机AppID开关 - 显示/隐藏选择行
    randomAppIdSwitch.setOnCheckedChangeListener((v_rand, isChecked) -> {
        selectAppIdRow.setVisibility(isChecked ? android.view.View.GONE : android.view.View.VISIBLE);
    });

    // ==== 卡片4: 预览 ====
    var card4 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card4.addView(createSection(ctx, "预览", titleColor, accentBlue));

    var inputRow = createInputRow(ctx, "文字", "", labelColor, inputTextColor, editBgHex, dark);
    var prevEdit = (EditText) inputRow.getTag();
    prevEdit.setHint("请输入举牌文字");
    card4.addView(inputRow);

    var btnRow = new LinearLayout(ctx)
    btnRow.setOrientation(LinearLayout.HORIZONTAL)
    btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL)

    var prevBtn = new Button(ctx)
    prevBtn.setText("预览")
    prevBtn.setTextSize(13)
    prevBtn.setTextColor(android.graphics.Color.WHITE)
    prevBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    prevBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    prevBtn.setMinHeight(0)
    prevBtn.setMinimumHeight(0)
    var pbP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    pbP.setMargins(0, dpToPx(ctx, 8), dpToPx(ctx, 4), 0)
    prevBtn.setLayoutParams(pbP)
    btnRow.addView(prevBtn)

    var signSendBtn = new Button(ctx)
    signSendBtn.setText("发送")
    signSendBtn.setTextSize(13)
    signSendBtn.setTextColor(android.graphics.Color.WHITE)
    signSendBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    signSendBtn.setBackground(createRoundedDrawable(accentOrange, 6))
    signSendBtn.setMinHeight(0)
    signSendBtn.setMinimumHeight(0)
    var sbP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 0.5f)
    sbP.setMargins(dpToPx(ctx, 4), dpToPx(ctx, 8), 0, 0)
    signSendBtn.setLayoutParams(sbP)
    btnRow.addView(signSendBtn)

    card4.addView(btnRow)

    var previewImg = new ImageView(ctx)
    previewImg.setBackgroundColor(dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#ECF0F1"));
    var pImgP = new LinearLayout.LayoutParams(-1, dpToPx(ctx, 200))
    pImgP.setMargins(0, dpToPx(ctx, 8), 0, 0)
    previewImg.setLayoutParams(pImgP)
    previewImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    previewImg.setClickable(true);
    card4.addView(previewImg);
    mainLayout.addView(card4);

    String[] previewPathHolder = new String[1];
    String signCachePath = cacheDir + SIGN_FILE_PREFIX + "_preview.png";
    // 加载缓存
    try {
        var cachedBmp = android.graphics.BitmapFactory.decodeFile(signCachePath);
        if (cachedBmp != null) {
            previewImg.setImageBitmap(cachedBmp);
            previewPathHolder[0] = signCachePath;
        }
    } catch (Exception e) {}
    // 点击全屏
    previewImg.setOnClickListener((v_sign_full) -> {
        if (previewPathHolder[0] != null) {
            openFullscreenImage(ctx, previewPathHolder[0], "sign_preview.png");
        }
    })

    prevBtn.setOnClickListener((v_preview_sign) -> {
        String txt = prevEdit.getText().toString().trim();
        if (txt.isEmpty()) {
            toast("请输入预览文字")
            return
        }
        toast("正在生成预览...")
        try {
            String enc = java.net.URLEncoder.encode(txt, "UTF-8");
            previewPathHolder[0] = signCachePath;
            download(SIGN_API_URL + enc, signCachePath, null, pf -> {
                try {
                    var bmp = android.graphics.BitmapFactory.decodeFile(pf.getAbsolutePath());
                    if (bmp != null) {
                        previewImg.post(new Runnable() {
                            public void run() {
                                previewImg.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception e) {
                    log("[Sign] preview error: " + e.toString());
                }
            });
        } catch (Exception e) {
            toast("预览失败：" + e.getMessage());
        }
    })

    signSendBtn.setOnClickListener((v_sign_send) -> {
        if (sfTk.isEmpty()) {
            toast("无法发送：未指定对话")
            return
        }
        String pfp = previewPathHolder[0];
        if (pfp == null || pfp.isEmpty()) {
            toast("请先预览图片")
            return
        }
        try {
            boolean withAppId = getBoolean("signWithAppId", true);
            if (withAppId) {
                String appId;
                if (getBoolean("signRandomAppId", true)) {
                    appId = getRandomSignAppId();
                } else {
                    appId = getString("signSelectedAppId", "");
                }
                if (appId != null && !appId.isEmpty()) {
                    sendImage(sfTk, pfp, appId);
                } else {
                    sendImage(sfTk, pfp);
                }
                toast("举牌AppID: " + (appId == null || appId.isEmpty() ? "无" : appId));
            } else {
                sendImage(sfTk, pfp);
                toast("举牌: 未附带AppID");
            }
            toast("已发送")
        } catch (Exception e) {
            toast("发送失败：" + e.getMessage());
        }
    })

    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)

    // 底部按钮
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(-1, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)

    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2))

    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setGravity(android.view.Gravity.CENTER)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var rzP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    rzP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(rzP)
    buttonLayout.addView(resetBtn)

    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setGravity(android.view.Gravity.CENTER)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var czP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    czP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(czP)
    buttonLayout.addView(cancelBtn)

    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setGravity(android.view.Gravity.CENTER)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var svP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    svP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(svP)
    buttonLayout.addView(saveBtn)

    mainContainer.addView(buttonLayout)
    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.85))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    // 事件绑定
    saveBtn.setOnClickListener((v_save_sign) -> {
        try {
            String cmd = cmdEdit.getText().toString().trim()
            if (cmd.isEmpty()) {
                toast("请输入指令")
                return
            }
            putString("signCommand", cmd)

            // 收集所有AppID行
            var idList = new org.json.JSONArray();
            int rowCount = appIdContainer.getChildCount();
            for (int ri = 0; ri < rowCount; ri++) {
                View rowView = appIdContainer.getChildAt(ri);
                if (rowView instanceof LinearLayout) {
                    var rl = (LinearLayout) rowView;
                    if (rl.getChildCount() >= 2) {
                        var idE = (EditText) rl.getChildAt(0);
                        var nmE = (EditText) rl.getChildAt(1);
                        String idTxt = idE.getText().toString().trim();
                        if (!idTxt.isEmpty()) {
                            var item = new org.json.JSONObject();
                            item.put("id", idTxt);
                            item.put("name", nmE.getText().toString().trim());
                            idList.put(item);
                        }
                    }
                }
            }
            putString("signAppIds", idList.toString());
            putBoolean("signWithAppId", withAppIdSwitch.isChecked());
            putBoolean("signRandomAppId", randomAppIdSwitch.isChecked());
            Object[] selTag = (Object[]) selectLabel.getTag();
            if (selTag != null) {
                putString("signSelectedAppId", (String) selTag[0]);
                putString("signSelectedAppName", (String) selTag[1]);
            } else if (selectedAppIdRef[0] != null && !selectedAppIdRef[0].isEmpty()) {
                putString("signSelectedAppId", selectedAppIdRef[0]);
                putString("signSelectedAppName", selectedAppIdRef[1]);
            }

            toast("保存成功")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[Sign] save error: " + e.toString());
        }
    })

    cancelBtn.setOnClickListener((v_cancel_sign) -> {
        dialog.dismiss()
    })

    resetBtn.setOnClickListener((v_reset_sign) -> {
        cmdEdit.setText(DEFAULT_SIGN_COMMAND)
        withAppIdSwitch.setChecked(true)
        randomAppIdSwitch.setChecked(true)
        selectAppIdRow.setVisibility(android.view.View.GONE);
        selectLabel.setText("默认AppID: 未选择");
        selectLabel.setTag(null);
        selectedAppIdRef[0] = null;
        selectedAppIdRef[1] = null;
        // 重置AppID行
        appIdContainer.removeAllViews()
        for (int ri = 0; ri < SIGN_PRESET_APP_IDS.length; ri++) {
            addSignAppIdRow(appIdContainer, SIGN_PRESET_APP_IDS[ri], "", ctx, editBgHex, inputTextColor);
        }
        previewImg.setImageBitmap(null)
        previewPathHolder[0] = null;
        toast("已重置")
    })

    addBtn.setOnClickListener((v_add_sign) -> {
        addSignAppIdRow(appIdContainer, "", "", ctx, editBgHex, inputTextColor);
        saveSignAppIds(appIdContainer);
    })

    // dialog 关闭时自动保存AppID
    dialog.setOnDismissListener((v_dismiss) -> {
        saveSignAppIds(appIdContainer);
        Object[] st = (Object[]) selectLabel.getTag();
        if (st != null) {
            putString("signSelectedAppId", (String) st[0]);
            putString("signSelectedAppName", (String) st[1]);
        } else if (selectedAppIdRef[0] != null && !selectedAppIdRef[0].isEmpty()) {
            putString("signSelectedAppId", selectedAppIdRef[0]);
            putString("signSelectedAppName", selectedAppIdRef[1]);
        }
    })
}

// ====== 合成表情设置面板 ======
void openEmojiPanel(String talker) {
    log("[Emoji] openEmojiPanel 开始执行");
    var ctx = getTopActivity()
    if (ctx == null) { log("[Emoji] ctx为null"); return; }
    boolean dark = isDarkMode(ctx);
    
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");
    
    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";
    
    var dm = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(dm)
    int screenHeight = dm.heightPixels
    int screenWidth = dm.widthPixels
    
    String savedCmd = getString("emojiCommand", DEFAULT_EMOJI_COMMAND);
    
    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1))
    mainContainer.setBackgroundColor(bgColor)
    
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)
    var titleText = new TextView(ctx)
    titleText.setText("合成表情")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f))
    titleLayout.addView(titleText)
    mainContainer.addView(titleLayout)
    
    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(-1, 1))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)
    
    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(-1, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)
    
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)
    
    // ==== 卡片1: 指令配置 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "指令配置", titleColor, accentBlue));
    var cmdLayout = createInputRow(ctx, "触发指令", savedCmd, labelColor, inputTextColor, editBgHex, dark);
    var cmdEdit = (EditText) cmdLayout.getTag();
    cmdEdit.setHint("输入 /合成 触发");
    card1.addView(cmdLayout);
    mainLayout.addView(card1);
    
    // ==== 卡片2: 预览 ====
    String efTk = (talker != null && !talker.isEmpty()) ? talker : "";
    var cardPrev = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    cardPrev.addView(createSection(ctx, "预览", titleColor, accentBlue));
    
    var inRow1 = createInputRow(ctx, "表情1", "", labelColor, inputTextColor, editBgHex, dark);
    var emoji1Edit = (EditText) inRow1.getTag();
    emoji1Edit.setHint("例如 😂");
    cardPrev.addView(inRow1);
    cardPrev.addView(createDivider(ctx, dividerColor));
    var inRow2 = createInputRow(ctx, "表情2", "", labelColor, inputTextColor, editBgHex, dark);
    var emoji2Edit = (EditText) inRow2.getTag();
    emoji2Edit.setHint("例如 😍");
    cardPrev.addView(inRow2);
    
    // 输入框与按钮间距
    var spacer = new View(ctx)
    spacer.setLayoutParams(new LinearLayout.LayoutParams(-1, dpToPx(ctx, 10)))
    cardPrev.addView(spacer)
    
    var btnRow = new LinearLayout(ctx)
    btnRow.setOrientation(LinearLayout.HORIZONTAL)
    btnRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    
    var prevBtn = new Button(ctx)
    prevBtn.setText("预览")
    prevBtn.setTextSize(14)
    prevBtn.setTextColor(android.graphics.Color.WHITE)
    prevBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    prevBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    prevBtn.setMinHeight(0)
    prevBtn.setMinimumHeight(0)
    var pvBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 1.0f)
    pvBP.setMargins(0, 0, dpToPx(ctx, 4), 0)
    prevBtn.setLayoutParams(pvBP)
    btnRow.addView(prevBtn)
    
    var emojiSendBtn = new Button(ctx)
    emojiSendBtn.setText("发送")
    emojiSendBtn.setTextSize(14)
    emojiSendBtn.setTextColor(android.graphics.Color.WHITE)
    emojiSendBtn.setPadding(dpToPx(ctx, 10), 0, dpToPx(ctx, 10), 0)
    emojiSendBtn.setBackground(createRoundedDrawable(accentOrange, 6))
    emojiSendBtn.setMinHeight(0)
    emojiSendBtn.setMinimumHeight(0)
    var esBP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 1.0f)
    esBP.setMargins(dpToPx(ctx, 4), 0, 0, 0)
    emojiSendBtn.setLayoutParams(esBP)
    btnRow.addView(emojiSendBtn)
    
    cardPrev.addView(btnRow)
    
    var pvImg = new ImageView(ctx)
    pvImg.setBackgroundColor(dark ? android.graphics.Color.parseColor("#333333") : android.graphics.Color.parseColor("#ECF0F1"));
    var pvIP = new LinearLayout.LayoutParams(-1, dpToPx(ctx, 200))
    pvIP.setMargins(0, dpToPx(ctx, 8), 0, 0)
    pvImg.setLayoutParams(pvIP)
    pvImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
    pvImg.setClickable(true);
    cardPrev.addView(pvImg);
    mainLayout.addView(cardPrev);
    
    String[] ePreviewPath = new String[1];
    String emojiCachePath = cacheDir + EMOJI_FILE_PREFIX + "_preview.png";
    try {
        var cachedBmp = android.graphics.BitmapFactory.decodeFile(emojiCachePath);
        if (cachedBmp != null) { pvImg.setImageBitmap(cachedBmp); ePreviewPath[0] = emojiCachePath; }
    } catch (Exception ex) {}
    pvImg.setOnClickListener((v_e_full) -> {
        if (ePreviewPath[0] != null) openFullscreenImage(ctx, ePreviewPath[0], "emoji_preview.png");
    })
    
    prevBtn.setOnClickListener((v_e_prev) -> {
        String e1 = emoji1Edit.getText().toString().trim();
        String e2 = emoji2Edit.getText().toString().trim();
        if (e1.isEmpty() || e2.isEmpty()) { toast("请输入两个表情"); return; }
        toast("正在合成表情...")
        try {
            String enc1 = java.net.URLEncoder.encode(e1, "UTF-8");
            String enc2 = java.net.URLEncoder.encode(e2, "UTF-8");
            String api = EMOJI_API_URL + enc1 + "&emoji2=" + enc2;
            ePreviewPath[0] = emojiCachePath;
            get(api, null, respContent -> {
                try {
                    var jsonObj = new org.json.JSONObject(respContent.toString());
                    if (jsonObj.optInt("code") == 1) {
                        var data = jsonObj.optJSONObject("data");
                        String url = data.optString("url");
                        if (url != null && !url.isEmpty()) {
                            download(url, emojiCachePath, null, pf -> {
                                try {
                                    var bmp = android.graphics.BitmapFactory.decodeFile(pf.getAbsolutePath());
                                    if (bmp != null) {
                                        pvImg.post(new Runnable() { public void run() { pvImg.setImageBitmap(bmp); } });
                                    }
                                } catch (Exception e2) { log("[Emoji] preview decode: " + e2.toString()); }
                            });
                        } else { toast("未获取到图片URL"); }
                    } else { toast("合成失败: " + jsonObj.optString("message", "")); }
                } catch (Exception e1x) { toast("预览失败: " + e1x.getMessage()); }
            });
        } catch (Exception e0) { toast("预览失败: " + e0.getMessage()); }
    })
    
    emojiSendBtn.setOnClickListener((v_e_send) -> {
        if (efTk.isEmpty()) { toast("无法发送：未指定对话"); return; }
        String pfp = ePreviewPath[0];
        if (pfp == null || pfp.isEmpty()) { toast("请先预览图片"); return; }
        try {
            sendEmoji(efTk, pfp);
            toast("已发送");
        } catch (Exception e) { toast("发送失败: " + e.getMessage()); }
    })
    
    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)
    
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(-1, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)
    
    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -2))
    
    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setGravity(android.view.Gravity.CENTER)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var rzP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    rzP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(rzP)
    buttonLayout.addView(resetBtn)
    
    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setGravity(android.view.Gravity.CENTER)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var ccP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    ccP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(ccP)
    buttonLayout.addView(cancelBtn)
    
    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setGravity(android.view.Gravity.CENTER)
    saveBtn.setBackground(createRoundedDrawable(accentBlue, 6))
    var svP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    svP.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(svP)
    buttonLayout.addView(saveBtn)
    
    mainContainer.addView(buttonLayout)
    
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.72))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    saveBtn.setOnClickListener((v_e_save) -> {
        try {
            String cmd = cmdEdit.getText().toString().trim()
            if (cmd.isEmpty()) { toast("请输入指令"); return; }
            putString("emojiCommand", cmd)
            toast("保存成功")
            dialog.dismiss()
        } catch (Exception e) { toast("保存失败: " + e.getMessage()) }
    })
    
    cancelBtn.setOnClickListener((v_e_cancel) -> dialog.dismiss())
    
    resetBtn.setOnClickListener((v_e_reset) -> {
        cmdEdit.setText(DEFAULT_EMOJI_COMMAND)
        emoji1Edit.setText(""); emoji2Edit.setText("")
        pvImg.setImageBitmap(null)
        toast("已重置")
    })
}

// ====== 打开设置面板 (二级弹窗) ======
void openSettingsPanel(){
    var ctx = getTopActivity()
    boolean dark = isDarkMode(ctx);
    
    // 定义颜色（深色/浅色）
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");
    
    // 获取屏幕尺寸
    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenHeight = displayMetrics.heightPixels
    int screenWidth = displayMetrics.widthPixels
    
    // 创建主布局 - 使用垂直布局
    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    ))
    mainContainer.setBackgroundColor(bgColor)
    
    // ====== 标题区域 ======
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 16), dpToPx(ctx, 12))
    titleLayout.setBackgroundColor(bgColor)
    
    var titleText = new TextView(ctx)
    titleText.setText("TTS 语音合成")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(18)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    titleLayout.addView(titleText)
    
    mainContainer.addView(titleLayout)
    
    // 标题分割线
    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)
    
    // ====== 滚动内容区域 ======
    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setBackgroundColor(contentBgColor)
    
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(dpToPx(ctx, 14), dpToPx(ctx, 10), dpToPx(ctx, 14), dpToPx(ctx, 10))
    mainLayout.setBackgroundColor(contentBgColor)
    mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    int cardBgColor = dark ? android.graphics.Color.parseColor("#2A2A2A") : android.graphics.Color.WHITE;
    String accentBlue = dark ? "#5DADE2" : "#3498DB";
    String accentGreen = dark ? "#58D68D" : "#27AE60";
    String accentOrange = dark ? "#F4D03F" : "#F39C12";
    String accentPurple = dark ? "#BB8FCE" : "#8E44AD";
    String accentRed = dark ? "#EC7063" : "#E74C3C";
    
    boolean savedTtsEnabled = getBoolean("ttsEnabled", true);
    boolean savedTtsManual = getBoolean("ttsManual", true);
    boolean savedAutoPlay = getBoolean("autoPlay", true);
    boolean savedPrivateChat = getBoolean("privateChat", true);
    boolean savedGroupChat = getBoolean("groupChat", false);
    boolean savedReadSelf = getBoolean("readSelf", false);
    boolean savedReadSpeakerName = getBoolean("readSpeakerName", true);
    String savedSpeed = getString("speed", "1.0");
    String savedPitch = getString("pitch", "1.0");
    String savedOpenCmd = getString("openCommand", DEFAULT_OPEN_COMMAND);
    String savedSendCmd = getString("sendCommand", DEFAULT_SEND_COMMAND);
    boolean savedDndEnabled = getBoolean("dndEnabled", false);
    String savedDndStartTime = getString("dndStartTime", "");
    String savedDndEndTime = getString("dndEndTime", "");
    String savedKeywordFilter = getString("keywordFilter", "");
    
    String editBgHex = dark ? "#3A3A3A" : "#F0F2F5";
    
    // ==== 卡片1: TTS总开关 ====
    var card1 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card1.addView(createSection(ctx, "TTS 总开关", titleColor, accentBlue));
    var masterSwitchRow = createSwitchRow(ctx, "TTS总开关", "启用或禁用所有TTS功能", savedTtsEnabled, dark);
    var masterSwitch = (Switch) masterSwitchRow.getTag();
    card1.addView(masterSwitchRow);
    mainLayout.addView(card1);
    
    // ==== 卡片2: 语音配置 ====
    var card2 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card2.addView(createSection(ctx, "语音配置", titleColor, accentGreen));
    var speedLayout = createInputRow(ctx, "语速", savedSpeed, labelColor, inputTextColor, editBgHex, dark);
    var speedEdit = (EditText) speedLayout.getTag();
    card2.addView(speedLayout);
    card2.addView(createDivider(ctx, dividerColor));
    var pitchLayout = createInputRow(ctx, "音调", savedPitch, labelColor, inputTextColor, editBgHex, dark);
    var pitchEdit = (EditText) pitchLayout.getTag();
    card2.addView(pitchLayout);
    mainLayout.addView(card2);
    
    // ==== 卡片3: 功能开关 ====
    var card3 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card3.addView(createSection(ctx, "功能开关", titleColor, accentOrange));
    var ttsManualRow = createSwitchRow(ctx, "文字转语音", "手动输入命令进行语音合成发送", savedTtsManual, dark);
    var ttsManualSwitch = (Switch) ttsManualRow.getTag();
    card3.addView(ttsManualRow);
    card3.addView(createDivider(ctx, dividerColor));
    var autoPlayRow = createSwitchRow(ctx, "自动朗读", "收到消息时自动朗读", savedAutoPlay, dark);
    var autoPlaySwitch = (Switch) autoPlayRow.getTag();
    card3.addView(autoPlayRow);
    card3.addView(createDivider(ctx, dividerColor));
    var readSpeakerRow = createSwitchRow(ctx, "朗读发言人", "朗读时读出【XXX说】前缀", savedReadSpeakerName, dark);
    var readSpeakerSwitch = (Switch) readSpeakerRow.getTag();
    card3.addView(readSpeakerRow);
    card3.addView(createDivider(ctx, dividerColor));
    var readSelfRow = createSwitchRow(ctx, "朗读自己", "朗读自己发送的消息", savedReadSelf, dark);
    var readSelfSwitch = (Switch) readSelfRow.getTag();
    card3.addView(readSelfRow);
    card3.addView(createDivider(ctx, dividerColor));
    var privateRow = createSwitchRow(ctx, "私聊朗读", "在私聊中启用TTS", savedPrivateChat, dark);
    var privateSwitch = (Switch) privateRow.getTag();
    card3.addView(privateRow);
    card3.addView(createDivider(ctx, dividerColor));
    var groupRow = createSwitchRow(ctx, "群聊朗读", "在群聊中启用TTS", savedGroupChat, dark);
    var groupSwitch = (Switch) groupRow.getTag();
    card3.addView(groupRow);
    mainLayout.addView(card3);
    
    // ==== 卡片3.5: 免打扰 ====
    var cardDnd = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    cardDnd.addView(createSection(ctx, "免打扰", titleColor, accentRed));
    var dndSwitchRow = createSwitchRow(ctx, "免打扰模式", "开启后在指定时间段内不自动朗读", savedDndEnabled, dark);
    var dndSwitch = (Switch) dndSwitchRow.getTag();
    cardDnd.addView(dndSwitchRow);
    cardDnd.addView(createDivider(ctx, dividerColor));
    var dndTimeRow = new LinearLayout(ctx)
    dndTimeRow.setOrientation(LinearLayout.HORIZONTAL)
    dndTimeRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    dndTimeRow.setPadding(0, dpToPx(ctx, 8), 0, dpToPx(ctx, 8))
    
    var dndTimeLabel = new TextView(ctx)
    dndTimeLabel.setText("免打扰时间")
    dndTimeLabel.setTextColor(labelColor)
    dndTimeLabel.setTextSize(14)
    dndTimeLabel.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL)
    dndTimeLabel.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(ctx, 100), dpToPx(ctx, 36)))
    dndTimeRow.addView(dndTimeLabel)
    
    var dndStartEdit = new EditText(ctx)
    dndStartEdit.setText(savedDndStartTime.isEmpty() ? "22:00" : savedDndStartTime)
    dndStartEdit.setTextColor(inputTextColor)
    dndStartEdit.setHint("22:00")
    dndStartEdit.setBackgroundColor(android.graphics.Color.parseColor(editBgHex))
    dndStartEdit.setTextSize(14)
    dndStartEdit.setGravity(android.view.Gravity.CENTER)
    dndStartEdit.setSingleLine(true)
    var dseP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 1.0f)
    dseP.setMargins(0, 0, dpToPx(ctx, 2), 0)
    dndStartEdit.setLayoutParams(dseP)
    dndTimeRow.addView(dndStartEdit)
    
    var dndEndEdit = new EditText(ctx)
    dndEndEdit.setText(savedDndEndTime.isEmpty() ? "08:00" : savedDndEndTime)
    dndEndEdit.setTextColor(inputTextColor)
    dndEndEdit.setHint("08:00")
    dndEndEdit.setBackgroundColor(android.graphics.Color.parseColor(editBgHex))
    dndEndEdit.setTextSize(14)
    dndEndEdit.setGravity(android.view.Gravity.CENTER)
    dndEndEdit.setSingleLine(true)
    var deeP = new LinearLayout.LayoutParams(0, dpToPx(ctx, 36), 1.0f)
    deeP.setMargins(dpToPx(ctx, 2), 0, 0, 0)
    dndEndEdit.setLayoutParams(deeP)
    dndTimeRow.addView(dndEndEdit)
    
    cardDnd.addView(dndTimeRow);
    var keywordLayout = createInputRow(ctx, "关键词过滤", savedKeywordFilter, labelColor, inputTextColor, editBgHex, dark);
    var keywordEdit = (EditText) keywordLayout.getTag();
    keywordEdit.setHint("用|分隔, 如: 广告|红包|福利");
    cardDnd.addView(keywordLayout);
    mainLayout.addView(cardDnd);
    
    var card4 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card4.addView(createSection(ctx, "命令配置", titleColor, accentPurple));
    var openCmdLayout = createInputRow(ctx, "打开面板", savedOpenCmd, labelColor, inputTextColor, editBgHex, dark);
    var openCmdEdit = (EditText) openCmdLayout.getTag();
    card4.addView(openCmdLayout);
    card4.addView(createDivider(ctx, dividerColor));
    var sendCmdLayout = createInputRow(ctx, "TTS发送", savedSendCmd, labelColor, inputTextColor, editBgHex, dark);
    var sendCmdEdit = (EditText) sendCmdLayout.getTag();
    card4.addView(sendCmdLayout);
    mainLayout.addView(card4);
    
    // ==== 卡片5: 白名单/黑名单 ====
    var card5 = createCard(ctx, cardBgColor, 12, dpToPx(ctx, 14), dpToPx(ctx, 10));
    card5.addView(createSection(ctx, "白名单/黑名单", titleColor, accentBlue));
    
    var tipView = new TextView(ctx);
    tipView.setText("白名单和黑名单不能同时生效，白名单优先级更高");
    tipView.setTextColor(descColor);
    tipView.setTextSize(11);
    tipView.setPadding(0, 0, 0, dpToPx(ctx, 6));
    card5.addView(tipView);
    
    Map<String, TextView> titleViewMap = new HashMap();
    
    var privateWhiteRow = createListSelectorRow(ctx, "私聊白名单", "仅朗读白名单中的好友", "privateWhiteList", dark, titleViewMap);
    card5.addView(privateWhiteRow);
    card5.addView(createDivider(ctx, dividerColor));
    
    var privateBlackRow = createListSelectorRow(ctx, "私聊黑名单", "不朗读黑名单中的好友", "privateBlackList", dark, titleViewMap);
    card5.addView(privateBlackRow);
    card5.addView(createDivider(ctx, dividerColor));
    
    var groupWhiteRow = createListSelectorRow(ctx, "群聊白名单", "仅朗读白名单中的群聊", "groupWhiteList", dark, titleViewMap);
    card5.addView(groupWhiteRow);
    card5.addView(createDivider(ctx, dividerColor));
    
    var groupBlackRow = createListSelectorRow(ctx, "群聊黑名单", "不朗读黑名单中的群聊", "groupBlackList", dark, titleViewMap);
    card5.addView(groupBlackRow);
    mainLayout.addView(card5);
    
    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)
    
    // ====== 底部按钮区域 ======
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)
    
    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(dpToPx(ctx, 16), dpToPx(ctx, 14), dpToPx(ctx, 16), dpToPx(ctx, 14))
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    var resetBtn = new Button(ctx)
    resetBtn.setText("重置")
    resetBtn.setTextSize(14)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(0, 0, 0, 0)
    resetBtn.setBackground(createRoundedDrawable("#95A5A6", 6))
    var resetParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    resetParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    resetBtn.setLayoutParams(resetParams)
    buttonLayout.addView(resetBtn)
    
    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(14)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(0, 0, 0, 0)
    cancelBtn.setBackground(createRoundedDrawable("#7F8C8D", 6))
    var cancelParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    cancelParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    cancelBtn.setLayoutParams(cancelParams)
    buttonLayout.addView(cancelBtn)
    
    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(14)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(0, 0, 0, 0)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 6))
    var saveParams = new LinearLayout.LayoutParams(0, dpToPx(ctx, 40), 0.33f)
    saveParams.setMargins(dpToPx(ctx, 4), 0, dpToPx(ctx, 4), 0)
    saveBtn.setLayoutParams(saveParams)
    buttonLayout.addView(saveBtn)
    
    mainContainer.addView(buttonLayout)
    
    // 构建对话框
    var dialog = new android.app.Dialog(ctx)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(mainContainer)
    dialog.setCancelable(true);
    dialog.setCanceledOnTouchOutside(true);
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.78))
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    dialog.show()
    
    // ====== 按钮点击事件 ======
    saveBtn.setOnClickListener((v_save_tts) -> {
        try {
            String speed = speedEdit.getText().toString().trim()
            String pitch = pitchEdit.getText().toString().trim()
            String openCmd = openCmdEdit.getText().toString().trim()
            String sendCmd = sendCmdEdit.getText().toString().trim()
            String dndStart = dndStartEdit.getText().toString().trim()
            String dndEnd = dndEndEdit.getText().toString().trim()
            String keywordFilter = keywordEdit.getText().toString().trim()
            
            if (speed.isEmpty()) {
                toast("请输入语速")
                return
            }
            if (pitch.isEmpty()) {
                toast("请输入音调")
                return
            }
            if (openCmd.isEmpty()) {
                toast("请输入打开面板命令")
                return
            }
            if (sendCmd.isEmpty()) {
                toast("请输入TTS发送命令")
                return
            }
            
            putBoolean("ttsEnabled", masterSwitch.isChecked())
            putBoolean("ttsManual", ttsManualSwitch.isChecked())
            putString("speed", speed)
            putString("pitch", pitch)
            putString("openCommand", openCmd)
            putString("sendCommand", sendCmd)
            putBoolean("autoPlay", autoPlaySwitch.isChecked())
            putBoolean("readSelf", readSelfSwitch.isChecked())
            putBoolean("readSpeakerName", readSpeakerSwitch.isChecked())
            putBoolean("privateChat", privateSwitch.isChecked())
            putBoolean("groupChat", groupSwitch.isChecked())
            putBoolean("dndEnabled", dndSwitch.isChecked())
            putString("dndStartTime", dndStart)
            putString("dndEndTime", dndEnd)
            putString("keywordFilter", keywordFilter)
            
            toast("✅ 保存成功")
            log("[TTS]配置已更新")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[TTS]保存配置异常：" + e.toString())
        }
    })
    
    cancelBtn.setOnClickListener((v_cancel_tts) -> {
        dialog.dismiss()
    })
    
    reloadBtn.setOnClickListener((v_reload_tts) -> {
        dialog.dismiss()
        toast("正在重载插件...")
        reloadPlugin()
    })
    
    resetBtn.setOnClickListener((v_reset_tts) -> {
        sendCmdEdit.setText(DEFAULT_SEND_COMMAND)
        readSelfSwitch.setChecked(false)
        readSpeakerSwitch.setChecked(true)
        privateSwitch.setChecked(true)
        groupSwitch.setChecked(false)
        dndSwitch.setChecked(false)
        dndStartEdit.setText("")
        dndEndEdit.setText("")
        keywordEdit.setText("")
        putString("privateWhiteList", "")
        putString("privateBlackList", "")
        putString("groupWhiteList", "")
        putString("groupBlackList", "")
        // 更新显示
        updateSelectedDisplay("privateWhiteList", 
            (TextView) privateWhiteRow.getTag(), ctx)
        updateSelectedDisplay("privateBlackList",
            (TextView) privateBlackRow.getTag(), ctx)
        updateSelectedDisplay("groupWhiteList",
            (TextView) groupWhiteRow.getTag(), ctx)
        updateSelectedDisplay("groupBlackList",
            (TextView) groupBlackRow.getTag(), ctx)
        // 刷新所有生效状态
        refreshAllEffectiveStatus(titleViewMap);
        toast("已重置为默认配置")
    })
}

// ====== 创建圆角背景方法 ======
android.graphics.drawable.GradientDrawable createRoundedDrawable(String colorHex, int radius) {
    var drawable = new android.graphics.drawable.GradientDrawable()
    drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE)
    drawable.setCornerRadius(radius)
    drawable.setColor(android.graphics.Color.parseColor(colorHex))
    return drawable
}

// ====== 创建卡片容器 ======
LinearLayout createCard(Context ctx, int bgColor, int radius, int padH, int padV) {
    var card = new LinearLayout(ctx)
    card.setOrientation(LinearLayout.VERTICAL)
    card.setPadding(padH, padV, padH, padV)
    var drawable = new android.graphics.drawable.GradientDrawable()
    drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE)
    drawable.setCornerRadius(radius)
    drawable.setColor(bgColor)
    card.setBackground(drawable)
    var cardParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    cardParams.setMargins(0, 0, 0, dpToPx(ctx, 5))
    card.setLayoutParams(cardParams)
    return card
}

// ====== 创建分割线 ======
View createDivider(Context ctx, int color) {
    var divider = new View(ctx)
    var divParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1)
    divParams.setMargins(0, 4, 0, 4)
    divider.setLayoutParams(divParams)
    divider.setBackgroundColor(color)
    return divider
}

// ====== 创建分组标题（带左侧色条） ======
LinearLayout createSection(Context ctx, String title, int titleColor, String accentColorHex) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setPadding(0, dpToPx(ctx, 4), 0, dpToPx(ctx, 2))
    layout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    
    var bar = new View(ctx)
    var barParams = new LinearLayout.LayoutParams(dpToPx(ctx, 3), dpToPx(ctx, 16))
    barParams.setMargins(0, 0, dpToPx(ctx, 8), 0)
    bar.setLayoutParams(barParams)
    bar.setBackground(createRoundedDrawable(accentColorHex, 2))
    layout.addView(bar)
    
    var textView = new TextView(ctx)
    textView.setText(title)
    textView.setTextColor(titleColor)
    textView.setTextSize(15)
    textView.setTypeface(null, android.graphics.Typeface.BOLD)
    layout.addView(textView)
    
    return layout
}

int dpToPx(Context ctx, int dp) {
    return (int)(dp * ctx.getResources().getDisplayMetrics().density + 0.5f)
}

// ====== 创建输入行 ======
LinearLayout createInputRow(Context ctx, String label, String defaultValue, int labelColor, int textColor, String editBgHex, boolean dark) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    layout.setPadding(0, dpToPx(ctx, 2), 0, dpToPx(ctx, 2))
    
    var labelView = new TextView(ctx)
    labelView.setText(label)
    labelView.setTextColor(labelColor)
    labelView.setTextSize(14)
    labelView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(ctx, 100), 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
    layout.addView(labelView)
    
    var editText = new EditText(ctx)
    editText.setText(defaultValue)
    editText.setTextColor(textColor)
    editText.setTextSize(14)
    editText.setBackground(createRoundedDrawable(editBgHex, 6))
    editText.setPadding(dpToPx(ctx, 10), dpToPx(ctx, 6), dpToPx(ctx, 10), dpToPx(ctx, 6))
    editText.setSingleLine(true)
    editText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        dpToPx(ctx, 36), 1.0f))
    if (dark) {
        editText.setHintTextColor(android.graphics.Color.parseColor("#666666"))
    } else {
        editText.setHintTextColor(android.graphics.Color.GRAY)
    }
    layout.addView(editText)
    
    layout.setTag(editText)
    return layout
}

// ====== 创建开关行 ======
LinearLayout createSwitchRow(Context ctx, String title, String desc, boolean defaultChecked, boolean dark) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    layout.setPadding(0, 4, 0, 4)
    
    var textLayout = new LinearLayout(ctx)
    textLayout.setOrientation(LinearLayout.VERTICAL)
    textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    
    var titleView = new TextView(ctx)
    titleView.setText(title)
    titleView.setTextColor(dark ? android.graphics.Color.parseColor("#E0E0E0") : android.graphics.Color.parseColor("#2C3E50"))
    titleView.setTextSize(14)
    textLayout.addView(titleView)
    
    var descView = new TextView(ctx)
    descView.setText(desc)
    descView.setTextColor(dark ? android.graphics.Color.parseColor("#777777") : android.graphics.Color.parseColor("#7F8C8D"))
    descView.setTextSize(11)
    descView.setPadding(0, 2, 0, 0)
    textLayout.addView(descView)
    
    layout.addView(textLayout)
    
    var switchBtn = new Switch(ctx)
    switchBtn.setChecked(defaultChecked)
    switchBtn.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    layout.addView(switchBtn)
    
    layout.setTag(switchBtn)
    return layout
}

// ====== 创建列表选择器行 ======
LinearLayout createListSelectorRow(Context ctx, String title, String desc, String key, boolean dark, Map<String, TextView> titleViewMap) {
    var outerLayout = new LinearLayout(ctx);
    outerLayout.setOrientation(LinearLayout.VERTICAL);
    outerLayout.setPadding(0, 3, 0, 3);
    
    var actionRow = new LinearLayout(ctx)
    actionRow.setOrientation(LinearLayout.HORIZONTAL)
    actionRow.setGravity(android.view.Gravity.CENTER_VERTICAL)
    
    var listTitle = new TextView(ctx);
    listTitle.setText(title);
    listTitle.setTextColor(dark ? android.graphics.Color.parseColor("#E0E0E0") : android.graphics.Color.parseColor("#2C3E50"));
    listTitle.setTextSize(14);
    listTitle.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f))
    actionRow.addView(listTitle);
    
    var selectBtn = new Button(ctx);
    selectBtn.setText("选择");
    selectBtn.setTextSize(13);
    selectBtn.setTextColor(android.graphics.Color.WHITE);
    selectBtn.setPadding(dpToPx(ctx, 16), 0, dpToPx(ctx, 16), 0);
    selectBtn.setBackground(createRoundedDrawable("#3498DB", 6));
    selectBtn.setMinHeight(0);
    selectBtn.setMinimumHeight(0);
    var selParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(ctx, 32));
    selParams.setMargins(0, 0, 8, 0);
    selectBtn.setLayoutParams(selParams);
    actionRow.addView(selectBtn);
    
    var clearBtn = new Button(ctx);
    clearBtn.setText("清空");
    clearBtn.setTextSize(13);
    clearBtn.setTextColor(android.graphics.Color.WHITE);
    clearBtn.setPadding(dpToPx(ctx, 16), 0, dpToPx(ctx, 16), 0);
    clearBtn.setBackground(createRoundedDrawable("#E74C3C", 6));
    clearBtn.setMinHeight(0);
    clearBtn.setMinimumHeight(0);
    var clrParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(ctx, 32));
    clearBtn.setLayoutParams(clrParams);
    actionRow.addView(clearBtn);
    
    outerLayout.addView(actionRow);
    
    var descView = new TextView(ctx);
    descView.setText(desc);
    descView.setTextColor(dark ? android.graphics.Color.parseColor("#777777") : android.graphics.Color.parseColor("#7F8C8D"));
    descView.setTextSize(11);
    descView.setPadding(0, 2, 0, 0);
    outerLayout.addView(descView);
    
    var selectedView = new TextView(ctx);
    selectedView.setText("已选：无");
    selectedView.setTextColor(dark ? android.graphics.Color.parseColor("#CCCCCC") : android.graphics.Color.parseColor("#34495E"));
    selectedView.setTextSize(12);
    selectedView.setPadding(0, 4, 0, 2);
    outerLayout.addView(selectedView);
    
    outerLayout.setTag(selectedView);
    
    if (titleViewMap != null) {
        titleViewMap.put(key, listTitle);
    }

    updateEffectiveStatus(listTitle, key);

    actionRow.setOnClickListener((v_row) -> {
        showListSelector(ctx, key, selectedView, listTitle, titleViewMap);
    });
    
    clearBtn.setOnClickListener((v_clear) -> {
        putString(key, "");
        selectedView.setText("已选：无");
        toast("已清空");
        refreshAllEffectiveStatus(titleViewMap);
    });
    
    return outerLayout;
}

void updateEffectiveStatus(TextView tv, String key) {
    boolean isWhite = key.endsWith("WhiteList");
    boolean isPrivate = key.startsWith("private");
    String whiteKey = (isPrivate ? "private" : "group") + "WhiteList";
    String blackKey = (isPrivate ? "private" : "group") + "BlackList";
    String white = getString(whiteKey, "");
    String black = getString(blackKey, "");
    
    boolean effective;
    if (isWhite) {
        effective = !white.isEmpty();
    } else {
        effective = white.isEmpty() && !black.isEmpty();
    }
    
    String baseTitle = isPrivate ? (isWhite ? "私聊白名单" : "私聊黑名单") : (isWhite ? "群聊白名单" : "群聊黑名单");
    tv.setText(effective ? baseTitle + "（生效）" : baseTitle);
}

void refreshAllEffectiveStatus(Map<String, TextView> titleViewMap) {
    if (titleViewMap == null) return;
    for (Map.Entry<String, TextView> entry : titleViewMap.entrySet()) {
        String key = entry.getKey();
        TextView tv = entry.getValue();
        updateEffectiveStatus(tv, key);
    }
}

// ====== 显示列表选择器 ======
void showListSelector(Context ctx, String key, TextView sv, TextView tv, Map<String, TextView> titleViewMap) {
    try {
        boolean isPrivate = key.startsWith("private");
        boolean dark = isDarkMode(ctx);
        
        int bgColor = dark ? Color.parseColor("#1E1E1E") : Color.WHITE;
        int contentBgColor = dark ? Color.parseColor("#2C2C2C") : Color.parseColor("#F5F7FA");
        int titleColor = dark ? Color.WHITE : Color.parseColor("#2C3E50");
        int textColor = dark ? Color.WHITE : Color.BLACK;
        int dividerColor = dark ? Color.parseColor("#444444") : Color.parseColor("#E8ECEF");
        int hintColor = dark ? Color.parseColor("#888888") : Color.GRAY;
        
        // ---- 收集数据：显示文本 = 名称 + (ID) ----
        List<String> displayItems = new ArrayList();
        List<String> pureNames = new ArrayList();
        List<String> itemIds = new ArrayList();
        List<Object> itemContacts = new ArrayList();
        
        if (isPrivate) {
            var friends = getFriendList();
            if (friends != null && !friends.isEmpty()) {
                for (var friend : friends) {
                    try {
                        String name = friend.getRemark();
                        if (name == null || name.isEmpty()) name = friend.getNickname();
                        if (name == null || name.isEmpty()) name = friend.getWxid();
                        String id = friend.getWxid();
                        if (name != null && !name.isEmpty() && id != null && !id.isEmpty()) {
                            pureNames.add(name);
                            itemIds.add(id);
                            itemContacts.add(friend);
                            displayItems.add(name + " (" + id + ")");
                        }
                    } catch (Exception e) { log("[TTS]获取好友信息失败：" + e.toString()); }
                }
            }
        } else {
            var groups = getGroupList();
            if (groups != null && !groups.isEmpty()) {
                for (var group : groups) {
                    try {
                        String name = group.getRemark();
                        if (name == null || name.isEmpty()) name = group.getName();
                        if (name == null || name.isEmpty()) name = group.getRoomId();
                        String id = group.getRoomId();
                        if (name != null && !name.isEmpty() && id != null && !id.isEmpty()) {
                            pureNames.add(name);
                            itemIds.add(id);
                            itemContacts.add(group);
                            displayItems.add(name + " (" + id + ")");
                        }
                    } catch (Exception e) { log("[TTS]获取群聊信息失败：" + e.toString()); }
                }
            }
        }
        
        if (displayItems.isEmpty()) {
            toast("没有可用的" + (isPrivate ? "好友" : "群聊"));
            return;
        }
        
        String savedList = getString(key, "");
        Set<String> selectedSet = new HashSet();
        if (!savedList.isEmpty()) {
            for (String item : savedList.split(",")) {
                if (item != null && !item.trim().isEmpty()) selectedSet.add(item.trim());
            }
        }
        
        int count = displayItems.size();
        String[] displayArray = new String[count];
        String[] nameArray = new String[count];
        final String[] idArray = new String[count];
        final Object[] contactArray = new Object[count];
        for (int i = 0; i < count; i++) {
            displayArray[i] = displayItems.get(i);
            nameArray[i] = pureNames.get(i);
            idArray[i] = itemIds.get(i);
            contactArray[i] = itemContacts.get(i);
        }
        
        boolean[] checked = new boolean[count];
        for (int i = 0; i < count; i++) {
            checked[i] = selectedSet.contains(nameArray[i]);
        }
        final boolean[] finalChecked = checked;
        
        var displayMetrics = new android.util.DisplayMetrics();
        ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        
        // ---- 根布局 RelativeLayout ----
        var rootLayout = new RelativeLayout(ctx);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setBackgroundColor(contentBgColor);
        
        // ---- 标题 ----
        var titleLayout = new LinearLayout(ctx);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(30, 35, 30, 25);
        titleLayout.setBackgroundColor(bgColor);
        var titleParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        titleLayout.setLayoutParams(titleParams);
        int titleId = View.generateViewId();
        titleLayout.setId(titleId);
        
        var titleText = new TextView(ctx);
        titleText.setText("选择" + (isPrivate ? "好友" : "群聊"));
        titleText.setTextColor(titleColor);
        titleText.setTextSize(22);
        titleText.setTypeface(null, Typeface.BOLD);
        titleText.setGravity(Gravity.LEFT);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleLayout.addView(titleText);
        rootLayout.addView(titleLayout);
        
        // ---- 标题分割线 ----
        var titleDivider = new View(ctx);
        titleDivider.setBackgroundColor(dividerColor);
        var dividerParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, 2);
        dividerParams.addRule(RelativeLayout.BELOW, titleId);
        int dividerId = View.generateViewId();
        titleDivider.setId(dividerId);
        titleDivider.setLayoutParams(dividerParams);
        rootLayout.addView(titleDivider);
        
        // ---- 搜索框 ----
        var searchLayout = new LinearLayout(ctx);
        searchLayout.setOrientation(LinearLayout.HORIZONTAL);
        searchLayout.setPadding(20, 12, 20, 10);
        searchLayout.setBackgroundColor(contentBgColor);
        var searchParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        searchParams.addRule(RelativeLayout.BELOW, dividerId);
        int searchId = View.generateViewId();
        searchLayout.setId(searchId);
        searchLayout.setLayoutParams(searchParams);
        
        var searchEdit = new EditText(ctx);
        searchEdit.setHint("搜索...");
        searchEdit.setTextSize(16);
        var editBg = new android.graphics.drawable.GradientDrawable();
        editBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        editBg.setCornerRadius(8);
        editBg.setColor(dark ? Color.parseColor("#3A3A3A") : Color.WHITE);
        searchEdit.setBackground(editBg);
        searchEdit.setPadding(18, 0, 18, 0);
        searchEdit.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 0.75f));
        searchEdit.setTextColor(textColor);
        searchEdit.setHintTextColor(hintColor);
        searchLayout.addView(searchEdit);
        
        var searchBtn = new Button(ctx);
        searchBtn.setText("搜索");
        searchBtn.setTextSize(15);
        searchBtn.setTextColor(Color.WHITE);
        searchBtn.setPadding(24, 0, 24, 0);
        var btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        btnBg.setCornerRadius(8);
        btnBg.setColor(Color.parseColor("#3498DB"));
        searchBtn.setBackground(btnBg);
        searchBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 120, 0.25f));
        searchLayout.addView(searchBtn);
        rootLayout.addView(searchLayout);
        
        // ---- 全选/反选/清空 ----
        var btnLayout = new LinearLayout(ctx);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setPadding(20, 0, 20, 12);
        btnLayout.setGravity(Gravity.CENTER);
        btnLayout.setBackgroundColor(contentBgColor);
        var btnParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.addRule(RelativeLayout.BELOW, searchId);
        int btnLayoutId = View.generateViewId();
        btnLayout.setId(btnLayoutId);
        btnLayout.setLayoutParams(btnParams);
        
        var selectAllBtn = new Button(ctx);
        selectAllBtn.setText("全选");
        selectAllBtn.setTextSize(14);
        selectAllBtn.setTextColor(Color.WHITE);
        selectAllBtn.setPadding(20, 0, 20, 0);
        var allBg = new android.graphics.drawable.GradientDrawable();
        allBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        allBg.setCornerRadius(8);
        allBg.setColor(Color.parseColor("#3498DB"));
        selectAllBtn.setBackground(allBg);
        LinearLayout.LayoutParams allParams = new LinearLayout.LayoutParams(0, 120, 0.3f);
        allParams.setMargins(0, 0, 8, 0);
        selectAllBtn.setLayoutParams(allParams);
        btnLayout.addView(selectAllBtn);
        
        var invertBtn = new Button(ctx);
        invertBtn.setText("反选");
        invertBtn.setTextSize(14);
        invertBtn.setTextColor(Color.WHITE);
        invertBtn.setPadding(20, 0, 20, 0);
        var invertBg = new android.graphics.drawable.GradientDrawable();
        invertBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        invertBg.setCornerRadius(8);
        invertBg.setColor(Color.parseColor("#F39C12"));
        invertBtn.setBackground(invertBg);
        LinearLayout.LayoutParams invertParams = new LinearLayout.LayoutParams(0, 120, 0.3f);
        invertParams.setMargins(4, 0, 4, 0);
        invertBtn.setLayoutParams(invertParams);
        btnLayout.addView(invertBtn);
        
        var clearAllBtn = new Button(ctx);
        clearAllBtn.setText("清空");
        clearAllBtn.setTextSize(14);
        clearAllBtn.setTextColor(Color.WHITE);
        clearAllBtn.setPadding(20, 0, 20, 0);
        var clearBg = new android.graphics.drawable.GradientDrawable();
        clearBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        clearBg.setCornerRadius(8);
        clearBg.setColor(Color.parseColor("#E74C3C"));
        clearAllBtn.setBackground(clearBg);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, 120, 0.3f);
        clearParams.setMargins(8, 0, 0, 0);
        clearAllBtn.setLayoutParams(clearParams);
        btnLayout.addView(clearAllBtn);
        rootLayout.addView(btnLayout);
        
        // ---- 列表区域（ScrollView + 逐项构建） ----
        float density = ctx.getResources().getDisplayMetrics().density;
        int avatarDp = (int)(40 * density + 0.5f);
        
        // 构建列表项容器
        var contentLayout = new LinearLayout(ctx);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        final java.util.List<android.widget.CheckBox> allCheckBoxes = new java.util.ArrayList<>();
        final java.util.List<android.view.View> allItemRows = new java.util.ArrayList<>();
        final java.util.List<android.widget.ImageView> allAvatarViews = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            var itemRow = new LinearLayout(ctx);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding((int)(12 * density), (int)(8 * density), (int)(12 * density), (int)(8 * density));
            
            // 左侧：头像
            var avatarView = new android.widget.ImageView(ctx);
            avatarView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            var avParams = new LinearLayout.LayoutParams(avatarDp, avatarDp);
            avParams.rightMargin = (int)(12 * density);
            avParams.gravity = Gravity.CENTER_VERTICAL;
            avatarView.setLayoutParams(avParams);
            // 优先从磁盘缓存加载
            String avId = idArray[i];
            try {
                android.graphics.Bitmap cachedBm = loadCachedAvatar(avId);
                if (cachedBm != null) {
                    avatarView.setImageBitmap(getCircularBitmap(cachedBm));
                } else {
                    avatarView.setImageResource(android.R.drawable.ic_menu_help);
                }
            } catch (Exception e) {
                avatarView.setImageResource(android.R.drawable.ic_menu_help);
            }
            itemRow.addView(avatarView);
            allAvatarViews.add(avatarView);
            
            // 中间：昵称 + ID（两行）
            var textContainer = new LinearLayout(ctx);
            textContainer.setOrientation(LinearLayout.VERTICAL);
            var textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            textParams.rightMargin = (int)(8 * density);
            textContainer.setLayoutParams(textParams);
            
            var tvName = new TextView(ctx);
            tvName.setText(nameArray[i]);
            tvName.setTextSize(14);
            tvName.setTextColor(textColor);
            tvName.setSingleLine(true);
            textContainer.addView(tvName);
            
            var tvId = new TextView(ctx);
            tvId.setText(idArray[i]);
            tvId.setTextSize(11);
            tvId.setTextColor(dark ? Color.parseColor("#888888") : Color.parseColor("#95A5A6"));
            tvId.setSingleLine(true);
            tvId.setPadding(0, (int)(2 * density), 0, 0);
            textContainer.addView(tvId);
            
            itemRow.addView(textContainer);
            
            // 右侧：复选框
            var cb = new android.widget.CheckBox(ctx);
            cb.setChecked(finalChecked[i]);
            var cbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cbParams.gravity = Gravity.CENTER_VERTICAL;
            cb.setLayoutParams(cbParams);
            itemRow.addView(cb);
            allCheckBoxes.add(cb);
            
            // 点击整行切换选中
            itemRow.setClickable(true);
            itemRow.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(View v) { cb.toggle(); }
            });
            
            contentLayout.addView(itemRow);
            allItemRows.add(itemRow);
        }
        
        var scrollView = new ScrollView(ctx);
        scrollView.setBackgroundColor(dark ? Color.parseColor("#2C2C2C") : Color.WHITE);
        scrollView.addView(contentLayout);
        
        var listCard = new LinearLayout(ctx);
        listCard.setOrientation(LinearLayout.VERTICAL);
        var cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(8);
        cardBg.setColor(dark ? Color.parseColor("#2C2C2C") : Color.WHITE);
        listCard.setBackground(cardBg);
        listCard.setPadding(10, 5, 10, 5);
        listCard.addView(scrollView);
        
        var listParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, 0);
        listParams.addRule(RelativeLayout.BELOW, btnLayoutId);
        int listCardId = View.generateViewId();
        listCard.setId(listCardId);
        listCard.setLayoutParams(listParams);
        rootLayout.addView(listCard);
        
        // ---- 底部按钮 ----
        var bottomDivider = new View(ctx);
        bottomDivider.setBackgroundColor(dividerColor);
        var bottomDividerParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, 2);
        bottomDividerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        int bottomDividerId = View.generateViewId();
        bottomDivider.setId(bottomDividerId);
        bottomDivider.setLayoutParams(bottomDividerParams);
        rootLayout.addView(bottomDivider);
        
        var bottomLayout = new LinearLayout(ctx);
        bottomLayout.setOrientation(LinearLayout.HORIZONTAL);
        bottomLayout.setPadding(30, 35, 30, 35);
        bottomLayout.setGravity(Gravity.CENTER);
        bottomLayout.setBackgroundColor(bgColor);
        var bottomParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        int bottomId = View.generateViewId();
        bottomLayout.setId(bottomId);
        bottomLayout.setLayoutParams(bottomParams);
        
        var cancelBtn = new Button(ctx);
        cancelBtn.setText("取消");
        cancelBtn.setTextSize(17);
        cancelBtn.setTextColor(Color.WHITE);
        cancelBtn.setPadding(20, 0, 20, 0);
        var cancelBg = new android.graphics.drawable.GradientDrawable();
        cancelBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cancelBg.setCornerRadius(8);
        cancelBg.setColor(Color.parseColor("#95A5A6"));
        cancelBtn.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, 120, 0.3f);
        cancelParams.setMargins(0, 0, 10, 0);
        cancelBtn.setLayoutParams(cancelParams);
        bottomLayout.addView(cancelBtn);
        
        var confirmBtn = new Button(ctx);
        confirmBtn.setText("确定");
        confirmBtn.setTextSize(17);
        confirmBtn.setTextColor(Color.WHITE);
        confirmBtn.setPadding(20, 0, 20, 0);
        var confirmBg = new android.graphics.drawable.GradientDrawable();
        confirmBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        confirmBg.setCornerRadius(8);
        confirmBg.setColor(Color.parseColor("#3498DB"));
        confirmBtn.setBackground(confirmBg);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(0, 120, 0.3f);
        confirmParams.setMargins(10, 0, 0, 0);
        confirmBtn.setLayoutParams(confirmParams);
        bottomLayout.addView(confirmBtn);
        rootLayout.addView(bottomLayout);
        
        // ---- 设置 listCard 的约束 ----
        RelativeLayout.LayoutParams listParamsFinal = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            0
        );
        listParamsFinal.addRule(RelativeLayout.BELOW, btnLayoutId);
        listParamsFinal.addRule(RelativeLayout.ABOVE, bottomId);
        listCard.setLayoutParams(listParamsFinal);
        
        // ---- 对话框 ----
        var dialog = new android.app.Dialog(ctx)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(rootLayout)
        
        var window = dialog.getWindow()
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(contentBgColor))
            window.setLayout((int)(screenWidth * 0.88), (int)(screenHeight * 0.85))
            window.setGravity(Gravity.CENTER)
        }
        
        dialog.show()
        
        // 后台线程下载头像（延迟 100ms 让对话框先完成渲染）
        final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(100); } catch (Exception e) {}
                for (int i = 0; i < count; i++) {
                    try {
                        String avatarUrl = getAvatarUrl(idArray[i]);
                        if (avatarUrl == null || avatarUrl.isEmpty()) continue;
                        java.net.URL url = new java.net.URL(avatarUrl);
                        java.io.InputStream is = url.openStream();
                        android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(is);
                        is.close();
                        if (bm != null) {
                            final android.graphics.Bitmap circular = getCircularBitmap(bm);
                            saveCachedAvatar(idArray[i], circular);
                            int idx = i;
                            mainHandler.post(new Runnable() {
                                public void run() {
                                    try { allAvatarViews.get(idx).setImageBitmap(circular); } catch (Exception e) {}
                                }
                            });
                        }
                    } catch (Exception e) {
                        log("[TTS] 下载头像失败 i=" + i + ": " + e.toString());
                    }
                }
            }
        }).start();
        
        // ---- 事件绑定 ----
        confirmBtn.setOnClickListener((v_confirm) -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < allCheckBoxes.size(); i++) {
                    if (allCheckBoxes.get(i).isChecked()) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(nameArray[i]);
                    }
                }
                putString(key, sb.toString());
                updateSelectedDisplay(key, sv, ctx);
                refreshAllEffectiveStatus(titleViewMap);
                toast("保存成功");
                dialog.dismiss();
            } catch (Exception e) {
                toast("保存失败：" + e.getMessage());
                log("[TTS]保存列表异常：" + e.toString());
            }
        });
        
        cancelBtn.setOnClickListener((v_cancel_sel) -> {
            dialog.dismiss();
        });
        
        selectAllBtn.setOnClickListener((v_sel_all) -> {
            try {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(true);
                }
            } catch (Exception e) { log("[TTS]全选异常：" + e.toString()); }
        });
        
        invertBtn.setOnClickListener((v_sel_invert) -> {
            try {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(!cb.isChecked());
                }
            } catch (Exception e) { log("[TTS]反选异常：" + e.toString()); }
        });
        
        clearAllBtn.setOnClickListener((v_sel_clear) -> {
            try {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(false);
                }
            } catch (Exception e) { log("[TTS]清空异常：" + e.toString()); }
        });
        
        searchBtn.setOnClickListener((v_sel_search) -> {
            try {
                String keyword = searchEdit.getText().toString().trim().toLowerCase();
                for (int i = 0; i < count; i++) {
                    boolean match = keyword.isEmpty()
                        || nameArray[i].toLowerCase().contains(keyword)
                        || idArray[i].toLowerCase().contains(keyword);
                    allItemRows.get(i).setVisibility(match ? View.VISIBLE : View.GONE);
                }
            } catch (Exception e) { log("[TTS]搜索异常：" + e.toString()); }
        });
        
    } catch (Exception e) {
        toast("打开选择器失败：" + e.getMessage());
        log("[TTS]打开选择器异常：" + e.toString());
        e.printStackTrace();
    }
}
// ====== 更新已选显示 ======
void updateSelectedDisplay(String key, TextView sv, Context ctx) {
    try {
        String savedList = getString(key, "")
        if (savedList == null || savedList.isEmpty()) {
            sv.setText("已选：无")
            return
        }
        
        var items = savedList.split(",")
        int count = items.length
        String display = "已选：" + count + "个"
        if (count <= 3) {
            display += " (" + savedList + ")"
        } else {
            display += " (" + items[0] + ", " + items[1] + ", " + items[2] + "...)"
        }
        sv.setText(display)
    } catch (Exception e) {
        sv.setText("已选：无")
        log("[TTS]更新显示失败：" + e.toString())
    }
}

// ====== 根据名称获取ID ======
String getItemId(String key, String name) {
    try {
        boolean isPrivate = key.startsWith("private")
        
        if (isPrivate) {
            var friends = getFriendList()
            if (friends != null && !friends.isEmpty()) {
                for (var friend : friends) {
                    try {
                        String remark = friend.getRemark()
                        String nickname = friend.getNickname()
                        String wxid = friend.getWxid()
                        if (name.equals(remark) || name.equals(nickname) || name.equals(wxid)) {
                            return wxid
                        }
                    } catch (Exception e) {
                        log("[TTS]获取好友ID失败：" + e.toString())
                    }
                }
            }
        } else {
            var groups = getGroupList()
            if (groups != null && !groups.isEmpty()) {
                for (var group : groups) {
                    try {
                        String remark = group.getRemark()
                        String groupName = group.getName()
                        String roomId = group.getRoomId()
                        if (name.equals(remark) || name.equals(groupName) || name.equals(roomId)) {
                            return roomId
                        }
                    } catch (Exception e) {
                        log("[TTS]获取群聊ID失败：" + e.toString())
                    }
                }
            }
        }
    } catch (Exception e) {
        log("[TTS]获取ID失败：" + e.toString())
    }
    return null
}

// ====== 检查是否在白名单/黑名单中 ======
boolean checkInList(String talker, boolean isGroup) {
    String keyPrefix = isGroup ? "group" : "private"
    
    String talkerName = getTalkerName(talker, isGroup)
    
    String whiteKey = keyPrefix + "WhiteList"
    String whiteList = getString(whiteKey, "")
    if (whiteList != null && !whiteList.isEmpty()) {
        var items = whiteList.split(",")
        for (var item : items) {
            if (item != null) {
                String trimmed = item.trim()
                if (trimmed.equals(talkerName) || trimmed.equals(talker)) {
                    return true
                }
            }
        }
        return false
    }
    
    String blackKey = keyPrefix + "BlackList"
    String blackList = getString(blackKey, "")
    if (blackList != null && !blackList.isEmpty()) {
        var items = blackList.split(",")
        for (var item : items) {
            if (item != null) {
                String trimmed = item.trim()
                if (trimmed.equals(talkerName) || trimmed.equals(talker)) {
                    return false
                }
            }
        }
        return true
    }
    
    return true
}

// ====== 根据talker获取名称 ======
String getTalkerName(String talker, boolean isGroup) {
    try {
        if (isGroup) {
            var groups = getGroupList()
            if (groups != null && !groups.isEmpty()) {
                for (var group : groups) {
                    try {
                        String roomId = group.getRoomId()
                        if (roomId != null && roomId.equals(talker)) {
                            String name = group.getRemark()
                            if (name == null || name.isEmpty()) {
                                name = group.getName()
                            }
                            return name != null ? name : roomId
                        }
                    } catch (Exception e) {
                        log("[TTS]获取群聊名称失败：" + e.toString())
                    }
                }
            }
        } else {
            var friends = getFriendList()
            if (friends != null && !friends.isEmpty()) {
                for (var friend : friends) {
                    try {
                        String wxid = friend.getWxid()
                        if (wxid != null && wxid.equals(talker)) {
                            String name = friend.getRemark()
                            if (name == null || name.isEmpty()) {
                                name = friend.getNickname()
                            }
                            return name != null ? name : wxid
                        }
                    } catch (Exception e) {
                        log("[TTS]获取好友名称失败：" + e.toString())
                    }
                }
            }
        }
    } catch (Exception e) {
        log("[TTS]获取名称失败：" + e.toString())
    }
    return talker
}

// ====== 免打扰时间判断 ======
boolean isInDndPeriod() {
    try {
        boolean dndEnabled = getBoolean("dndEnabled", false);
        if (!dndEnabled) return false;
        
        String startTime = getString("dndStartTime", "");
        String endTime = getString("dndEndTime", "");
        if (startTime == null || startTime.isEmpty()) startTime = "22:00";
        if (endTime == null || endTime.isEmpty()) endTime = "08:00";
        
        String[] startParts = startTime.split(":");
        String[] endParts = endTime.split(":");
        if (startParts.length != 2 || endParts.length != 2) return false;
        
        int startHour = Integer.parseInt(startParts[0].trim());
        int startMin = Integer.parseInt(startParts[1].trim());
        int endHour = Integer.parseInt(endParts[0].trim());
        int endMin = Integer.parseInt(endParts[1].trim());
        
        int startMinutes = startHour * 60 + startMin;
        int endMinutes = endHour * 60 + endMin;
        
        java.util.Calendar now = java.util.Calendar.getInstance();
        int nowHour = now.get(java.util.Calendar.HOUR_OF_DAY);
        int nowMin = now.get(java.util.Calendar.MINUTE);
        int nowMinutes = nowHour * 60 + nowMin;
        
        boolean inPeriod;
        if (startMinutes <= endMinutes) {
            inPeriod = nowMinutes >= startMinutes && nowMinutes < endMinutes;
        } else {
            inPeriod = nowMinutes >= startMinutes || nowMinutes < endMinutes;
        }
        if (inPeriod) {
            log("[TTS] 免打扰命中: 当前=" + nowHour + ":" + padMin(nowMin)
                + " 范围=" + startTime + "-" + endTime);
        }
        return inPeriod;
    } catch (Exception e) {
        log("[TTS] isInDndPeriod error: " + e.toString());
        return false;
    }
}

String padMin(int m) {
    return m < 10 ? "0" + m : "" + m;
}

// ====== 关键词过滤 ======
boolean matchesKeywordFilter(String content) {
    try {
        String keywordFilter = getString("keywordFilter", "");
        if (keywordFilter == null || keywordFilter.isEmpty()) return false;
        
        String[] keywords = keywordFilter.split("\\|");
        for (int i = 0; i < keywords.length; i++) {
            String kw = keywords[i].trim();
            if (kw.isEmpty()) continue;
            if (content.contains(kw)) {
                log("[TTS] 命中关键词过滤: " + kw);
                return true;
            }
        }
        return false;
    } catch (Exception e) {
        log("[TTS] keywordFilter error: " + e.toString());
        return false;
    }
}

// ====== 消息监听 ======
void onHandleMsg(Object msgInfoBean) {
    if (msgInfoBean.isText()) {
        var content = msgInfoBean.getContent();
        var talker = msgInfoBean.getTalker();
        log("[TTS] onHandleMsg: talker=" + talker + " content=" + content.substring(0, Math.min(40, content.length())));
        
        // ====== 摸鱼日报指令 ======
        boolean moyuEnabled = getBoolean("moyuEnabled", false)
        if (moyuEnabled) {
            String moyuCmd = getString("moyuCommand", "/摸鱼日报");
            if (content.equals(moyuCmd)) {
                fetchMoyu(talker);
                return
            }
        }
        
        // ====== 小人举牌指令 ======
        boolean signEnabled = getBoolean("signEnabled", false)
        if (signEnabled) {
            String signCmd = getString("signCommand", DEFAULT_SIGN_COMMAND);
            if (content.startsWith(signCmd)) {
                String signMsg = content.substring(signCmd.length()).trim();
                if (signMsg.isEmpty()) {
                    toast("举牌失败：请输入内容（如" + signCmd + " 你好）");
                    return
                }
                fetchSign(talker, signMsg);
                return
            }
        }
        
        // ====== 合成表情指令 ======
        boolean emojiEnabled = getBoolean("emojiEnabled", false)
        if (emojiEnabled) {
            String emojiCmd = getString("emojiCommand", DEFAULT_EMOJI_COMMAND);
            if (content.startsWith(emojiCmd)) {
                String rest = content.substring(emojiCmd.length()).trim();
                if (rest.contains("+")) {
                    var parts = rest.split("\\+", 2);
                    String e1 = parts[0].trim();
                    String e2 = parts[1].trim();
                    if (!e1.isEmpty() && !e2.isEmpty()) {
                        fetchEmoji(talker, e1, e2);
                        return
                    }
                }
                toast("格式：" + emojiCmd + " 😂+😍");
                return
            }
            // 内联 + 格式
            if (content.contains("+")) {
                var parts = content.split("\\+", 2);
                String e1 = parts[0].trim();
                String e2 = parts[1].trim();
                if (!e1.isEmpty() && !e2.isEmpty() && e1.length() <= 4 && e2.length() <= 4) {
                    fetchEmoji(talker, e1, e2);
                    return
                }
            }
        }
        
        // ====== 历史今天指令 ======
        boolean historyEnabled = getBoolean("historyEnabled", false)
        if (historyEnabled) {
            String historyCmd = getString("historyCommand", "/历史今天");
            if (content.equals(historyCmd)) {
                sendHistoryToday(talker);
                return
            }
        }
        
        // ====== 每日六十秒指令 ======
        boolean sixtyEnabled = getBoolean("sixtyEnabled", false)
        if (sixtyEnabled) {
            String textCmd60 = getString("60sTextCommand", "/每日60秒");
            String imageCmd60 = getString("60sImageCommand", "/每日60秒图");
            if (content.equals(textCmd60)) {
                String token = getString("60sToken", "");
                if (token == null || token.trim().isEmpty()) {
                    toast("请先在每日六十秒设置中配置Token");
                } else {
                    send60s(talker, false);
                }
                return
            }
            if (content.equals(imageCmd60)) {
                String token = getString("60sToken", "");
                if (token == null || token.trim().isEmpty()) {
                    toast("请先在每日六十秒设置中配置Token");
                } else {
                    send60s(talker, true);
                }
                return
            }
        }
        
        // ====== TTS 消息处理 ======
        boolean ttsEnabled = getBoolean("ttsEnabled", true)
        if (!ttsEnabled) {
            log("[TTS] 跳过: ttsEnabled=false");
            return
        }
        
        boolean autoPlay = getBoolean("autoPlay", true)
        if (!autoPlay) {
            log("[TTS] 跳过: autoPlay=false");
            return
        }
        
        if (isInDndPeriod()) {
            log("[TTS] 跳过: 免打扰时段");
            return
        }
        
        if (matchesKeywordFilter(content)) {
            log("[TTS] 跳过: 命中关键词过滤, content=" + content.substring(0, Math.min(30, content.length())));
            return
        }
        
        boolean readSelf = getBoolean("readSelf", false)
        boolean isSelf = msgInfoBean.isSend()
        if (isSelf && !readSelf) {
            log("[TTS] 跳过: 不朗读自己");
            return
        }
        
        boolean isGroup = msgInfoBean.isGroupChat()
        if (isGroup && !getBoolean("groupChat", false)) {
            log("[TTS] 跳过: 群聊朗读已关闭");
            return
        }
        if (!isGroup && !getBoolean("privateChat", true)) {
            log("[TTS] 跳过: 私聊朗读已关闭");
            return
        }
        
        if (!checkInList(talker, isGroup)) {
            log("[TTS] " + talker + " 在黑名单中，跳过朗读")
            return
        }
        
        String speakerName = getTalkerName(talker, isGroup);
        String speakText;
        if (getBoolean("readSpeakerName", true)) {
            speakText = speakerName + "说：" + content;
        } else {
            speakText = content;
        }
        doSpeak(speakText);
    }
}

// ====== 发送按钮监听 ======
boolean onClickSendBtn(String text) {
    String content = text.trim();
    String talker = getTargetTalker();
    
    if(talker == null || talker.isEmpty()) {
        log("[TTS]talker无效，取消操作: content=" + content);
        toast("操作失败：请先发送消息以初始化会话");
        return true;
    }
    log("tts send button click: content=" + content + ", talker=" + talker);
    
    // ====== 摸鱼日报指令 ======
    boolean moyuEnabled = getBoolean("moyuEnabled", false)
    if (moyuEnabled) {
        String moyuCmd = getString("moyuCommand", "/摸鱼日报");
        if (content.equals(moyuCmd)) {
            fetchMoyu(talker);
            return true
        }
    }
    
    // ====== 小人举牌指令 ======
    boolean signEnabled = getBoolean("signEnabled", false)
    if (signEnabled) {
        String signCmd = getString("signCommand", DEFAULT_SIGN_COMMAND);
        if (content.startsWith(signCmd)) {
            String signMsg = content.substring(signCmd.length()).trim();
            if (signMsg.isEmpty()) {
                toast("举牌失败：请输入内容（如" + signCmd + " 你好）");
                return true
            }
            fetchSign(talker, signMsg);
            return true
        }
    }
    
    // ====== 合成表情指令 ======
    boolean emojiEnabled = getBoolean("emojiEnabled", false)
    if (emojiEnabled) {
        String emojiCmd = getString("emojiCommand", DEFAULT_EMOJI_COMMAND);
        if (content.startsWith(emojiCmd)) {
            String rest = content.substring(emojiCmd.length()).trim();
            if (rest.contains("+")) {
                var parts = rest.split("\\+", 2);
                String e1 = parts[0].trim();
                String e2 = parts[1].trim();
                if (!e1.isEmpty() && !e2.isEmpty()) {
                    fetchEmoji(talker, e1, e2);
                    return true
                }
            }
            toast("格式：" + emojiCmd + " 😂+😍");
            return true
        }
        if (content.contains("+")) {
            var parts = content.split("\\+", 2);
            String e1 = parts[0].trim();
            String e2 = parts[1].trim();
            if (!e1.isEmpty() && !e2.isEmpty() && e1.length() <= 4 && e2.length() <= 4) {
                fetchEmoji(talker, e1, e2);
                return true
            }
        }
    }
    
    // ====== 历史今天指令 ======
    boolean historyEnabled = getBoolean("historyEnabled", false)
    if (historyEnabled) {
        String historyCmd = getString("historyCommand", "/历史今天");
        if (content.equals(historyCmd)) {
            sendHistoryToday(talker);
            return true
        }
    }
    
    // ====== 每日六十秒指令 ======
    boolean sixtyEnabled = getBoolean("sixtyEnabled", false)
    if (sixtyEnabled) {
        String textCmd60 = getString("60sTextCommand", "/每日60秒");
        String imageCmd60 = getString("60sImageCommand", "/每日60秒图");
        if (content.equals(textCmd60) || content.equals(imageCmd60)) {
            String token = getString("60sToken", "");
            if (token == null || token.trim().isEmpty()) {
                toast("请先在每日六十秒设置中配置Token");
            } else {
                send60s(talker, content.equals(imageCmd60));
            }
            return true
        }
    }
    
    boolean ttsEnabled = getBoolean("ttsEnabled", true)
    if (!ttsEnabled) {
        toast("TTS功能已禁用，请在面板中开启")
        return true
    }
    
    String openCommand = getString("openCommand", DEFAULT_OPEN_COMMAND)
    String sendCommand = getString("sendCommand", DEFAULT_SEND_COMMAND)
    
    if(content.startsWith(openCommand)){
        log("[TTS]触发打开设置面板指令");
        openMainPanel();
        return true;
    }else if(content.startsWith(sendCommand)) {
        boolean ttsManual = getBoolean("ttsManual", true)
        if (!ttsManual) {
            toast("文字转语音功能已禁用，请在面板中开启")
            return true
        }
        
        String textContent = content.substring(sendCommand.length()).trim();
        if (textContent.length() == 0) {
            log("[TTS]指令内容为空，忽略");
            toast("TTS失败：请输入内容（如" + sendCommand + " 你好）");
            return true
        }
        log("触发TTS指令: talker=" + talker + ", content=" + textContent);
        toast("正在合成语音...");
        String textToSynthesize = textContent;
        String targetTalker = talker;
        new Thread(new Runnable() {
            public void run() {
                synthesizeAndSend(textToSynthesize, targetTalker);
            }
        }).start();
        return true;
    }
    log("未匹配触发指令: content=" + content);
    return false;
}
