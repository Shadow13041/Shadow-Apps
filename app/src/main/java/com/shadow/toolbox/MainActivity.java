package com.shadow.toolbox;

import java.security.SecureRandom;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {
    private static final int REQ_OPEN_FILE = 1001;
    private static final int REQ_OPEN_FOLDER = 1002;
    private static final int REQ_DEVICE_AUTH = 1003;

    private LinearLayout content;
    private LinearLayout drawer;
    private TextView toolbarTitle;
    private TextView pageSubtitle;
    private View drawerScrim;
    private SecureStore secureStore;
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newCachedThreadPool();
    private int appInfoTaps = 0;
    private long lastAppInfoTap = 0L;
    private final List<Rule> rules = new ArrayList<>();
    private final List<String> studioFiles = new ArrayList<>();

    private final int BG = 0xFF000000;
    private final int SURFACE = 0xFF202632;
    private final int SURFACE2 = 0xFF1A202B;
    private final int TEXT = 0xFFF0F3F8;
    private final int MUTED = 0xFFA8B2C2;
    private final int ACCENT = 0xFF45C4FF;
    private final int SUCCESS = 0xFF70D6A3;
    private final int WARNING = 0xFFF1C76A;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("shadow", MODE_PRIVATE);
        secureStore = new SecureStore(this);
        ensureDirs();
        buildShell();
        showPage("Developer Infos");
    }

    private void ensureDirs() {
        new File(getFilesDir(), "apk_store").mkdirs();
        new File(getFilesDir(), "vault").mkdirs();
        new File(getFilesDir(), "workspace").mkdirs();
        new File(getFilesDir(), "remote").mkdirs();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(16), dp(10), dp(16), dp(10));
        toolbar.setBackgroundColor(BG);

        TextView hamburger = iconButton("☰", 26);
        hamburger.setOnClickListener(v -> openDrawer());
        toolbar.addView(hamburger, lp(dp(46), dp(46)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        toolbar.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        toolbarTitle = text("Shadow Toolbox", 20, TEXT, true);
        pageSubtitle = text("Developer Infos", 12, MUTED, false);
        titleBox.addView(toolbarTitle);
        titleBox.addView(pageSubtitle);

        TextView status = text("●", 14, SUCCESS, true);
        status.setGravity(Gravity.CENTER);
        toolbar.addView(status, lp(dp(38), dp(46)));
        root.addView(toolbar);

        FrameContainer fc = new FrameContainer(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), 0, dp(14), dp(28));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        fc.addView(scroll, new ViewGroup.LayoutParams(-1, -1));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(0x99000000);
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closeDrawer());
        fc.addView(drawerScrim, new ViewGroup.LayoutParams(-1, -1));

        drawer = new LinearLayout(this);
        drawer.setOrientation(LinearLayout.VERTICAL);
        drawer.setPadding(dp(18), dp(54), dp(12), dp(20));
        drawer.setBackgroundColor(SURFACE);
        buildDrawer();
        FrameContainer.LayoutParams drawerParams = new FrameContainer.LayoutParams(dp(310), -1);
        drawerParams.gravity = Gravity.START;
        drawer.setTranslationX(-dp(320));
        fc.addView(drawer, drawerParams);

        root.addView(fc, new LinearLayout.LayoutParams(-1, 0, 1));
        com.shadow.apps.ui.SystemBars.apply(root);
        setContentView(root);
    }

    private void buildDrawer() {
        TextView brand = text("SHADOW", 12, ACCENT, true);
        drawer.addView(brand, lp(-1, dp(32)));
        TextView caption = text("Toolbox", 26, TEXT, true);
        drawer.addView(caption, lp(-1, dp(48)));

        String[] sections = {
                "Developer Infos", "Shadow AI", "Shadow Files", "Developer Tools", "Shadow Studio",
                "Shadow Network", "Shadow Automation", "Shadow Terminal", "Shadow Remote", "Shadow Update Center",
                "Shadow App Management", "Shadow Virtual Environment", "VPN", "Proxy", "About / App Info"
        };
        for (String section : sections) addDrawerItem(section);
    }

    private void addDrawerItem(String name) {
        TextView item = text(name, 15, TEXT, false);
        item.setPadding(dp(14), 0, dp(8), 0);
        item.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.GradientDrawable itemBg = roundBg(SURFACE2, 14);
        itemBg.setStroke(dp(1), 0xFF303B4B);
        item.setBackground(itemBg);
        item.setOnClickListener(v -> {
            closeDrawer();
            if (name.equals("About / App Info")) showPage("About / App Info"); else showPage(name);
        });
        LinearLayout.LayoutParams p = lp(-1, dp(44));
        p.topMargin = dp(5);
        drawer.addView(item, p);
    }

    private void openDrawer() {
        drawerScrim.setVisibility(View.VISIBLE);
        drawer.animate().translationX(0).setDuration(190).start();
    }

    private void closeDrawer() {
        drawer.animate().translationX(-dp(320)).setDuration(170).withEndAction(() -> drawerScrim.setVisibility(View.GONE)).start();
    }

    private void showPage(String page) {
        toolbarTitle.setText("Shadow Toolbox");
        pageSubtitle.setText(page);
        content.removeAllViews();
        animateIn(content);
        switch (page) {
            case "Developer Infos": buildDeveloperInfo(); break;
            case "Shadow AI": buildShadowAI(); break;
            case "Shadow Files": buildShadowFiles(); break;
            case "Developer Tools": buildDeveloperTools(); break;
            case "Shadow Studio": buildStudio(); break;
            case "Shadow Network": buildNetwork(); break;
            case "Shadow Automation": buildAutomation(); break;
            case "Shadow Terminal": buildTerminal(); break;
            case "Shadow Remote": buildRemote(); break;
            case "Shadow Update Center": buildUpdateCenter(); break;
            case "Shadow App Management": buildAppManagement(); break;
            case "Shadow Virtual Environment": buildVirtualEnvironment(); break;
            case "VPN": buildVpn(); break;
            case "Proxy": buildProxy(); break;
            case "About / App Info": buildAbout(); break;
        }
    }

    private void buildDeveloperInfo() {
        content.addView(section("Dashboard / Developer Infos", "Ein kompakter Überblick über das aktuelle Gerät."));
        card("Gerät", infoRows(
                row("Hersteller", Build.MANUFACTURER),
                row("Modell", Build.MODEL),
                row("Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"),
                row("CPU ABI", Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0 ? String.join(", ", Build.SUPPORTED_ABIS) : "unbekannt"),
                row("Build", Build.DISPLAY),
                row("Security Patch", Build.VERSION.SECURITY_PATCH)
        ));
        card("Akku", infoRows(row("Ladestand", batteryPercent() + "%"), row("Status", batteryStatus())));
        card("Speicher", infoRows(storageRow("Intern", Environment.getDataDirectory())));
        card("Display", infoRows(displayRows()));
        card("Netzwerk", infoRows(networkRows()));
        card("Hardware / Systeme", infoRows(
                row("Bluetooth", hasBluetooth() ? "Dienst vorhanden" : "nicht erkannt"),
                row("NFC", getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) ? "vorhanden" : "nicht vorhanden"),
                row("Kamera", getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ? "vorhanden" : "nicht vorhanden"),
                row("Sensoren", String.valueOf(getSensorCount()))
        ));
        Button diagnostics = button("System Diagnosis öffnen");
        diagnostics.setOnClickListener(v -> showPage("Developer Tools"));
        content.addView(diagnostics, bottomLp());
    }

    private void buildShadowAI() {
        content.addView(section("Shadow AI", "Provider, Modelle, Profil und Chat-Kontext bleiben lokal verwaltbar."));
        LinearLayout providerCard = (LinearLayout) card("Provider", new LinearLayout(this));
        providerCard.addView(text("Mindestens ein API-Key muss von dir hinterlegt werden. Keine Keys sind fest eingebaut.", 13, MUTED, false));
        providerCard.addView(space(8));
        String[] providers = {"Gemini", "OpenAI", "Claude", "Groq"};
        for (String provider : providers) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(provider, 15, TEXT, true);
            line.addView(label, new LinearLayout.LayoutParams(0, dp(45), 1));
            Button edit = smallButton(secureStore.get("ai_key_" + provider.toLowerCase(Locale.US), "").isEmpty() ? "Key setzen" : "Key gespeichert");
            edit.setOnClickListener(v -> promptSecret("API-Key: " + provider, "ai_key_" + provider.toLowerCase(Locale.US)));
            line.addView(edit);
            providerCard.addView(line);
        }
        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.VERTICAL);
        EditText model = input("Modell-ID, z. B. dein gewünschtes Modell");
        model.setText(prefs.getString("ai_model", ""));
        settings.addView(labelled("Standardmodell", model));
        EditText endpoint = input("Optionale API-Endpunkt-URL");
        endpoint.setText(prefs.getString("ai_endpoint", ""));
        settings.addView(labelled("Endpoint", endpoint));
        Button save = button("AI-Konfiguration speichern");
        save.setOnClickListener(v -> {
            prefs.edit().putString("ai_model", model.getText().toString().trim()).putString("ai_endpoint", endpoint.getText().toString().trim()).apply();
            toast("AI-Konfiguration gespeichert");
        });
        settings.addView(save);
        card("AI-Einstellungen", settings);

        LinearLayout chat = new LinearLayout(this);
        chat.setOrientation(LinearLayout.VERTICAL);
        EditText prompt = input("Nachricht an Shadow AI");
        prompt.setMinLines(4);
        prompt.setGravity(Gravity.TOP | Gravity.START);
        chat.addView(prompt);
        Button send = button("Anfrage vorbereiten");
        TextView result = text("Noch keine Anfrage.", 13, MUTED, false);
        send.setOnClickListener(v -> result.setText("Anfrage gespeichert. Die konkrete Provider-Ausführung benötigt einen konfigurierten API-Endpunkt und Schlüssel."));
        chat.addView(send);
        chat.addView(result);
        card("Chat", chat);

        Button profile = smallButton("Profil / Erinnerungen");
        profile.setOnClickListener(v -> promptText("Shadow AI Profil", "Name, Präferenzen, Kontext – lokal gespeichert", "ai_profile"));
        content.addView(profile, bottomLp());
    }

    private void buildShadowFiles() {
        content.addView(section("Shadow Files", "Dateien, APK-Bibliothek und Shadow Vault in einer lokalen Oberfläche."));
        LinearLayout files = new LinearLayout(this);
        files.setOrientation(LinearLayout.VERTICAL);
        Button folder = button("Geräteordner auswählen");
        folder.setOnClickListener(v -> chooseFolder());
        files.addView(folder);
        Button importFile = button("Datei importieren");
        importFile.setOnClickListener(v -> chooseGenericFile());
        files.addView(importFile);
        Button vault = button("Shadow Vault öffnen");
        vault.setOnClickListener(v -> buildVaultDialog());
        files.addView(vault);
        card("Dateien", files);

        LinearLayout apk = new LinearLayout(this);
        apk.setOrientation(LinearLayout.VERTICAL);
        Button importApk = button("APK in Shadow Apps importieren");
        importApk.setOnClickListener(v -> chooseApk());
        apk.addView(importApk);
        apk.addView(text("APK-Dateien bleiben in Shadow Toolbox gespeichert und werden nicht normal installiert. Die aktuelle Version dieses Projekts implementiert Import, Analyse und Verwaltung; die tatsächliche Ausführung eingebetteter Apps hängt von der später integrierten Virtualisierungs-Engine ab.", 13, MUTED, false));
        card("Shadow Apps / APK-Bibliothek", apk);
        apkListCard();
    }

    private View apkListCard() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        File dir = new File(getFilesDir(), "apk_store");
        File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.US).endsWith(".apk"));
        if (files == null || files.length == 0) {
            box.addView(text("Noch keine APKs gespeichert.", 13, MUTED, false));
        } else {
            for (File f : files) {
                PackageInfo pi = getArchiveInfo(f);
                String label = pi == null ? f.getName() : String.valueOf(pi.applicationInfo.loadLabel(getPackageManager()));
                TextView t = text(label + "\n" + f.getName() + (pi == null ? "" : "\n" + pi.packageName + " · " + version(pi)), 14, TEXT, false);
                t.setPadding(dp(8), dp(10), dp(8), dp(10));
                android.graphics.drawable.GradientDrawable apkBg = roundBg(SURFACE, 14);
                apkBg.setStroke(dp(1), 0xFF303B4B);
                t.setBackground(apkBg);
                box.addView(t, topMarginLp(dp(6)));
            }
        }
        return card("Gespeicherte APKs", box);
    }

    private void buildVaultDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(14), dp(18), dp(10));
        layout.addView(text("Shadow Vault schützt lokale Dateien mit AES-GCM und einem Schlüssel aus Android Keystore.", 13, MUTED, false));
        Button auth = smallButton("Gerätesperre bestätigen");
        auth.setOnClickListener(v -> authenticateVault());
        layout.addView(auth, topMarginLp(dp(10)));
        Button add = smallButton("Datei verschlüsselt hinzufügen");
        add.setOnClickListener(v -> { chooseVaultFile(); });
        layout.addView(add, topMarginLp(dp(8)));
        File dir = new File(getFilesDir(), "vault");
        File[] entries = dir.listFiles();
        StringBuilder list = new StringBuilder("\nGespeicherte Vault-Einträge:\n");
        if (entries != null && entries.length > 0) for (File f : entries) list.append("• ").append(f.getName()).append("\n"); else list.append("Noch leer\n");
        layout.addView(text(list.toString(), 13, TEXT, false));
        b.setTitle("Shadow Vault").setView(layout).setNegativeButton("Schließen", null).show();
    }

    private void authenticateVault() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardSecure()) {
            Intent intent = km.createConfirmDeviceCredentialIntent("Shadow Vault", "Gerätesperre bestätigen");
            if (intent != null) startActivityForResult(intent, REQ_DEVICE_AUTH);
        } else {
            toast("Auf diesem Gerät ist keine sichere Gerätesperre eingerichtet.");
        }
    }

    private void buildDeveloperTools() {
        content.addView(section("Developer Tools", "Große Diagnose- und Analysewerkzeuge – zentral von hier aus erreichbar."));
        addToolCard("System Diagnosis", "Schnellprüfung von Speicher, Akku, Netzwerk, WebView und App-Zustand.", v -> runDiagnosis());
        addToolCard("Live Monitor", "Live-Ansicht für Akku, Speicher und Prozess-Ressourcen der Toolbox.", v -> buildLiveMonitor());
        addToolCard("App Inspector", "Installierte Apps nach Paketname, Version und Startbarkeit untersuchen.", v -> showPage("Shadow App Management"));
        addToolCard("Permission Center", "Vom System erlaubte Berechtigungen und relevante Settings-Zugänge öffnen.", v -> openAppDetails());
        addToolCard("Network Tools", "IP, DNS, aktive Schnittstelle und Erreichbarkeit prüfen.", v -> showPage("Shadow Network"));
        addToolCard("Storage Analyzer", "Große lokale Bereiche und Shadow-Daten sichtbar machen.", v -> storageAnalyzer());
        addToolCard("Device Profile", "Geräteprofil als lesbaren Diagnose-Snapshot erstellen.", v -> deviceProfile());
    }

    private void buildStudio() {
        content.addView(section("Shadow Studio", "Leichter lokaler Editor für Text, JSON, XML und Projektdateien."));
        File dir = new File(getFilesDir(), "workspace");
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) addEditorFile(f);
        Button newFile = button("Neue Datei");
        newFile.setOnClickListener(v -> promptText("Neue Datei", "z. B. notes.txt", "studio_new_file"));
        content.addView(newFile, bottomLp());
        TextView integration = text("Integration: Shadow Files für Dateien, Shadow Terminal für Befehle, Shadow AI für kontextbezogene Hilfe.", 13, MUTED, false);
        content.addView(integration, bottomLp());
    }

    private void addEditorFile(File file) {
        Button b = smallButton(file.getName());
        b.setOnClickListener(v -> openEditor(file));
        content.addView(b, topMarginLp(dp(6)));
    }

    private void openEditor(File file) {
        String data = readText(file);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText editor = input("");
        editor.setTypeface(android.graphics.Typeface.MONOSPACE);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setMinLines(18);
        editor.setText(data);
        layout.addView(editor, new LinearLayout.LayoutParams(-1, 0, 1));
        Button save = button("Speichern");
        save.setOnClickListener(v -> { writeText(file, editor.getText().toString()); toast("Gespeichert"); });
        layout.addView(save);
        new AlertDialog.Builder(this).setTitle(file.getName()).setView(layout).setNegativeButton("Schließen", null).show();
    }

    private void buildNetwork() {
        content.addView(section("Shadow Network", "Netzwerkdiagnose ohne eigenen Netz-Provider."));
        card("Aktive Verbindung", infoRows(networkRows()));
        EditText host = input("Host für Erreichbarkeitstest, z. B. example.com");
        host.setText("example.com");
        Button ping = button("Verbindung testen");
        TextView out = text("Noch kein Test.", 13, MUTED, false);
        ping.setOnClickListener(v -> executor.execute(() -> {
            boolean ok = false;
            try { ok = InetAddress.getByName(host.getText().toString().trim()).isReachable(2500); } catch (Exception ignored) {}
            boolean finalOk = ok;
            runOnUiThread(() -> out.setText(finalOk ? "Erreichbar." : "Keine Erreichbarkeit innerhalb des Tests festgestellt."));
        }));
        card("Network Tools", stack(host, ping, out));
        content.addView(text("VPN und Proxy sind separate Shadow-Module. Shadow Toolbox stellt keinen eigenen Internet-Provider bereit.", 13, MUTED, false), topMarginLp(dp(12)));
    }

    private void buildAutomation() {
        content.addView(section("Shadow Automation", "If/then-Regeln und lokale Konfiguration. Android legt Grenzen für dauerhaft autonome Hintergrundaktionen fest."));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = input("Regelname");
        EditText when = input("Bedingung, z. B. Akku < 20 %");
        EditText then = input("Aktion, z. B. Energiesparansicht öffnen");
        Button add = button("Regel speichern");
        TextView list = text("Noch keine Regeln.", 13, MUTED, false);
        add.setOnClickListener(v -> {
            Rule r = new Rule(name.getText().toString().trim(), when.getText().toString().trim(), then.getText().toString().trim());
            rules.add(r); saveRules(); refreshRuleText(list);
            name.setText(""); when.setText(""); then.setText("");
        });
        box.addView(name); box.addView(when); box.addView(then); box.addView(add); box.addView(list);
        card("Regel-Editor", box);
        Button settings = smallButton("Android-Automations-/Berechtigungsseiten öffnen");
        settings.setOnClickListener(v -> openSpecialSettings());
        content.addView(settings, bottomLp());
    }

    private void buildTerminal() {
        content.addView(section("Shadow Terminal", "Lokaler Shell-Zugriff mit den Rechten der Shadow-Toolbox-App. Kein Root wird vorausgesetzt."));
        EditText cmd = input("Befehl, z. B. getprop | head");
        cmd.setText("getprop | head -n 20");
        EditText output = input("");
        output.setTypeface(android.graphics.Typeface.MONOSPACE);
        output.setMinLines(16);
        output.setGravity(Gravity.TOP | Gravity.START);
        output.setTextIsSelectable(true);
        Button run = button("Ausführen");
        run.setOnClickListener(v -> executeShell(cmd.getText().toString(), output));
        content.addView(cmd); content.addView(run); content.addView(output);
        TextView hint = text("Hinweis: Android beschränkt Shell-Zugriffe. Befehle laufen unter der App-UID und haben keinen Root-Zugriff.", 12, MUTED, false);
        content.addView(hint, bottomLp());
    }

    private void buildRemote() {
        content.addView(section("Shadow Remote", "Verbindungs- und Freigabeschicht für andere Geräte. Shadow Toolbox enthält hier noch keinen eigenen Relay-Server."));
        String deviceId = prefs.getString("remote_id", null);
        if (deviceId == null) { deviceId = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US); prefs.edit().putString("remote_id", deviceId).apply(); }
        card("Dieses Gerät", infoRows(row("Geräte-ID", deviceId), row("Status", "Nicht verbunden"), row("Zugriff", "Manuell freigeben")));
        EditText peer = input("Geräte-ID des Partners");
        Button pair = button("Gerät koppeln");
        TextView result = text("Noch kein Peer gekoppelt.", 13, MUTED, false);
        pair.setOnClickListener(v -> { prefs.edit().putString("peer_id", peer.getText().toString().trim()).apply(); result.setText("Peer gespeichert. Eine echte Ende-zu-Ende-Verbindung benötigt noch ein Transport-Backend." ); });
        card("Pairing", stack(peer, pair, result));
        TextView security = text("Sicherheitsregeln für die spätere Transportphase: explizite Geräteautorisierung, Verschlüsselung, einzelne Rechte, sichtbarer Remote-Status, sofortige Trennung.", 13, TEXT, false);
        card("Sicherheit", security);
    }

    private void buildUpdateCenter() {
        content.addView(section("Shadow Update Center", "Lokale Update-Verwaltung. Keine verpflichtenden Online-Updates."));
        String current = getVersionName();
        card("Shadow Toolbox", infoRows(row("Version", current), row("Package", getPackageName()), row("Update-Modus", "lokale APK-Prüfung")));
        Button importUpdate = button("Lokale APK prüfen");
        TextView out = text("Keine Datei geprüft.", 13, MUTED, false);
        importUpdate.setOnClickListener(v -> chooseUpdateApk(out));
        content.addView(stack(importUpdate, out));
    }

    private void buildAppManagement() {
        content.addView(section("Shadow App Management", "Installierte Android-Apps analysieren und ihre offiziellen App-Einstellungen öffnen."));
        Button refresh = smallButton("Liste aktualisieren");
        refresh.setOnClickListener(v -> { showPage("Shadow App Management"); });
        content.addView(refresh);
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(apps, Comparator.comparing(a -> String.valueOf(a.loadLabel(pm)).toLowerCase(Locale.US)));
        int count = 0;
        for (ApplicationInfo app : apps) {
            if (getPackageName().equals(app.packageName)) continue;
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && count > 120) continue;
            String title = String.valueOf(app.loadLabel(pm));
            TextView row = text(title + "\n" + app.packageName, 14, TEXT, false);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            android.graphics.drawable.GradientDrawable appBg = roundBg(SURFACE, 14);
            appBg.setStroke(dp(1), 0xFF303B4B);
            row.setBackground(appBg);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> appActions(app.packageName, title));
            content.addView(row, topMarginLp(dp(8)));
            count++;
        }
    }

    private void buildVirtualEnvironment() {
        content.addView(section("Shadow App Container", "Die APK-Bibliothek bleibt getrennt vom normalen Android-Installationsbereich."));
        card("Aktueller Stand", text("Import, Analyse, Dateispeicher und Verwaltungsoberfläche sind vorbereitet. Die tatsächliche Ausführung nicht installierter APKs ist der technisch schwerste Teil und wird erst nach Einbindung einer passenden Virtualisierungs-Engine aktiviert.", 13, TEXT, false));
        addToolCard("APK analysieren", "ABI, Paketname, Version und native Bibliotheken prüfen.", v -> showPage("Shadow Files"));
        addToolCard("Container-Einstellungen", "Pro-App Speicher, Netzwerk, Benachrichtigungen und Lebenszyklus als Zielarchitektur.", v -> containerSettings());
        addToolCard("Kompatibilitätsprofil", "Android-Version, ABI und erkannte Abhängigkeiten anzeigen.", v -> showPage("Shadow Files"));
    }

    private void buildVpn() {
        content.addView(section("VPN", "Shadow Toolbox ist Konfigurations- und Verbindungsclient, kein VPN-Provider."));
        EditText address = input("VPN-Server / Host");
        EditText username = input("Benutzername (optional)");
        EditText password = input("Passwort (optional)");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button save = button("Konfiguration speichern");
        TextView state = text("Keine Verbindung aktiv.", 13, MUTED, false);
        save.setOnClickListener(v -> { secureStore.put("vpn_password", password.getText().toString()); prefs.edit().putString("vpn_host", address.getText().toString()).putString("vpn_user", username.getText().toString()).apply(); state.setText("Konfiguration gespeichert. Zum echten Tunnel wird ein kompatibles VPN-Profil/Backend benötigt."); });
        Button system = smallButton("Android-VPN-Einstellungen öffnen");
        system.setOnClickListener(v -> { try { startActivity(new Intent(Settings.ACTION_VPN_SETTINGS)); } catch (Exception e) { toast("Einstellungen nicht verfügbar"); } });
        card("Verbindung", stack(address, username, password, save, state, system));
    }

    private void buildProxy() {
        content.addView(section("Proxy", "Konfiguration für einen vorhandenen Proxy-Server. Shadow Toolbox selbst ist kein Proxy-Provider."));
        EditText host = input("Proxy Host");
        EditText port = input("Port");
        EditText user = input("Benutzername (optional)");
        EditText pass = input("Passwort (optional)");
        Button save = button("Proxy-Konfiguration speichern");
        TextView state = text("Noch nicht gespeichert.", 13, MUTED, false);
        save.setOnClickListener(v -> { secureStore.put("proxy_password", pass.getText().toString()); prefs.edit().putString("proxy_host", host.getText().toString()).putString("proxy_port", port.getText().toString()).putString("proxy_user", user.getText().toString()).apply(); state.setText("Proxy-Konfiguration gespeichert."); });
        card("Proxy", stack(host, port, user, pass, save, state));
        content.addView(text("Ein normaler Android-App-Prozess kann den systemweiten Proxy nicht beliebig für jede andere App erzwingen. Die UI hält deshalb Konfiguration und später mögliche System-/VPN-Integrationen getrennt.", 12, MUTED, false), topMarginLp(dp(10)));
    }

    private void buildAbout() {
        content.addView(section("About / App Info", "Shadow Toolbox – private, lokale Entwickler- und Gerätesuite."));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView version = text("Version " + getVersionName(), 16, TEXT, true);
        info.addView(version);
        info.addView(text("Package: " + getPackageName(), 13, MUTED, false));
        TextView secret = text("App Info", 14, TEXT, true);
        secret.setPadding(dp(10), dp(14), dp(10), dp(14));
        secret.setBackground(roundBg(SURFACE2, 12));
        secret.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            if (now - lastAppInfoTap > 1100) appInfoTaps = 0;
            lastAppInfoTap = now;
            appInfoTaps++;
            if (appInfoTaps >= 3) { appInfoTaps = 0; showHiddenAppMenu(); }
        });
        info.addView(secret, topMarginLp(dp(10)));
        Button details = button("Android App-Info öffnen");
        details.setOnClickListener(v -> openAppDetails());
        info.addView(details);
        card("Shadow", info);
        content.addView(text("Hinweis: Das versteckte App Menu wird bewusst nicht in der normalen Navigation angezeigt.", 12, MUTED, false), bottomLp());
    }

    private void showHiddenAppMenu() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(12), dp(18), dp(8));
        Button browser = button("Shadow Browser");
        browser.setOnClickListener(v -> { buildBrowser(); });
        Button apk = button("APK Launcher");
        apk.setOnClickListener(v -> chooseApk());
        box.addView(browser);
        box.addView(apk, topMarginLp(dp(8)));
        new AlertDialog.Builder(this).setTitle("Shadow App Menu").setView(box).setNegativeButton("Schließen", null).show();
    }

    private void buildBrowser() {
        content.removeAllViews();
        pageSubtitle.setText("Shadow Browser");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText url = input("https://example.com");
        url.setText(prefs.getString("browser_url", "https://example.com"));
        Button go = button("Öffnen");
        WebView web = new WebView(this);
        web.setWebViewClient(new WebViewClient());
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(false);
        web.setBackgroundColor(BG);
        go.setOnClickListener(v -> {
            String u = url.getText().toString().trim();
            if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://" + u;
            prefs.edit().putString("browser_url", u).apply();
            web.loadUrl(u);
        });
        box.addView(url); box.addView(go); box.addView(web, new LinearLayout.LayoutParams(-1, dp(480)));
        content.addView(box);
    }

    private void runDiagnosis() {
        StringBuilder s = new StringBuilder();
        s.append("Shadow Toolbox Diagnose\n\n");
        s.append("Android: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append("\n");
        s.append("ABI: ").append(Build.SUPPORTED_ABIS.length > 0 ? String.join(", ", Build.SUPPORTED_ABIS) : "?").append("\n");
        s.append("Akku: ").append(batteryPercent()).append("%\n");
        s.append("Speicher frei: ").append(formatBytes(freeStorage())).append("\n");
        s.append("Netzwerk: ").append(networkType()).append("\n");
        s.append("WebView: ").append(webViewPackage()).append("\n");
        new AlertDialog.Builder(this).setTitle("System Diagnosis").setMessage(s.toString()).setPositiveButton("OK", null).show();
    }

    private void buildLiveMonitor() {
        content.removeAllViews();
        pageSubtitle.setText("Live Monitor");
        TextView live = text("", 16, TEXT, false);
        content.addView(section("Live Monitor", "Automatisch aktualisiert."));
        card("Live-Daten", live);
        Runnable update = new Runnable() {
            @Override public void run() {
                live.setText("Zeit: " + new java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new java.util.Date()) +
                        "\nAkku: " + batteryPercent() + "%" +
                        "\nFreier Speicher: " + formatBytes(freeStorage()) +
                        "\nProzess-PID: " + android.os.Process.myPid() +
                        "\nHeap: " + formatBytes(Runtime.getRuntime().totalMemory()) +
                        " / " + formatBytes(Runtime.getRuntime().maxMemory()));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(update);
        Button close = button("Zurück zu Developer Tools");
        close.setOnClickListener(v -> { handler.removeCallbacksAndMessages(null); showPage("Developer Tools"); });
        content.addView(close, bottomLp());
    }

    private void storageAnalyzer() {
        File base = getFilesDir();
        File apk = new File(base, "apk_store");
        File vault = new File(base, "vault");
        File workspace = new File(base, "workspace");
        String msg = "Shadow intern\nAPK Store: " + formatBytes(folderSize(apk)) + "\nVault: " + formatBytes(folderSize(vault)) + "\nWorkspace: " + formatBytes(folderSize(workspace)) + "\nGesamt: " + formatBytes(folderSize(base));
        new AlertDialog.Builder(this).setTitle("Storage Analyzer").setMessage(msg).setPositiveButton("OK", null).show();
    }

    private void deviceProfile() {
        String json = "";
        try {
            JSONObject o = new JSONObject();
            o.put("manufacturer", Build.MANUFACTURER);
            o.put("model", Build.MODEL);
            o.put("android", Build.VERSION.RELEASE);
            o.put("api", Build.VERSION.SDK_INT);
            JSONArray abis = new JSONArray();
            for (String a : Build.SUPPORTED_ABIS) abis.put(a);
            o.put("abis", abis);
            o.put("batteryPercent", batteryPercent());
            o.put("freeStorageBytes", freeStorage());
            json = o.toString(2);
        } catch (Exception e) { json = "Profile konnte nicht erstellt werden."; }
        final String finalJson = json;
        new AlertDialog.Builder(this).setTitle("Device Profile").setMessage(finalJson).setPositiveButton("OK", null).show();
    }

    private void containerSettings() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        String[] names = {"Netzwerk", "Benachrichtigungen", "Hintergrundaktivität", "Dateizugriff"};
        for (String n : names) {
            Button b = smallButton(n + " · Standard: aus/minimal");
            b.setOnClickListener(v -> toast("Einstellung wird für die Container-Engine gespeichert, sobald deren Backend aktiv ist."));
            box.addView(b, topMarginLp(dp(5)));
        }
        new AlertDialog.Builder(this).setTitle("Container-Einstellungen").setView(box).setNegativeButton("Schließen", null).show();
    }

    private void addToolCard(String title, String description, View.OnClickListener listener) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(title, 16, TEXT, true));
        box.addView(text(description, 12, MUTED, false), topMarginLp(dp(3)));
        Button open = smallButton("Öffnen"); open.setOnClickListener(listener); box.addView(open, topMarginLp(dp(8)));
        card(title, box);
    }

    private LinearLayout infoRows(Row... rows) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        for (Row r : rows) {
            LinearLayout line = new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
            TextView l = text(r.key, 13, MUTED, false); TextView v = text(r.value, 13, TEXT, false);
            l.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 0.46f)); v.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 0.54f));
            line.addView(l); line.addView(v);
            box.addView(line, topMarginLp(dp(8)));
        }
        return box;
    }

    private Row storageRow(String name, File f) { return row(name, formatBytes(freeSpace(f)) + " frei / " + formatBytes(totalSpace(f)) + " gesamt"); }

    private Row[] displayRows() {
        android.view.WindowManager wm = (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        return new Row[]{row("Auflösung", dm.widthPixels + " × " + dm.heightPixels), row("Dichte", String.format(Locale.US, "%d dpi", dm.densityDpi)), row("Skalierung", String.format(Locale.US, "%.2fx", dm.density))};
    }

    private Row[] networkRows() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        String type = networkType();
        String iface = "unbekannt"; String dns = "unbekannt";
        try {
            Network n = cm.getActiveNetwork();
            LinkProperties lp = n == null ? null : cm.getLinkProperties(n);
            if (lp != null) {
                iface = String.valueOf(lp.getInterfaceName());
                if (!lp.getDnsServers().isEmpty()) dns = lp.getDnsServers().toString();
            }
        } catch (Exception ignored) {}
        return new Row[]{row("Verbindung", type), row("Interface", iface), row("DNS", dns)};
    }

    private String networkType() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        try {
            Network n = cm.getActiveNetwork();
            if (n == null) return "offline";
            android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(n);
            if (caps == null) return "verbunden";
            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return "WLAN";
            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobilfunk";
            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
            return "sonstiges";
        } catch (Exception e) { return "unbekannt"; }
    }

    private String batteryStatus() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return bm != null && bm.isCharging() ? "lädt" : "entlädt / nicht am Ladegerät";
    }

    private int batteryPercent() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm == null) return -1;
        int p = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return p < 0 ? -1 : p;
    }

    private boolean hasBluetooth() {
        try {
            android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            return adapter != null;
        } catch (Exception e) { return false; }
    }

    private int getSensorCount() {
        android.hardware.SensorManager sm = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
        return sm == null ? 0 : sm.getSensorList(android.hardware.Sensor.TYPE_ALL).size();
    }

    private String webViewPackage() {
        if (Build.VERSION.SDK_INT >= 26) {
            try { return WebView.getCurrentWebViewPackage() == null ? "unbekannt" : WebView.getCurrentWebViewPackage().packageName; } catch (Throwable e) { return "nicht verfügbar"; }
        }
        return "API < 26";
    }

    private void chooseFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_OPEN_FOLDER);
    }

    private void chooseGenericFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, REQ_OPEN_FILE);
    }

    private void chooseApk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, REQ_OPEN_FILE);
    }

    private void chooseVaultFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("*/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, REQ_OPEN_FILE + 10);
    }

    private void chooseUpdateApk(TextView out) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.android.package-archive"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i, REQ_OPEN_FILE + 20);
        prefs.edit().putBoolean("pending_update_check", true).apply();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == REQ_OPEN_FOLDER) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                prefs.edit().putString("root_tree_uri", uri.toString()).apply();
                toast("Ordner gespeichert");
            } else if (requestCode == REQ_OPEN_FILE + 10) {
                encryptToVault(uri);
            } else if (requestCode == REQ_OPEN_FILE + 20) {
                checkUpdateApk(uri);
            } else {
                importSelected(uri);
            }
        } catch (Exception e) { toast("Vorgang fehlgeschlagen: " + e.getMessage()); }
    }

    private void importSelected(Uri uri) throws Exception {
        String name = fileName(uri);
        if (name.toLowerCase(Locale.US).endsWith(".apk")) {
            File dest = new File(getFilesDir(), "apk_store/" + safeName(name));
            copyUri(uri, dest);
            PackageInfo pi = getArchiveInfo(dest);
            String msg = pi == null ? "APK gespeichert, Analyse fehlgeschlagen." : "APK gespeichert: " + pi.packageName + " · " + version(pi) + "\nABI: " + detectAbi(dest);
            toast(msg);
            showPage("Shadow Files");
        } else {
            File dest = new File(getFilesDir(), safeName(name));
            copyUri(uri, dest);
            toast("Importiert: " + dest.getName());
        }
    }

    private void encryptToVault(Uri uri) throws Exception {
        String name = safeName(fileName(uri));
        File out = new File(getFilesDir(), "vault/" + name + ".shadowvault");
        SecureRandom sr = new SecureRandom(); byte[] iv = new byte[12]; sr.nextBytes(iv);
        javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE, getVaultKey(), new javax.crypto.spec.GCMParameterSpec(128, iv));
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream fos = new FileOutputStream(out)) {
            fos.write(iv);
            javax.crypto.CipherOutputStream cos = new javax.crypto.CipherOutputStream(fos, c);
            byte[] buf = new byte[8192]; int read;
            while ((read = in.read(buf)) != -1) cos.write(buf, 0, read);
            cos.flush(); cos.close();
        }
        toast("Verschlüsselt in Shadow Vault: " + out.getName());
    }

    private javax.crypto.SecretKey getVaultKey() throws Exception {
        javax.crypto.KeyGenerator generator = javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore");
        String alias = "shadow_vault_key";
        java.security.KeyStore ks = java.security.KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias(alias)) {
            generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(alias, android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        ks.load(null); return ((java.security.KeyStore.SecretKeyEntry) ks.getEntry(alias, null)).getSecretKey();
    }

    private void checkUpdateApk(Uri uri) throws Exception {
        File tmp = new File(getCacheDir(), "update_check.apk"); copyUri(uri, tmp);
        PackageInfo pi = getArchiveInfo(tmp);
        String currentPkg = getPackageName();
        if (pi == null) { toast("Keine gültige APK erkannt"); return; }
        String msg = "Paket: " + pi.packageName + "\nVersion: " + version(pi) + "\nAktuelle App: " + currentPkg;
        new AlertDialog.Builder(this).setTitle("Update Center").setMessage(msg).setPositiveButton("OK", null).show();
    }

    private PackageInfo getArchiveInfo(File file) {
        try { return getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES); } catch (Exception e) { return null; }
    }

    private String detectAbi(File apk) {
        Set<String> found = new HashSet<>();
        try (ZipFile z = new ZipFile(apk)) {
            for (java.util.Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements();) {
                String n = en.nextElement().getName();
                if (!n.startsWith("lib/")) continue;
                String[] p = n.split("/"); if (p.length >= 3) found.add(p[1]);
            }
        } catch (Exception ignored) {}
        return found.isEmpty() ? "keine native lib erkannt" : String.join(", ", found);
    }

    private void openAppDetails() {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { toast("App-Info nicht verfügbar"); }
    }

    private void openSpecialSettings() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception e) { openAppDetails(); }
    }

    private void appActions(String packageName, String label) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        Button open = smallButton("App öffnen");
        open.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) startActivity(launch); else toast("Keine startbare Activity gefunden.");
        });
        Button info = smallButton("App-Info / Berechtigungen");
        info.setOnClickListener(v -> { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName))); } catch (Exception ignored) {} });
        box.setPadding(dp(18), dp(12), dp(18), dp(8));
        box.addView(open);
        box.addView(info, topMarginLp(dp(8)));
        new AlertDialog.Builder(this).setTitle(label).setView(box).setNegativeButton("Schließen", null).show();
    }

    private void executeShell(String command, EditText output) {
        executor.execute(() -> {
            StringBuilder out = new StringBuilder();
            try {
                Process p = new ProcessBuilder("/system/bin/sh", "-c", command).redirectErrorStream(true).start();
                try (InputStream in = p.getInputStream()) { byte[] buf = new byte[4096]; int r; while ((r = in.read(buf)) != -1) out.append(new String(buf, 0, r, java.nio.charset.StandardCharsets.UTF_8)); }
                int code = p.waitFor(); out.append("\n[exit ").append(code).append("]");
            } catch (Exception e) { out.append("Fehler: ").append(e.getMessage()); }
            final String value = out.toString(); runOnUiThread(() -> output.setText(value));
        });
    }

    private String fileName(Uri uri) {
        android.database.Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null) try { int ix = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && ix >= 0) return c.getString(ix); } finally { c.close(); }
        return "import_" + System.currentTimeMillis();
    }

    private void copyUri(Uri uri, File dest) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192]; int r; while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
    }

    private String safeName(String s) { return s.replaceAll("[^A-Za-z0-9._-]", "_"); }

    private String readText(File f) {
        try (InputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int r;
            while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
            return out.toString("UTF-8");
        } catch (Exception e) { return ""; }
    }
    private void writeText(File f, String s) {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    private void promptSecret(String title, String key) {
        EditText e = input("API-Key"); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); e.setTransformationMethod(PasswordTransformationMethod.getInstance()); e.setText(secureStore.get(key, ""));
        new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Speichern", (d, w) -> { secureStore.put(key, e.getText().toString()); toast("Gespeichert"); }).setNegativeButton("Abbrechen", null).show();
    }

    private void promptText(String title, String hint, String prefKey) {
        EditText e = input(hint); e.setText(prefKey.startsWith("ai_") ? secureStore.get(prefKey, "") : "");
        new AlertDialog.Builder(this).setTitle(title).setView(e).setPositiveButton("Speichern", (d, w) -> { if (prefKey.startsWith("ai_")) secureStore.put(prefKey, e.getText().toString()); else prefs.edit().putString(prefKey, e.getText().toString()).apply(); toast("Gespeichert"); }).setNegativeButton("Abbrechen", null).show();
    }

    private View card(String title, View body) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(15));
        android.graphics.drawable.GradientDrawable background = roundBg(SURFACE, 18);
        background.setStroke(dp(1), 0xFF303B4B);
        box.setBackground(background);
        TextView t = text(title, 15, TEXT, true);
        box.addView(t);
        box.addView(body, topMarginLp(dp(11)));
        content.addView(box, topMarginLp(dp(12)));
        return box;
    }

    private View section(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(3), dp(8), dp(3), dp(2));
        box.addView(text(title, 26, TEXT, true)); box.addView(text(subtitle, 13, MUTED, false), topMarginLp(dp(4)));
        return box;
    }

    private LinearLayout labelled(String title, EditText field) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.addView(text(title, 12, MUTED, false)); box.addView(field); return box;
    }

    private LinearLayout stack(View... views) { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); for (View v : views) box.addView(v, topMarginLp(dp(7))); return box; }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(14);
        e.setPadding(dp(14), dp(11), dp(14), dp(11));
        android.graphics.drawable.GradientDrawable bg = roundBg(SURFACE2, 14);
        bg.setStroke(dp(1), 0xFF303B4B);
        e.setBackground(bg);
        return e;
    }


    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setPadding(dp(12), dp(4), dp(12), dp(4));
        b.setMinHeight(dp(48));
        b.setMinWidth(dp(48));
        android.graphics.drawable.GradientDrawable bg = roundBg(SURFACE, 14);
        bg.setStroke(dp(1), 0xFF303B4B);
        b.setBackground(bg);
        return b;
    }

    private Button smallButton(String s) {
        Button b = button(s);
        b.setTextSize(12);
        b.setPadding(dp(10), dp(3), dp(10), dp(3));
        b.setMinHeight(dp(44));
        b.setMinWidth(dp(48));
        return b;
    }
    private TextView iconButton(String s, int size) { TextView t = text(s, size, TEXT, true); t.setGravity(Gravity.CENTER); return t; }
    private TextView text(String s, float size, int color, boolean bold) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.START); if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); return t; }
    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(h))); return v; }

    private android.graphics.drawable.GradientDrawable roundBg(int color, float radius) { android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(); g.setColor(color); g.setCornerRadius(dp((int) radius)); return g; }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private LinearLayout.LayoutParams bottomLp() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(12); return p; }
    private LinearLayout.LayoutParams topMarginLp(int margin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = margin; return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private Row row(String k, String v) { return new Row(k, v); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    private void animateIn(View v) { v.setAlpha(0f); v.setTranslationY(dp(8)); v.animate().alpha(1f).translationY(0).setDuration(180).start(); }
    private String version(PackageInfo pi) { return pi.versionName == null ? String.valueOf(pi.versionCode) : pi.versionName; }
    private String getVersionName() { try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "1.0.0-dev"; } }
    private long totalSpace(File f) { try { return new StatFs(f.getAbsolutePath()).getTotalBytes(); } catch (Exception e) { return f.getTotalSpace(); } }
    private long freeSpace(File f) { try { return new StatFs(f.getAbsolutePath()).getAvailableBytes(); } catch (Exception e) { return f.getFreeSpace(); } }
    private long freeStorage() { return freeSpace(Environment.getDataDirectory()); }
    private long folderSize(File f) { if (f == null || !f.exists()) return 0; if (f.isFile()) return f.length(); long n = 0; File[] list = f.listFiles(); if (list != null) for (File c : list) n += folderSize(c); return n; }
    private String formatBytes(long bytes) { if (bytes < 1024) return bytes + " B"; double v = bytes; String[] units = {"B","KB","MB","GB","TB"}; int i=0; while (v >= 1024 && i < units.length-1) { v/=1024; i++; } return new DecimalFormat("0.0").format(v) + " " + units[i]; }

    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); executor.shutdownNow(); super.onDestroy(); }

    static final class Row { final String key, value; Row(String k, String v) { key=k; value=v; } }
    static final class Rule { final String name, when, then; Rule(String n,String w,String t){name=n;when=w;then=t;} }

    private void saveRules() { JSONArray a = new JSONArray(); for (Rule r : rules) { JSONObject o = new JSONObject(); try { o.put("name",r.name); o.put("when",r.when); o.put("then",r.then); a.put(o);} catch(Exception ignored){} } prefs.edit().putString("rules", a.toString()).apply(); }
    private void refreshRuleText(TextView t) { StringBuilder s=new StringBuilder(); for(Rule r:rules)s.append("• ").append(r.name).append(" — ").append(r.when).append(" → ").append(r.then).append("\n"); t.setText(s.length()==0?"Noch keine Regeln.":s.toString()); }

    /** Tiny in-app FrameLayout wrapper to avoid external UI dependencies. */
    static class FrameContainer extends android.widget.FrameLayout {
        FrameContainer(Context c) { super(c); }
    }
}
