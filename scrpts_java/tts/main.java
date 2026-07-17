// ==================== 配置区 ====================
// ✅ 音频保存目录
static final String AUDIO_DIR_NAME = "/tts_audio";
// 默认唤起面板命令
static final String DEFAULT_OPEN_COMMAND = "/TTS"
// 默认发送命令
static final String DEFAULT_SEND_COMMAND = "/tts"
// ================================================

// ====== 头像缓存相关 ======
static String sAvatarDir = null;
Map<String, android.graphics.Bitmap> avatarCache = new java.util.LinkedHashMap<String, android.graphics.Bitmap>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, android.graphics.Bitmap> eldest) {
        return size() > 80;
    }
};

void initAvatarDir() {
    if (sAvatarDir != null) return;
    try {
        java.io.File mmf = new java.io.File("/data/data/com.tencent.mm/MicroMsg");
        if (mmf.exists() && mmf.isDirectory()) {
            java.io.File[] subs = mmf.listFiles();
            if (subs != null) {
                for (java.io.File sub : subs) {
                    java.io.File avd = new java.io.File(sub, "avatar");
                    if (avd.exists() && avd.isDirectory()) {
                        sAvatarDir = avd.getAbsolutePath();
                        break;
                    }
                }
            }
        }
    } catch (Exception e) { }
    log("[TTS] 头像目录: " + (sAvatarDir != null ? sAvatarDir : "未找到"));
}

String getAvatarFilePath(String wxid) {
    if (sAvatarDir == null) return null;
    String[] exts = {".jpg", ".png", "_hd.jpg", "_hd.png", ".jpeg", ".webp"};
    for (String ext : exts) {
        java.io.File f = new java.io.File(sAvatarDir, wxid + ext);
        if (f.exists()) return f.getAbsolutePath();
    }
    return null;
}

void loadContactAvatar(final android.widget.ImageView imageView, final String wxid, final Object contact) {
    if (avatarCache.containsKey(wxid)) {
        imageView.setImageBitmap(avatarCache.get(wxid));
        return;
    }
    imageView.setImageResource(android.R.drawable.ic_menu_help);
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                android.graphics.Bitmap bm = null;
                try {
                    java.lang.reflect.Method m = contact.getClass().getMethod("getAvatar");
                    Object result = m.invoke(contact);
                    if (result instanceof android.graphics.Bitmap) {
                        bm = (android.graphics.Bitmap) result;
                    }
                } catch (Exception e1) {
                    try {
                        java.lang.reflect.Method m = contact.getClass().getMethod("getAvatarBitmap");
                        Object result = m.invoke(contact);
                        if (result instanceof android.graphics.Bitmap) {
                            bm = (android.graphics.Bitmap) result;
                        }
                    } catch (Exception e2) {}
                }
                if (bm == null) {
                    initAvatarDir();
                    String path = getAvatarFilePath(wxid);
                    if (path != null) {
                        bm = android.graphics.BitmapFactory.decodeFile(path);
                    }
                }
                if (bm != null) {
                    android.graphics.Bitmap circularBm = getCircularBitmap(bm);
                    avatarCache.put(wxid, circularBm);
                    final android.graphics.Bitmap finalBm = circularBm;
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(finalBm);
                        }
                    });
                }
            } catch (Exception e) { }
        }
    }).start();
}

