// ==================== 配置区 ====================
// ✅ 音频保存目录
static final String AUDIO_DIR_NAME = "/tts_audio";
// 默认唤起面板命令
static final String DEFAULT_OPEN_COMMAND = "/TTS"
// 默认发送命令
static final String DEFAULT_SEND_COMMAND = "/tts"
// ================================================

// ====== 头像缓存与加载 ======
static java.util.concurrent.atomic.AtomicReference<String> sAvatarDir = new java.util.concurrent.atomic.AtomicReference<>(null);
final Map<String, android.graphics.Bitmap> avatarCache = new java.util.LinkedHashMap<String, android.graphics.Bitmap>() {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, android.graphics.Bitmap> eldest) {
        return size() > 80;
    }
};

void initAvatarDir() {
    if (sAvatarDir.get() != null) return;
    try {
        java.io.File mmf = new java.io.File("/data/data/com.tencent.mm/MicroMsg");
        if (mmf.exists() && mmf.isDirectory()) {
            java.io.File[] subs = mmf.listFiles();
            if (subs != null) {
                for (java.io.File sub : subs) {
                    java.io.File avd = new java.io.File(sub, "avatar");
                    if (avd.exists() && avd.isDirectory()) {
                        sAvatarDir.set(avd.getAbsolutePath());
                        break;
                    }
                }
            }
        }
    } catch (Exception e) { }
    log("[TTS] 头像目录: " + (sAvatarDir.get() != null ? sAvatarDir.get() : "未找到"));
}

String getAvatarFilePath(String wxid) {
    String dir = sAvatarDir.get();
    if (dir == null) return null;
    String[] exts = {".jpg", ".png", "_hd.jpg", "_hd.png", ".jpeg", ".webp"};
    for (String ext : exts) {
        java.io.File f = new java.io.File(dir, wxid + ext);
        if (f.exists()) return f.getAbsolutePath();
    }
    return null;
}

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

