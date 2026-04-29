# VulnBank

Deliberately vulnerable Android app for local mobile security practice.

## Build

Open this folder in Android Studio, then run the `app` configuration on an emulator.

Or from terminal:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

## Test account

```text
admin / admin123
```

## Practice points

```bash
adb logcat | grep VulnBank
adb shell run-as com.example.vulnbank cat shared_prefs/login_data.xml
adb shell am start -n com.example.vulnbank/.BalanceActivity --es username attacker --es balance 999999999
adb shell am start -n com.example.vulnbank/.WebActivity --es url "file:///etc/hosts"
```

SQL injection input in the search box:

```sql
' OR '1'='1
```

Use only on your own emulator or test device.