android.graphics.Bitmap getCircularBitmap(android.graphics.Bitmap source) {
    int size = Math.min(source.getWidth(), source.getHeight());
    int x = (source.getWidth() - size) / 2;
    int y = (source.getHeight() - size) / 2;
    android.graphics.Bitmap squared = android.graphics.Bitmap.createBitmap(source, x, y, size, size);
    if (squared != source) {
        source.recycle();
    }
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

// ====== 判断深色模式（主面板用） ======
boolean isDarkMode(Context ctx) {
    int nightModeFlags = ctx.getResources().getConfiguration().uiMode 
        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
    return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
}

// ====== 打开面板（保持不变） ======
void openPanel(){
    var ctx = getTopActivity()
    boolean dark = isDarkMode(ctx);
    initAvatarDir();
    
    int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
    int contentBgColor = dark ? android.graphics.Color.parseColor("#2C2C2C") : android.graphics.Color.parseColor("#F5F7FA");
    int titleColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int descColor = dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D");
    int dividerColor = dark ? android.graphics.Color.parseColor("#444444") : android.graphics.Color.parseColor("#E8ECEF");
    int inputTextColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50");
    int labelColor = dark ? android.graphics.Color.parseColor("#DDDDDD") : android.graphics.Color.parseColor("#34495E");
    int versionColor = dark ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.parseColor("#95A5A6");
    int editBgColor = dark ? android.graphics.Color.parseColor("#3A3A3A") : android.graphics.Color.WHITE;
    
    var displayMetrics = new android.util.DisplayMetrics()
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
    int screenHeight = displayMetrics.heightPixels
    int screenWidth = displayMetrics.widthPixels
    
    var mainContainer = new LinearLayout(ctx)
    mainContainer.setOrientation(LinearLayout.VERTICAL)
    mainContainer.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    ))
    mainContainer.setBackgroundColor(bgColor)
    
    var titleLayout = new LinearLayout(ctx)
    titleLayout.setOrientation(LinearLayout.HORIZONTAL)
    titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL)
    titleLayout.setPadding(30, 35, 30, 25)
    titleLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    titleLayout.setBackgroundColor(bgColor)
    
    var titleText = new TextView(ctx)
    titleText.setText("TTS 语音合成设置")
    titleText.setTextColor(titleColor)
    titleText.setTextSize(22)
    titleText.setTypeface(null, android.graphics.Typeface.BOLD)
    titleText.setGravity(android.view.Gravity.LEFT)
    titleText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f))
    titleLayout.addView(titleText)
    
    var versionText = new TextView(ctx)
    versionText.setText(pluginVersion)
    versionText.setTextColor(versionColor)
    versionText.setTextSize(14)
    versionText.setGravity(android.view.Gravity.RIGHT)
    versionText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 0.3f))
    titleLayout.addView(versionText)
    
    mainContainer.addView(titleLayout)
    
    var titleDivider = new View(ctx)
    titleDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    titleDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(titleDivider)
    
    var scrollView = new ScrollView(ctx)
    var scrollParams = new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0)
    scrollParams.weight = 1.0f
    scrollView.setLayoutParams(scrollParams)
    scrollView.setPadding(0, 0, 0, 0)
    scrollView.setBackgroundColor(contentBgColor)
    
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(30, 15, 30, 15)
    mainLayout.setBackgroundColor(contentBgColor)
    mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    boolean savedTtsEnabled = getBoolean("ttsEnabled", true)
    boolean savedTtsManual = getBoolean("ttsManual", true)
    boolean savedAutoPlay = getBoolean("autoPlay", true)
    boolean savedPrivateChat = getBoolean("privateChat", true)
    boolean savedGroupChat = getBoolean("groupChat", false)
    boolean savedReadSelf = getBoolean("readSelf", false)
    String savedSpeed = getString("speed", "1.0")
    String savedPitch = getString("pitch", "1.0")
    String savedOpenCmd = getString("openCommand", DEFAULT_OPEN_COMMAND)
    String savedSendCmd = getString("sendCommand", DEFAULT_SEND_COMMAND)
    
    var masterSwitchRow = createSwitchRow(ctx, "🔊 TTS总开关", "启用或禁用所有TTS功能", savedTtsEnabled, dark)
    var masterSwitch = (Switch) masterSwitchRow.getTag()
    mainLayout.addView(masterSwitchRow)
    
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    var voiceGroup = createSection(ctx, "🎤 语音配置", titleColor)
    mainLayout.addView(voiceGroup)
    
    var speedLayout = createInputRow(ctx, "语速 (0.5-2.0)", savedSpeed, 120, labelColor, inputTextColor, editBgColor, dark)
    var speedEdit = (EditText) speedLayout.getTag()
    mainLayout.addView(speedLayout)
    
    var pitchLayout = createInputRow(ctx, "音调 (0.5-2.0)", savedPitch, 120, labelColor, inputTextColor, editBgColor, dark)
    var pitchEdit = (EditText) pitchLayout.getTag()
    mainLayout.addView(pitchLayout)
    
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    var cmdGroup = createSection(ctx, "⌨️ 命令配置", titleColor)
    mainLayout.addView(cmdGroup)
    
    var openCmdLayout = createInputRow(ctx, "打开面板命令", savedOpenCmd, 120, labelColor, inputTextColor, editBgColor, dark)
    var openCmdEdit = (EditText) openCmdLayout.getTag()
    mainLayout.addView(openCmdLayout)
    
    var sendCmdLayout = createInputRow(ctx, "TTS发送命令", savedSendCmd, 120, labelColor, inputTextColor, editBgColor, dark)
    var sendCmdEdit = (EditText) sendCmdLayout.getTag()
    mainLayout.addView(sendCmdLayout)
    
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    var switchGroup = createSection(ctx, "⚙️ 功能开关", titleColor)
    mainLayout.addView(switchGroup)
    
    var ttsManualRow = createSwitchRow(ctx, "📝 文字转语音", "手动输入命令进行语音合成", savedTtsManual, dark)
    var ttsManualSwitch = (Switch) ttsManualRow.getTag()
    mainLayout.addView(ttsManualRow)
    
    var autoPlayRow = createSwitchRow(ctx, "🔁 自动朗读", "收到消息时自动朗读", savedAutoPlay, dark)
    var autoPlaySwitch = (Switch) autoPlayRow.getTag()
    mainLayout.addView(autoPlayRow)
    
    var readSelfRow = createSwitchRow(ctx, "朗读自己", "朗读自己发送的消息", savedReadSelf, dark)
    var readSelfSwitch = (Switch) readSelfRow.getTag()
    mainLayout.addView(readSelfRow)
    
    var privateRow = createSwitchRow(ctx, "私聊朗读", "在私聊中启用TTS", savedPrivateChat, dark)
    var privateSwitch = (Switch) privateRow.getTag()
    mainLayout.addView(privateRow)
    
    var groupRow = createSwitchRow(ctx, "群聊朗读", "在群聊中启用TTS", savedGroupChat, dark)
    var groupSwitch = (Switch) groupRow.getTag()
    mainLayout.addView(groupRow)
    
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    var listGroup = createSection(ctx, "📋 白名单/黑名单配置", titleColor)
    mainLayout.addView(listGroup)
    
    Map<String, TextView> titleViewMap = new HashMap();
    
    var privateWhiteRow = createListSelectorRow(ctx, "私聊白名单", "仅朗读白名单中的好友", "privateWhiteList", dark, titleViewMap)
    mainLayout.addView(privateWhiteRow)
    
    var privateBlackRow = createListSelectorRow(ctx, "私聊黑名单", "不朗读黑名单中的好友", "privateBlackList", dark, titleViewMap)
    mainLayout.addView(privateBlackRow)
    
    var groupWhiteRow = createListSelectorRow(ctx, "群聊白名单", "仅朗读白名单中的群聊", "groupWhiteList", dark, titleViewMap)
    mainLayout.addView(groupWhiteRow)
    
    var groupBlackRow = createListSelectorRow(ctx, "群聊黑名单", "不朗读黑名单中的群聊", "groupBlackList", dark, titleViewMap)
    mainLayout.addView(groupBlackRow)
    
    var tipView = new TextView(ctx)
    tipView.setText("💡 提示：白名单和黑名单不能同时生效，白名单优先级更高")
    tipView.setTextColor(descColor)
    tipView.setTextSize(11)
    tipView.setPadding(0, 10, 0, 5)
    mainLayout.addView(tipView)
    
    scrollView.addView(mainLayout)
    mainContainer.addView(scrollView)
    
    var buttonDivider = new View(ctx)
    buttonDivider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 2))
    buttonDivider.setBackgroundColor(dividerColor)
    mainContainer.addView(buttonDivider)
    
    var buttonLayout = new LinearLayout(ctx)
    buttonLayout.setOrientation(LinearLayout.HORIZONTAL)
    buttonLayout.setPadding(30, 35, 30, 35)
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    var resetBtn = new Button(ctx)
    resetBtn.setText("重置默认")
    resetBtn.setTextSize(17)
    resetBtn.setTextColor(android.graphics.Color.WHITE)
    resetBtn.setPadding(20, 0, 20, 0)
    resetBtn.setBackground(createRoundedDrawable("#F39C12", 8))
    var resetParams = new LinearLayout.LayoutParams(0, 120, 0.3f)
    resetParams.setMargins(0, 0, 10, 0)
    resetBtn.setLayoutParams(resetParams)
    buttonLayout.addView(resetBtn)
    
    var cancelBtn = new Button(ctx)
    cancelBtn.setText("取消")
    cancelBtn.setTextSize(17)
    cancelBtn.setTextColor(android.graphics.Color.WHITE)
    cancelBtn.setPadding(20, 0, 20, 0)
    cancelBtn.setBackground(createRoundedDrawable("#95A5A6", 8))
    var cancelParams = new LinearLayout.LayoutParams(0, 120, 0.3f)
    cancelParams.setMargins(5, 0, 5, 0)
    cancelBtn.setLayoutParams(cancelParams)
    buttonLayout.addView(cancelBtn)
    
    var saveBtn = new Button(ctx)
    saveBtn.setText("保存")
    saveBtn.setTextSize(17)
    saveBtn.setTextColor(android.graphics.Color.WHITE)
    saveBtn.setPadding(20, 0, 20, 0)
    saveBtn.setBackground(createRoundedDrawable("#3498DB", 8))
    var saveParams = new LinearLayout.LayoutParams(0, 120, 0.3f)
    saveParams.setMargins(10, 0, 0, 0)
    saveBtn.setLayoutParams(saveParams)
    buttonLayout.addView(saveBtn)
    
    mainContainer.addView(buttonLayout)
    
    var builder = new AlertDialog.Builder(ctx)
        .setView(mainContainer)
    
    var dialog = builder.create()
    dialog.show()
    
    var window = dialog.getWindow()
    if (window != null) {
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor))
        int dialogWidth = (int)(screenWidth * 0.92)
        int dialogHeight = (int)(screenHeight * 0.75)
        window.setLayout(dialogWidth, dialogHeight)
        window.setGravity(android.view.Gravity.CENTER)
    }
    
    saveBtn.setOnClickListener((view) -> {
        try {
            String speed = speedEdit.getText().toString().trim()
            String pitch = pitchEdit.getText().toString().trim()
            String openCmd = openCmdEdit.getText().toString().trim()
            String sendCmd = sendCmdEdit.getText().toString().trim()
            
            if (speed.isEmpty()) { toast("请输入语速"); return; }
            if (pitch.isEmpty()) { toast("请输入音调"); return; }
            if (openCmd.isEmpty()) { toast("请输入打开面板命令"); return; }
            if (sendCmd.isEmpty()) { toast("请输入TTS发送命令"); return; }
            
            putBoolean("ttsEnabled", masterSwitch.isChecked())
            putBoolean("ttsManual", ttsManualSwitch.isChecked())
            putString("speed", speed)
            putString("pitch", pitch)
            putString("openCommand", openCmd)
            putString("sendCommand", sendCmd)
            putBoolean("autoPlay", autoPlaySwitch.isChecked())
            putBoolean("readSelf", readSelfSwitch.isChecked())
            putBoolean("privateChat", privateSwitch.isChecked())
            putBoolean("groupChat", groupSwitch.isChecked())
            
            toast("✅ 保存成功")
            log("[TTS]配置已更新")
            dialog.dismiss()
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage())
            log("[TTS]保存配置异常：" + e.toString())
        }
    })
    
    cancelBtn.setOnClickListener((view) -> { dialog.dismiss(); })
    
    resetBtn.setOnClickListener((view) -> {
        masterSwitch.setChecked(true)
        ttsManualSwitch.setChecked(true)
        autoPlaySwitch.setChecked(true)
        speedEdit.setText("1.0")
        pitchEdit.setText("1.0")
        openCmdEdit.setText(DEFAULT_OPEN_COMMAND)
        sendCmdEdit.setText(DEFAULT_SEND_COMMAND)
        readSelfSwitch.setChecked(false)
        privateSwitch.setChecked(true)
        groupSwitch.setChecked(false)
        putString("privateWhiteList", "")
        putString("privateBlackList", "")
        putString("groupWhiteList", "")
        putString("groupBlackList", "")
        updateSelectedDisplay("privateWhiteList", (TextView) privateWhiteRow.getTag(), ctx)
        updateSelectedDisplay("privateBlackList", (TextView) privateBlackRow.getTag(), ctx)
        updateSelectedDisplay("groupWhiteList", (TextView) groupWhiteRow.getTag(), ctx)
        updateSelectedDisplay("groupBlackList", (TextView) groupBlackRow.getTag(), ctx)
        refreshAllEffectiveStatus(titleViewMap);
        toast("已重置为默认配置")
    })
}

