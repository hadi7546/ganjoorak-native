#!/usr/bin/env bash
# Installs each APK on the running emulator, launches it, and reports whether it stays up,
# any crash stack traces, and the text visible on screen.
set -u
pkg=net.ganjoorak.app
status=0

# Taps the centre of the first node whose text or content-desc is exactly $1.
tap() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b
  b=$(adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep -E "(text|content-desc)=\"$1\"" \
    | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1 | grep -oE '[0-9]+' | tr '\n' ' ')
  set -- $b
  if [ $# -eq 4 ]; then adb shell input tap $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )); else echo "(nothing labelled to tap)"; fi
}

# The emulator's launcher sometimes raises an "isn't responding" dialog over the app; wait it out.
dismiss_anr() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  if adb shell cat /sdcard/ui.xml | grep -q "isn't responding"; then
    echo "(dismissing a system 'not responding' dialog)"
    tap "Wait"
    sleep 2
  fi
}

# Prints the screen as base64 PNG between markers.
shot() {
  adb exec-out screencap -p > /tmp/shot.png
  echo "BEGIN-SHOT $1" && base64 -w 200 /tmp/shot.png && echo "END-SHOT"
}
echo "--- what the APIs return from this runner"
for url in "https://api.ganjoor.net/api/ganjoor/poem/random" "https://echolalia.ir/wp-json/wp/v2/categories?slug=sher"; do
  rm -f /tmp/body
  curl -sS -m 20 -o /tmp/body -D - "$url" | grep -iE "^(HTTP|content-type|server)" || true
  [ -f /tmp/body ] && head -c 200 /tmp/body; echo
done
echo "--- random poem status by request headers (3 tries each)"
random=https://api.ganjoor.net/api/ganjoor/poem/random
for ua in "curl/8" "okhttp/5.1.0" "Ganjoorak/1.0.0"; do
  codes=""
  for i in 1 2 3; do
    codes="$codes $(curl -s -m 20 -o /dev/null -w '%{http_code}' -A "$ua" -H 'Accept: application/json' -H 'Accept-Encoding: gzip' "$random")"
  done
  echo "$ua:$codes"
done
for apk in androidApp/build/outputs/apk/debug/androidApp-debug.apk androidApp/build/outputs/apk/release/androidApp-release.apk; do
  name=$(basename "$apk")
  echo "::group::$name"
  adb uninstall "$pkg" >/dev/null 2>&1
  if ! adb install -r "$apk"; then
    echo "::error::Could not install $name"
    status=1
    echo "::endgroup::"
    continue
  fi
  adb logcat -c
  adb shell am start -W -n "$pkg/.MainActivity"
  sleep 25
  # First run opens the topic picker over the feed; keep a picture of it, then close it.
  dismiss_anr
  [[ "$name" == *release* ]] && shot "$name-first-run"
  tap "بعداً"
  sleep 3
  if adb shell pidof "$pkg" >/dev/null; then
    echo "$name is running"
  else
    echo "::error::$name is not running 25s after launch"
    status=1
  fi
  echo "--- crash buffer"
  adb logcat -d -b crash
  echo "--- app log"
  adb logcat -d | grep -E "Ganjoorak:" | head -20
  echo "--- errors"
  adb logcat -d '*:E' | grep -iE "ganjoorak|AndroidRuntime|FATAL|Exception" | head -100
  dismiss_anr
  echo "--- on screen"
  # Wait up to a minute for the feed to show a poem or an error; chrome alone is about 9 nodes.
  for i in $(seq 1 12); do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb shell cat /sdcard/ui.xml > /tmp/ui.xml
    nodes=$(grep -oE '(text|content-desc)="[^"]+"' /tmp/ui.xml 2>/dev/null | wc -l)
    { grep -q "ممکن نشد" /tmp/ui.xml || [ "$nodes" -gt 12 ]; } 2>/dev/null && break
    sleep 5
  done
  if [ -f /tmp/ui.xml ]; then
    grep -oE '(text|content-desc)="[^"]+"' /tmp/ui.xml | head -60
    if grep -q "ممکن نشد" /tmp/ui.xml; then
      echo "::warning::$name shows a loading error on the home screen"
    elif [ "$nodes" -le 12 ]; then
      echo "::warning::$name showed no poem within a minute"
    fi
  else
    echo "(screen dump unavailable)"
  fi
  echo "--- uiautomator says"
  adb shell uiautomator dump /sdcard/ui2.xml 2>&1 | tail -2
  echo "--- screenshot (base64 png)"
  dismiss_anr
  shot "$name"
  if [[ "$name" == *release* ]]; then
    for tab in "شاعران" "حساب من"; do
      echo "--- menu → $tab"
      dismiss_anr
      tap "منو"
      sleep 2
      shot "$name-menu"
      tap "$tab"
      sleep 12
      dismiss_anr
      adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 && adb shell cat /sdcard/ui.xml \
        | grep -oE '(text|content-desc)="[^"]+"' | head -30
      shot "$name-$tab"
    done
  fi
  echo "--- app log after waiting"
  adb logcat -d | grep -E "Ganjoorak:" | head -20
  rm -f /tmp/ui.xml
  echo "::endgroup::"
done
exit $status