void loadItemAvatar(final android.widget.ImageView imageView, final String wxid, final Object contact) {
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

// ====== 判断当前是否为深色模式 ======
boolean isDarkMode(Context ctx) {
    int nightModeFlags = ctx.getResources().getConfiguration().uiMode 
        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
    return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
}

// ====== 打开面板 - 自动适配深色/浅色模式 ======
void openPanel(){
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
    int versionColor = dark ? android.graphics.Color.parseColor("#888888") : android.graphics.Color.parseColor("#95A5A6");
    int editBgColor = dark ? android.graphics.Color.parseColor("#3A3A3A") : android.graphics.Color.WHITE;
    
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
    scrollView.setPadding(0, 0, 0, 0)
    scrollView.setBackgroundColor(contentBgColor)
    
    // 主容器
    var mainLayout = new LinearLayout(ctx)
    mainLayout.setOrientation(LinearLayout.VERTICAL)
    mainLayout.setPadding(30, 15, 30, 15)
    mainLayout.setBackgroundColor(contentBgColor)
    mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    // 获取保存的配置
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
    
    // ====== 总开关 ======
    var masterSwitchRow = createSwitchRow(ctx, "🔊 TTS总开关", "启用或禁用所有TTS功能", savedTtsEnabled, dark)
    var masterSwitch = (Switch) masterSwitchRow.getTag()
    mainLayout.addView(masterSwitchRow)
    
    // 分割线
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    // ====== 语音配置组 ======
    var voiceGroup = createSection(ctx, "🎤 语音配置", titleColor)
    mainLayout.addView(voiceGroup)
    
    // 语速（高度120dp）
    var speedLayout = createInputRow(ctx, "语速 (0.5-2.0)", savedSpeed, 120, labelColor, inputTextColor, editBgColor, dark)
    var speedEdit = (EditText) speedLayout.getTag()
    mainLayout.addView(speedLayout)
    
    // 音调（高度120dp）
    var pitchLayout = createInputRow(ctx, "音调 (0.5-2.0)", savedPitch, 120, labelColor, inputTextColor, editBgColor, dark)
    var pitchEdit = (EditText) pitchLayout.getTag()
    mainLayout.addView(pitchLayout)
    
    // 分割线
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    // ====== 命令配置组 ======
    var cmdGroup = createSection(ctx, "⌨️ 命令配置", titleColor)
    mainLayout.addView(cmdGroup)
    
    // 打开面板命令（高度120dp）
    var openCmdLayout = createInputRow(ctx, "打开面板命令", savedOpenCmd, 120, labelColor, inputTextColor, editBgColor, dark)
    var openCmdEdit = (EditText) openCmdLayout.getTag()
    mainLayout.addView(openCmdLayout)
    
    // 发送TTS命令（高度120dp）
    var sendCmdLayout = createInputRow(ctx, "TTS发送命令", savedSendCmd, 120, labelColor, inputTextColor, editBgColor, dark)
    var sendCmdEdit = (EditText) sendCmdLayout.getTag()
    mainLayout.addView(sendCmdLayout)
    
    // 分割线
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    // ====== 功能开关组 ======
    var switchGroup = createSection(ctx, "⚙️ 功能开关", titleColor)
    mainLayout.addView(switchGroup)
    
    // 文字转语音开关
    var ttsManualRow = createSwitchRow(ctx, "📝 文字转语音", "手动输入命令进行语音合成", savedTtsManual, dark)
    var ttsManualSwitch = (Switch) ttsManualRow.getTag()
    mainLayout.addView(ttsManualRow)
    
    // 自动播放开关
    var autoPlayRow = createSwitchRow(ctx, "🔁 自动朗读", "收到消息时自动朗读", savedAutoPlay, dark)
    var autoPlaySwitch = (Switch) autoPlayRow.getTag()
    mainLayout.addView(autoPlayRow)
    
    // 朗读自己
    var readSelfRow = createSwitchRow(ctx, "朗读自己", "朗读自己发送的消息", savedReadSelf, dark)
    var readSelfSwitch = (Switch) readSelfRow.getTag()
    mainLayout.addView(readSelfRow)
    
    // 私聊开关
    var privateRow = createSwitchRow(ctx, "私聊朗读", "在私聊中启用TTS", savedPrivateChat, dark)
    var privateSwitch = (Switch) privateRow.getTag()
    mainLayout.addView(privateRow)
    
    // 群聊开关
    var groupRow = createSwitchRow(ctx, "群聊朗读", "在群聊中启用TTS", savedGroupChat, dark)
    var groupSwitch = (Switch) groupRow.getTag()
    mainLayout.addView(groupRow)
    
    // 分割线
    mainLayout.addView(createDivider(ctx, 6, dividerColor))
    
    // ====== 白名单/黑名单配置 ======
    var listGroup = createSection(ctx, "📋 白名单/黑名单配置", titleColor)
    mainLayout.addView(listGroup)
    
    // 用于存储所有选择器行的标题视图（联动刷新）
    Map<String, TextView> titleViewMap = new HashMap();
    
    // 私聊白名单
    var privateWhiteRow = createListSelectorRow(ctx, "私聊白名单", "仅朗读白名单中的好友", "privateWhiteList", dark, titleViewMap)
    mainLayout.addView(privateWhiteRow)
    
    // 私聊黑名单
    var privateBlackRow = createListSelectorRow(ctx, "私聊黑名单", "不朗读黑名单中的好友", "privateBlackList", dark, titleViewMap)
    mainLayout.addView(privateBlackRow)
    
    // 群聊白名单
    var groupWhiteRow = createListSelectorRow(ctx, "群聊白名单", "仅朗读白名单中的群聊", "groupWhiteList", dark, titleViewMap)
    mainLayout.addView(groupWhiteRow)
    
    // 群聊黑名单
    var groupBlackRow = createListSelectorRow(ctx, "群聊黑名单", "不朗读黑名单中的群聊", "groupBlackList", dark, titleViewMap)
    mainLayout.addView(groupBlackRow)
    
    // 底部提示
    var tipView = new TextView(ctx)
    tipView.setText("💡 提示：白名单和黑名单不能同时生效，白名单优先级更高")
    tipView.setTextColor(descColor)
    tipView.setTextSize(11)
    tipView.setPadding(0, 10, 0, 5)
    mainLayout.addView(tipView)
    
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
    buttonLayout.setPadding(30, 35, 30, 35)
    buttonLayout.setBackgroundColor(bgColor)
    buttonLayout.setGravity(android.view.Gravity.CENTER)
    buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    ))
    
    // 重置按钮 - 高度120dp，颜色 #F39C12
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
    
    // 取消按钮 - 高度120dp
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
    
    // 保存按钮 - 高度120dp
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
    
    // 构建对话框
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
    
    // ====== 按钮点击事件 ======
    saveBtn.setOnClickListener((view) -> {
        try {
            String speed = speedEdit.getText().toString().trim()
            String pitch = pitchEdit.getText().toString().trim()
            String openCmd = openCmdEdit.getText().toString().trim()
            String sendCmd = sendCmdEdit.getText().toString().trim()
            
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
    
    cancelBtn.setOnClickListener((view) -> {
        dialog.dismiss()
    })
    
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

// ====== 创建分割线（带颜色） ======
View createDivider(Context ctx, int padding, int color) {
    var divider = new View(ctx)
    divider.setLayoutParams(new LinearLayout.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1))
    divider.setBackgroundColor(color)
    divider.setPadding(0, padding, 0, padding)
    return divider
}

// ====== 创建分组标题（带颜色） ======
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

// ====== 创建输入行（高度由参数指定，统一120dp） ======
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
    editText.setLayoutParams(new LinearLayout.LayoutParams(0, 
        height, 0.6f))  // height 统一为120
    if (dark) {
        editText.setHintTextColor(android.graphics.Color.parseColor("#888888"));
    } else {
        editText.setHintTextColor(android.graphics.Color.GRAY);
    }
    layout.addView(editText)
    
    layout.setTag(editText)
    return layout
}

// ====== 创建开关行（带深色模式） ======
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

// ====== 更新单个选择器行的生效状态标题 ======
void updateEffectiveStatus(TextView titleView, String key) {
    boolean isWhite = key.endsWith("WhiteList");
    boolean isPrivate = key.startsWith("private");
    String whiteKey = (isPrivate ? "private" : "group") + "WhiteList";
    String blackKey = (isPrivate ? "private" : "group") + "BlackList";
    String white = getString(whiteKey, "");
    String black = getString(blackKey, "");
    
    boolean effective;
    if (isWhite) {
        // 白名单项生效条件：白名单非空
        effective = !white.isEmpty();
    } else {
        // 黑名单项生效条件：白名单为空 且 黑名单非空
        effective = white.isEmpty() && !black.isEmpty();
    }
    
    String baseTitle = isPrivate ? (isWhite ? "私聊白名单" : "私聊黑名单") : (isWhite ? "群聊白名单" : "群聊黑名单");
    titleView.setText(effective ? baseTitle + "（生效）" : baseTitle);
}

// ====== 刷新所有选择器行的生效状态 ======
void refreshAllEffectiveStatus(Map<String, TextView> titleViewMap) {
    if (titleViewMap == null) return;
    for (Map.Entry<String, TextView> entry : titleViewMap.entrySet()) {
        String key = entry.getKey();
        TextView titleView = entry.getValue();
        updateEffectiveStatus(titleView, key);
    }
}

// ====== 创建列表选择器行（RelativeLayout版，带 titleViewMap 注册） ======
LinearLayout createListSelectorRow(Context ctx, String title, String desc, String key, boolean dark, Map<String, TextView> titleViewMap) {
    var outerLayout = new LinearLayout(ctx);
    outerLayout.setOrientation(LinearLayout.VERTICAL);
    outerLayout.setPadding(0, 4, 0, 4);
    outerLayout.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ));
    
    // 标题行（RelativeLayout）
    var titleRow = new RelativeLayout(ctx);
    titleRow.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ));
    
    final TextView titleView = new TextView(ctx);
    titleView.setText(title);
    titleView.setTextColor(dark ? Color.WHITE : Color.parseColor("#2C3E50"));
    titleView.setTextSize(14);
    RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        RelativeLayout.LayoutParams.WRAP_CONTENT
    );
    titleParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    titleParams.addRule(RelativeLayout.CENTER_VERTICAL);
    titleView.setLayoutParams(titleParams);
    titleRow.addView(titleView);
    
    // 按钮容器
    var btnContainer = new LinearLayout(ctx);
    btnContainer.setOrientation(LinearLayout.HORIZONTAL);
    btnContainer.setGravity(Gravity.CENTER_VERTICAL);
    RelativeLayout.LayoutParams btnContainerParams = new RelativeLayout.LayoutParams(
        RelativeLayout.LayoutParams.WRAP_CONTENT,
        120  // 固定高度120
    );
    btnContainerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    btnContainerParams.addRule(RelativeLayout.CENTER_VERTICAL);
    btnContainer.setLayoutParams(btnContainerParams);
    
    // 选择按钮
    var selectBtn = new Button(ctx);
    selectBtn.setText("选择");
    selectBtn.setTextSize(15);
    selectBtn.setTextColor(Color.WHITE);
    selectBtn.setPadding(36, 0, 36, 0);
    selectBtn.setBackground(createRoundedDrawable("#3498DB", 8));
    selectBtn.setMinHeight(0);
    selectBtn.setMinimumHeight(0);
    LinearLayout.LayoutParams selectParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, 120);
    selectParams.setMargins(0, 0, 12, 0);
    selectBtn.setLayoutParams(selectParams);
    btnContainer.addView(selectBtn);
    
    // 清空按钮
    var clearBtn = new Button(ctx);
    clearBtn.setText("清空");
    clearBtn.setTextSize(15);
    clearBtn.setTextColor(Color.WHITE);
    clearBtn.setPadding(36, 0, 36, 0);
    clearBtn.setBackground(createRoundedDrawable("#E74C3C", 8));
    clearBtn.setMinHeight(0);
    clearBtn.setMinimumHeight(0);
    LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, 120);
    clearBtn.setLayoutParams(clearParams);
    btnContainer.addView(clearBtn);
    
    titleRow.addView(btnContainer);
    outerLayout.addView(titleRow);
    
    // 描述文字
    var descView = new TextView(ctx);
    descView.setText(desc);
    descView.setTextColor(dark ? Color.parseColor("#AAAAAA") : Color.parseColor("#7F8C8D"));
    descView.setTextSize(11);
    descView.setPadding(0, 2, 0, 0);
    outerLayout.addView(descView);
    
    // 已选显示
    var selectedLayout = new LinearLayout(ctx);
    selectedLayout.setOrientation(LinearLayout.HORIZONTAL);
    selectedLayout.setPadding(0, 2, 0, 6);
    selectedLayout.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ));
    
    final TextView selectedView = new TextView(ctx);
    selectedView.setText("已选：无");
    selectedView.setTextColor(dark ? Color.WHITE : Color.parseColor("#34495E"));
    selectedView.setTextSize(12);
    selectedView.setGravity(Gravity.CENTER_VERTICAL);
    selectedView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    selectedLayout.addView(selectedView);
    outerLayout.addView(selectedLayout);
    
    // 分割线
    var divider = new View(ctx);
    divider.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 1));
    int dividerColor = dark ? Color.parseColor("#444444") : Color.parseColor("#E0E0E0");
    divider.setBackgroundColor(dividerColor);
    divider.setPadding(0, 6, 0, 0);
    outerLayout.addView(divider);
    
    outerLayout.setTag(selectedView);
    
    // 注册 titleView 到 Map
    if (titleViewMap != null) {
        titleViewMap.put(key, titleView);
    }
    
    // 初始化生效状态
    updateEffectiveStatus(titleView, key);
    updateSelectedDisplay(key, selectedView, ctx);
    
    // 选择按钮
    selectBtn.setOnClickListener((view) -> {
        showListSelector(ctx, key, selectedView, titleView, titleViewMap);
    });
    
    // 清空按钮：清空后刷新所有标题
    clearBtn.setOnClickListener((view) -> {
        putString(key, "");
        selectedView.setText("已选：无");
        toast("已清空");
        refreshAllEffectiveStatus(titleViewMap);
    });
    
    return outerLayout;
}