// ====== 辅助UI函数（不变） ======
android.graphics.drawable.GradientDrawable createRoundedDrawable(String colorHex, int radius) {
    android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
    drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
    drawable.setCornerRadius(radius);
    drawable.setColor(android.graphics.Color.parseColor(colorHex));
    return drawable;
}

View createDivider(Context ctx, int padding, int color) {
    var divider = new View(ctx)
    divider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1))
    divider.setBackgroundColor(color)
    divider.setPadding(0, padding, 0, padding)
    return divider
}

LinearLayout createSection(Context ctx, String title, int color) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setPadding(0, 8, 0, 4)
    var textView = new TextView(ctx)
    textView.setText(title)
    textView.setTextColor(color)
    textView.setTextSize(15)
    textView.setTypeface(null, android.graphics.Typeface.BOLD)
    layout.addView(textView)
    return layout
}

LinearLayout createInputRow(Context ctx, String label, String defaultValue, int height, int labelColor, int textColor, int editBgColor, boolean dark) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setPadding(0, 3, 0, 3)
    layout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    var labelView = new TextView(ctx)
    labelView.setText(label + "：")
    labelView.setTextColor(labelColor)
    labelView.setTextSize(15)
    labelView.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 0.4f))
    layout.addView(labelView)
    
    var editText = new EditText(ctx)
    editText.setText(defaultValue)
    editText.setTextColor(textColor)
    editText.setTextSize(15)
    var editBg = createRoundedDrawable(dark ? "#3A3A3A" : "#FFFFFF", 8);
    editText.setBackground(editBg)
    editText.setPadding(12, 8, 12, 8)
    editText.setLayoutParams(new LinearLayout.LayoutParams(0, height, 0.6f))
    if (dark) {
        editText.setHintTextColor(android.graphics.Color.parseColor("#888888"));
    } else {
        editText.setHintTextColor(android.graphics.Color.GRAY);
    }
    layout.addView(editText)
    layout.setTag(editText)
    return layout
}

LinearLayout createSwitchRow(Context ctx, String title, String desc, boolean defaultValue, boolean dark) {
    var layout = new LinearLayout(ctx)
    layout.setOrientation(LinearLayout.HORIZONTAL)
    layout.setPadding(0, 5, 0, 5)
    layout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    var textLayout = new LinearLayout(ctx)
    textLayout.setOrientation(LinearLayout.VERTICAL)
    textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f))
    var titleView = new TextView(ctx)
    titleView.setText(title)
    titleView.setTextColor(dark ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#2C3E50"))
    titleView.setTextSize(15)
    textLayout.addView(titleView)
    var descView = new TextView(ctx)
    descView.setText(desc)
    descView.setTextColor(dark ? android.graphics.Color.parseColor("#AAAAAA") : android.graphics.Color.parseColor("#7F8C8D"))
    descView.setTextSize(12)
    textLayout.addView(descView)
    layout.addView(textLayout)
    var switchBtn = new Switch(ctx)
    switchBtn.setChecked(defaultValue)
    switchBtn.setLayoutParams(new LinearLayout.LayoutParams(0, 
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 0.3f))
    layout.addView(switchBtn)
    layout.setTag(switchBtn)
    return layout
}

void updateEffectiveStatus(TextView titleView, String key) {
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
    titleView.setText(effective ? baseTitle + "（生效）" : baseTitle);
}