// ====== 显示列表选择器（纯 LinearLayout + ScrollView，无适配器，无 lambda） ======
// ====== 显示列表选择器对话框（自定义适配器：圆形头像 + 名称(ID) + 复选框） ======
void showListSelector(Context ctx, String key, TextView selectedView, TextView titleView, Map<String, TextView> titleViewMap) {
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
        
        final int count = displayItems.size();
        final String[] displayArray = new String[count];
        final String[] nameArray = new String[count];
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
        
        // ---- ListView卡片 ----
        var listCard = new LinearLayout(ctx);
        listCard.setOrientation(LinearLayout.VERTICAL);
        var cardBg = new android.graphics.drawable.GradientDrawable();
        cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(8);
        cardBg.setColor(dark ? Color.parseColor("#2C2C2C") : Color.WHITE);
        listCard.setBackground(cardBg);
        listCard.setPadding(10, 5, 10, 5);
        var listParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            0
        );
        listParams.addRule(RelativeLayout.BELOW, btnLayoutId);
        int listCardId = View.generateViewId();
        listCard.setId(listCardId);
        listCard.setLayoutParams(listParams);
        
        final ListView listView = new ListView(ctx);
        listView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ));
        listView.setBackgroundColor(dark ? Color.parseColor("#2C2C2C") : Color.WHITE);
        listView.setCacheColorHint(0);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSelector(android.R.drawable.list_selector_background);
        
        // 初始化头像目录
        initAvatarDir();
        
        // ---- 自定义适配器：头像 + 名称 + 复选框 ----
        final float density = ctx.getResources().getDisplayMetrics().density;
        final int avatarDp = (int)(40 * density + 0.5f);
        
        android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
            @Override public int getCount() { return count; }
            @Override public Object getItem(int pos) { return displayArray[pos]; }
            @Override public long getItemId(int pos) { return pos; }
            @Override
            public android.view.View getView(int pos, android.view.View convertView, android.view.ViewGroup parent) {
                android.widget.LinearLayout row;
                android.widget.ImageView avatarView;
                android.widget.CheckedTextView textView;
                
                if (convertView == null) {
                    row = new android.widget.LinearLayout(ctx);
                    row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding((int)(12 * density), (int)(6 * density), (int)(12 * density), (int)(6 * density));
                    
                    avatarView = new android.widget.ImageView(ctx);
                    avatarView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    android.widget.LinearLayout.LayoutParams avParams = new android.widget.LinearLayout.LayoutParams(avatarDp, avatarDp);
                    avParams.rightMargin = (int)(12 * density);
                    avatarView.setLayoutParams(avParams);
                    avatarView.setId(View.generateViewId());
                    row.addView(avatarView);
                    
                    textView = new android.widget.CheckedTextView(ctx);
                    textView.setTextSize(15);
                    textView.setPadding((int)(8 * density), 0, 0, 0);
                    textView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                    row.addView(textView);
                    
                    row.setTag(new Object[]{avatarView, textView});
                } else {
                    row = (android.widget.LinearLayout) convertView;
                    Object[] tags = (Object[]) row.getTag();
                    avatarView = (android.widget.ImageView) tags[0];
                    textView = (android.widget.CheckedTextView) tags[1];
                }
                
                textView.setText(displayArray[pos]);
                textView.setChecked(listView.isItemChecked(pos));
                textView.setTextColor(textColor);
                
                loadItemAvatar(avatarView, idArray[pos], contactArray[pos]);
                
                return row;
            }
        };
        
        listView.setAdapter(adapter);
        for (int i = 0; i < finalChecked.length; i++) {
            listView.setItemChecked(i, finalChecked[i]);
        }
        
        listCard.addView(listView);
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
        var builder = new AlertDialog.Builder(ctx)
            .setView(rootLayout);
        var dialog = builder.create();
        dialog.show();
        
        var window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(contentBgColor));
            int width = (int)(screenWidth * 0.92);
            int height = (int)(screenHeight * 0.85);
            window.setLayout(width, height);
            window.setGravity(Gravity.CENTER);
        }
        
        // ---- 事件绑定 ----
        confirmBtn.setOnClickListener((v) -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < listView.getCount(); i++) {
                    if (listView.isItemChecked(i)) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(nameArray[i]);
                    }
                }
                putString(key, sb.toString());
                updateSelectedDisplay(key, selectedView, ctx);
                refreshAllEffectiveStatus(titleViewMap);
                toast("保存成功");
                dialog.dismiss();
            } catch (Exception e) {
                toast("保存失败：" + e.getMessage());
                log("[TTS]保存列表异常：" + e.toString());
            }
        });
        
        cancelBtn.setOnClickListener((v) -> {
            try { dialog.dismiss(); } catch (Exception e) {}
        });
        
        selectAllBtn.setOnClickListener((v) -> {
            try {
                for (int i = 0; i < listView.getCount(); i++) {
                    listView.setItemChecked(i, true);
                }
            } catch (Exception e) { log("[TTS]全选异常：" + e.toString()); }
        });
        
        invertBtn.setOnClickListener((v) -> {
            try {
                for (int i = 0; i < listView.getCount(); i++) {
                    listView.setItemChecked(i, !listView.isItemChecked(i));
                }
            } catch (Exception e) { log("[TTS]反选异常：" + e.toString()); }
        });
        
        clearAllBtn.setOnClickListener((v) -> {
            try {
                for (int i = 0; i < listView.getCount(); i++) {
                    listView.setItemChecked(i, false);
                }
            } catch (Exception e) { log("[TTS]清空异常：" + e.toString()); }
        });
        
        searchBtn.setOnClickListener((v) -> {
            try {
                String keyword = searchEdit.getText().toString().trim().toLowerCase();
                List<String> filteredDisplay = new ArrayList();
                List<String> filteredNames = new ArrayList();
                List<String> filteredIds = new ArrayList();
                List<Object> filteredContacts = new ArrayList();
                for (int i = 0; i < displayArray.length; i++) {
                    if (keyword.isEmpty() || displayArray[i].toLowerCase().contains(keyword)) {
                        filteredDisplay.add(displayArray[i]);
                        filteredNames.add(nameArray[i]);
                        filteredIds.add(idArray[i]);
                        filteredContacts.add(contactArray[i]);
                    }
                }
                final int fCount = filteredDisplay.size();
                final String[] newDisplay = new String[fCount];
                filteredDisplay.toArray(newDisplay);
                final String[] newNames = new String[fCount];
                filteredNames.toArray(newNames);
                final String[] newIds = new String[fCount];
                filteredIds.toArray(newIds);
                final Object[] newContacts = new Object[fCount];
                filteredContacts.toArray(newContacts);
                
                android.widget.BaseAdapter newAdapter = new android.widget.BaseAdapter() {
                    @Override public int getCount() { return fCount; }
                    @Override public Object getItem(int pos) { return newDisplay[pos]; }
                    @Override public long getItemId(int pos) { return pos; }
                    @Override
                    public android.view.View getView(int pos, android.view.View convertView, android.view.ViewGroup parent) {
                        android.widget.LinearLayout row;
                        android.widget.ImageView avatarView;
                        android.widget.CheckedTextView tv;
                        if (convertView == null) {
                            row = new android.widget.LinearLayout(ctx);
                            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                            row.setPadding((int)(12 * density), (int)(6 * density), (int)(12 * density), (int)(6 * density));
                            avatarView = new android.widget.ImageView(ctx);
                            avatarView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                            avatarView.setLayoutParams(new android.widget.LinearLayout.LayoutParams(avatarDp, avatarDp));
                            ((android.widget.LinearLayout.LayoutParams)avatarView.getLayoutParams()).rightMargin = (int)(12 * density);
                            avatarView.setId(View.generateViewId());
                            row.addView(avatarView);
                            tv = new android.widget.CheckedTextView(ctx);
                            tv.setTextSize(15);
                            tv.setPadding((int)(8 * density), 0, 0, 0);
                            tv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                            row.addView(tv);
                            row.setTag(new Object[]{avatarView, tv});
                        } else {
                            row = (android.widget.LinearLayout) convertView;
                            Object[] tags = (Object[]) row.getTag();
                            avatarView = (android.widget.ImageView) tags[0];
                            tv = (android.widget.CheckedTextView) tags[1];
                        }
                        tv.setText(newDisplay[pos]);
                        tv.setChecked(listView.isItemChecked(pos));
                        tv.setTextColor(textColor);
                        loadItemAvatar(avatarView, newIds[pos], newContacts[pos]);
                        return row;
                    }
                };
                listView.setAdapter(newAdapter);
                for (int i = 0; i < newNames.length; i++) {
                    String name = newNames[i];
                    for (int j = 0; j < nameArray.length; j++) {
                        if (nameArray[j].equals(name) && finalChecked[j]) {
                            listView.setItemChecked(i, true);
                            break;
                        }
                    }
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

// ====== 消息监听 ======
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