void refreshAllEffectiveStatus(Map<String, TextView> titleViewMap) {
    if (titleViewMap == null) return;
    for (Map.Entry<String, TextView> entry : titleViewMap.entrySet()) {
        updateEffectiveStatus(entry.getValue(), entry.getKey());
    }
}

LinearLayout createListSelectorRow(Context ctx, String title, String desc, String key, boolean dark, Map<String, TextView> titleViewMap) {
    var outerLayout = new LinearLayout(ctx);
    outerLayout.setOrientation(LinearLayout.VERTICAL);
    outerLayout.setPadding(0, 4, 0, 4);
    outerLayout.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    
    var titleRow = new RelativeLayout(ctx);
    titleRow.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    final TextView titleView = new TextView(ctx);
    titleView.setText(title);
    titleView.setTextColor(dark ? Color.WHITE : Color.parseColor("#2C3E50"));
    titleView.setTextSize(14);
    RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    titleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
    titleView.setLayoutParams(titleParams);
    titleRow.addView(titleView);
    
    var btnContainer = new LinearLayout(ctx);
    btnContainer.setOrientation(LinearLayout.HORIZONTAL);
    btnContainer.setGravity(Gravity.CENTER_VERTICAL);
    RelativeLayout.LayoutParams btnContainerParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT, 120);
    btnContainerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    btnContainerParams.addRule(RelativeLayout.CENTER_VERTICAL);
    btnContainer.setLayoutParams(btnContainerParams);
    
    var selectBtn = new Button(ctx);
    selectBtn.setText("选择");
    selectBtn.setTextSize(15);
    selectBtn.setTextColor(Color.WHITE);
    selectBtn.setPadding(36, 0, 36, 0);
    selectBtn.setBackground(createRoundedDrawable("#3498DB", 8));
    selectBtn.setMinHeight(0);
    selectBtn.setMinimumHeight(0);
    LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 120);
    selectParams.setMargins(0, 0, 12, 0);
    selectBtn.setLayoutParams(selectParams);
    btnContainer.addView(selectBtn);
    
    var clearBtn = new Button(ctx);
    clearBtn.setText("清空");
    clearBtn.setTextSize(15);
    clearBtn.setTextColor(Color.WHITE);
    clearBtn.setPadding(36, 0, 36, 0);
    clearBtn.setBackground(createRoundedDrawable("#E74C3C", 8));
    clearBtn.setMinHeight(0);
    clearBtn.setMinimumHeight(0);
    LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 120);
    clearBtn.setLayoutParams(clearParams);
    btnContainer.addView(clearBtn);
    
    titleRow.addView(btnContainer);
    outerLayout.addView(titleRow);
    
    var descView = new TextView(ctx);
    descView.setText(desc);
    descView.setTextColor(dark ? Color.parseColor("#AAAAAA") : Color.parseColor("#7F8C8D"));
    descView.setTextSize(11);
    descView.setPadding(0, 2, 0, 0);
    outerLayout.addView(descView);
    
    var selectedLayout = new LinearLayout(ctx);
    selectedLayout.setOrientation(LinearLayout.HORIZONTAL);
    selectedLayout.setPadding(0, 2, 0, 6);
    selectedLayout.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    final TextView selectedView = new TextView(ctx);
    selectedView.setText("已选：无");
    selectedView.setTextColor(dark ? Color.WHITE : Color.parseColor("#34495E"));
    selectedView.setTextSize(12);
    selectedView.setGravity(Gravity.CENTER_VERTICAL);
    selectedView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    selectedLayout.addView(selectedView);
    outerLayout.addView(selectedLayout);
    
    var divider = new View(ctx);
    divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    int dividerColor = dark ? Color.parseColor("#444444") : Color.parseColor("#E0E0E0");
    divider.setBackgroundColor(dividerColor);
    divider.setPadding(0, 6, 0, 0);
    outerLayout.addView(divider);
    
    outerLayout.setTag(selectedView);
    if (titleViewMap != null) titleViewMap.put(key, titleView);
    
    updateEffectiveStatus(titleView, key);
    updateSelectedDisplay(key, selectedView, ctx);
    
    selectBtn.setOnClickListener((view) -> {
        showListSelector(ctx, key, selectedView, titleView, titleViewMap);
    });
    clearBtn.setOnClickListener((view) -> {
        putString(key, "");
        selectedView.setText("已选：无");
        toast("已清空");
        refreshAllEffectiveStatus(titleViewMap);
    });
    return outerLayout;
}

// ====== 显示列表选择器（最终修正版：CheckBox 直接响应点击，头像可加载） ======
void showListSelector(Context ctx, String key, TextView selectedView, TextView titleView, Map<String, TextView> titleViewMap) {
    try {
        boolean isPrivate = key.startsWith("private");
        int nightModeFlags = ctx.getResources().getConfiguration().uiMode 
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        final boolean dark = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
        final int textColor = dark ? android.graphics.Color.WHITE : android.graphics.Color.BLACK;
        final int bgColor = dark ? android.graphics.Color.parseColor("#1E1E1E") : android.graphics.Color.WHITE;
        final int itemBgColor = dark ? android.graphics.Color.parseColor("#3A3A3A") : android.graphics.Color.WHITE;
        final int hintColor = dark ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.GRAY;

        // 收集好友/群聊数据
        java.util.List<String> allWxids = new java.util.ArrayList<>();
        java.util.List<String> allNames = new java.util.ArrayList<>();
        java.util.List<Object> allContacts = new java.util.ArrayList<>();
        if (isPrivate) {
            var friends = getFriendList();
            if (friends != null) {
                for (var f : friends) {
                    try {
                        String name = f.getRemark();
                        if (name == null || name.isEmpty()) name = f.getNickname();
                        if (name == null || name.isEmpty()) name = f.getWxid();
                        allWxids.add(f.getWxid());
                        allNames.add(name);
                        allContacts.add(f);
                    } catch (Exception e) { }
                }
            }
        } else {
            var groups = getGroupList();
            if (groups != null) {
                for (var g : groups) {
                    try {
                        String name = g.getRemark();
                        if (name == null || name.isEmpty()) name = g.getName();
                        if (name == null || name.isEmpty()) name = g.getRoomId();
                        allWxids.add(g.getRoomId());
                        allNames.add(name);
                        allContacts.add(g);
                    } catch (Exception e) { }
                }
            }
        }
        if (allWxids.isEmpty()) {
            toast("没有可用的" + (isPrivate ? "好友" : "群聊"));
            return;
        }

        String saved = getString(key, "");
        final java.util.Set<String> selectedSet = new java.util.HashSet<>();
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) selectedSet.add(trimmed);
            }
        }

        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        int screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
        int dialogWidth = (int)(screenWidth * 0.92);
        int dialogHeight = (int)(screenHeight * 0.85);

        // 构建界面
        android.widget.LinearLayout rootLayout = new android.widget.LinearLayout(ctx);
        rootLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(bgColor);

        // 标题
        android.widget.TextView tvTitle = new android.widget.TextView(ctx);
        tvTitle.setText("选择" + (isPrivate ? "好友" : "群聊"));
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(textColor);
        tvTitle.setPadding(20, 20, 20, 10);
        rootLayout.addView(tvTitle);

        // 搜索框
        android.widget.EditText etSearch = new android.widget.EditText(ctx);
        etSearch.setHint("搜索...");
        etSearch.setTextSize(16);
        etSearch.setTextColor(textColor);
        etSearch.setHintTextColor(hintColor);
        etSearch.setPadding(20, 10, 20, 10);
        rootLayout.addView(etSearch, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        // 操作按钮行
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(ctx);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setPadding(10, 5, 10, 5);

        android.widget.Button btnAll = new android.widget.Button(ctx);
        btnAll.setText("全选");
        btnRow.addView(btnAll, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.Button btnInvert = new android.widget.Button(ctx);
        btnInvert.setText("反选");
        btnRow.addView(btnInvert, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.Button btnClear = new android.widget.Button(ctx);
        btnClear.setText("清空");
        btnRow.addView(btnClear, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        rootLayout.addView(btnRow, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        // 可滚动的列表区域
        android.widget.ScrollView scrollView = new android.widget.ScrollView(ctx);
        scrollView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        final android.widget.LinearLayout itemContainer = new android.widget.LinearLayout(ctx);
        itemContainer.setOrientation(android.widget.LinearLayout.VERTICAL);

        // 存储所有复选框和对应ID，便于操作和保存
        java.util.List<android.widget.CheckBox> allCheckBoxes = new java.util.ArrayList<>();
        java.util.List<String> itemIds = new java.util.ArrayList<>();
        java.util.List<android.view.View> allItemViews = new java.util.ArrayList<>();

        // 动态生成所有条目
        for (int i = 0; i < allWxids.size(); i++) {
            String wxid = allWxids.get(i);
            String name = allNames.get(i);
            boolean checked = selectedSet.contains(wxid);

            android.widget.LinearLayout itemRow = new android.widget.LinearLayout(ctx);
            itemRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            itemRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            itemRow.setPadding(12, 8, 12, 8);
            itemRow.setBackgroundColor(itemBgColor);

            // 头像
            int avatarSize = (int)(32 * ctx.getResources().getDisplayMetrics().density + 0.5f);
            android.widget.ImageView iv = new android.widget.ImageView(ctx);
            iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            android.widget.LinearLayout.LayoutParams ivParams = new android.widget.LinearLayout.LayoutParams(avatarSize, avatarSize);
            ivParams.rightMargin = 12;
            itemRow.addView(iv, ivParams);
            loadContactAvatar(iv, wxid, allContacts.get(i));

            // 复选框（直接响应点击，不再禁用）
            android.widget.CheckBox cb = new android.widget.CheckBox(ctx);
            cb.setChecked(checked);
            android.widget.LinearLayout.LayoutParams cbParams = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            cbParams.rightMargin = 8;
            itemRow.addView(cb, cbParams);

            // 名称
            android.widget.TextView tvName = new android.widget.TextView(ctx);
            tvName.setText(name);
            tvName.setTextSize(14);
            tvName.setTextColor(textColor);
            tvName.setSingleLine(true);
            itemRow.addView(tvName, new android.widget.LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            itemContainer.addView(itemRow);
            allCheckBoxes.add(cb);
            itemIds.add(wxid);
            allItemViews.add(itemRow);
        }

        scrollView.addView(itemContainer);
        rootLayout.addView(scrollView);

        // 底部按钮
        android.widget.LinearLayout bottomRow = new android.widget.LinearLayout(ctx);
        bottomRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        bottomRow.setPadding(20, 10, 20, 10);

        android.widget.Button btnCancel = new android.widget.Button(ctx);
        btnCancel.setText("取消");
        bottomRow.addView(btnCancel, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        android.widget.Button btnConfirm = new android.widget.Button(ctx);
        btnConfirm.setText("确定");
        bottomRow.addView(btnConfirm, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        rootLayout.addView(bottomRow, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        // 对话框
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(ctx)
                .setView(rootLayout)
                .create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(bgColor));
            window.setLayout(dialogWidth, dialogHeight);
        }

        // ====== 按钮事件 ======
        btnAll.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(true);
                }
            }
        });
        btnInvert.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(!cb.isChecked());
                }
            }
        });
        btnClear.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                for (android.widget.CheckBox cb : allCheckBoxes) {
                    cb.setChecked(false);
                }
            }
        });

        btnConfirm.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < allCheckBoxes.size(); i++) {
                    if (allCheckBoxes.get(i).isChecked()) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(itemIds.get(i));
                    }
                }
                putString(key, sb.toString());
                updateSelectedDisplay(key, selectedView, ctx);
                refreshAllEffectiveStatus(titleViewMap);
                toast("保存成功");
                dialog.dismiss();
            }
        });
        btnCancel.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                dialog.dismiss();
            }
        });

        // 搜索过滤（控制可见性）
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String keyword = s.toString().toLowerCase();
                for (int i = 0; i < allWxids.size(); i++) {
                    String name = allNames.get(i).toLowerCase();
                    String id = allWxids.get(i).toLowerCase();
                    boolean match = keyword.isEmpty() || name.contains(keyword) || id.contains(keyword);
                    allItemViews.get(i).setVisibility(match ? android.view.View.VISIBLE : android.view.View.GONE);
                }
            }
        });

    } catch (Exception e) {
        toast("打开选择器失败：" + e.getMessage());
        log("[TTS] 打开选择器异常：" + e.toString());
    }
}

// ====== 其他函数（不变） ======
void updateSelectedDisplay(String key, TextView selectedView, Context ctx) {
    try {
        String savedList = getString(key, "")
        if (savedList == null || savedList.isEmpty()) {
            selectedView.setText("已选：无")
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
        selectedView.setText(display)
    } catch (Exception e) {
        selectedView.setText("已选：无")
        log("[TTS]更新显示失败：" + e.toString())
    }
}

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
                    } catch (Exception e) {}
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
                    } catch (Exception e) {}
                }
            }
        }
    } catch (Exception e) {}
    return null
}

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
                            if (name == null || name.isEmpty()) name = group.getName()
                            return name != null ? name : roomId
                        }
                    } catch (Exception e) {}
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
                            if (name == null || name.isEmpty()) name = friend.getNickname()
                            return name != null ? name : wxid
                        }
                    } catch (Exception e) {}
                }
            }
        }
    } catch (Exception e) {}
    return talker
}

void onHandleMsg(Object msgInfoBean) {
    if (msgInfoBean.isText()) {
        var content = msgInfoBean.getContent();
        var talker = msgInfoBean.getTalker();
        boolean ttsEnabled = getBoolean("ttsEnabled", true)
        if (!ttsEnabled) return
        boolean autoPlay = getBoolean("autoPlay", true)
        if (!autoPlay) return
        boolean readSelf = getBoolean("readSelf", false)
        boolean isSelf = msgInfoBean.isSend()
        if (isSelf && !readSelf) return
        boolean isGroup = msgInfoBean.isGroupChat()
        if (isGroup && !getBoolean("groupChat", false)) return
        if (!isGroup && !getBoolean("privateChat", true)) return
        if (!checkInList(talker, isGroup)) {
            log("[TTS] " + talker + " 在黑名单中，跳过朗读")
            return
        }
    }
}

boolean onClickSendBtn(String text) {
    String content = text.trim();
    String talker = getTargetTalker();
    if(talker == null || talker.isEmpty()) {
        log("[TTS]talker无效，取消操作: content=" + content);
        toast("操作失败：请先发送消息以初始化会话");
        return true;
    }
    log("tts send button click: content=" + content + ", talker=" + talker);
    boolean ttsEnabled = getBoolean("ttsEnabled", true)
    if (!ttsEnabled) {
        toast("TTS功能已禁用，请在面板中开启")
        return true
    }
    String openCommand = getString("openCommand", DEFAULT_OPEN_COMMAND)
    String sendCommand = getString("sendCommand", DEFAULT_SEND_COMMAND)
    if(content.startsWith(openCommand)){
        log("[TTS]触发打开设置面板指令");
        openPanel();
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
        return true;
    }
    log("未匹配触发指令: content=" + content);
    return false;
}
