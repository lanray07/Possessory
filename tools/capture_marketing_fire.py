"""Collect seven instrumented views and one native OCR view per locale on the isolated Fire QA app."""
import argparse
import hashlib
import json
import os
import re
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "app.possessory.iapqa"
ADB = Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe"
REMOTE = f"/sdcard/Android/data/{PACKAGE}/files/marketing-captures"


def adb(*args, binary=False):
    result = subprocess.run([str(ADB), "-s", "G001KT0620330AWU", *map(str, args)],
                            capture_output=True, timeout=240, check=True)
    return result.stdout if binary else result.stdout.decode("utf-8", "replace").strip()


def node(predicate, attempts=4, scroll=False, rtl=False):
    for _ in range(attempts):
        dump = adb("shell", "uiautomator", "dump", "/sdcard/possessory-qa-ui.xml")
        if "dumped to" not in dump:
            continue
        doc = ET.fromstring(adb("shell", "cat", "/sdcard/possessory-qa-ui.xml"))
        for n in doc.iter("node"):
            if n.get("package") == PACKAGE and predicate(n):
                return n
        if scroll:
            x = 820 if rtl else 1100
            adb("shell", "input", "swipe", x, 935, x, 380, 380)
    raise ValueError("Could not find required control on the live Possessory screen")


def tap(n):
    x, y, x2, y2 = map(int, re.findall(r"\d+", n.attrib["bounds"]))
    adb("shell", "input", "tap", (x+x2)//2, (y+y2)//2)


def ocr(tag, out):
    labels = {n.get("name"): n.text for n in ET.parse(
        ROOT/"localization/generated"/("values-b+"+tag.replace("-", "+"))/"strings.xml").findall("string")}
    def label(key, attr="text", **opts):
        return node(lambda n: n.get(attr)==labels[key], rtl=tag=="ar", **opts)
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-n", PACKAGE+"/app.possessory.MainActivity")
    tap(label("inventory", attempts=8))
    tap(node(lambda n:n.get("text")=="Reading lamp" and n.get("class")=="android.widget.TextView",attempts=10,scroll=True,rtl=tag=="ar"))
    tap(label("scan_receipt","content-desc",attempts=12,scroll=True))
    tap(label("extract_text",attempts=8,scroll=True))
    node(lambda n:"WILLOW SHOP" in n.get("text","").upper(),attempts=10,scroll=True)
    adb("shell","input","swipe",960,340,960,910,350)
    label("receipt_preview","content-desc",attempts=3)
    (out/"ocr.png").write_bytes(adb("exec-out","screencap","-p",binary=True))


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--version",required=True)
    p.add_argument("--locale")
    p.add_argument("--ocr-only",action="store_true",help="Diagnostic mode; does not certify a complete capture")
    args=p.parse_args()
    tags=json.loads((ROOT/"localization/status.json").read_text("utf-8"))
    selected=[args.locale] if args.locale else list(tags)
    assert set(selected)<=set(tags) and re.fullmatch(r"\d+\.\d+\.\d+",args.version)
    assert "mIsShowing=true" not in adb("shell","dumpsys","window","policy")
    settings=[("global","stay_on_while_plugged_in","3"),("system","accelerometer_rotation","0"),("system","user_rotation","3")]
    original=[adb("shell","settings","get",s,k) for s,k,_ in settings]
    wifi=adb("shell","settings","get","global","wifi_on")
    sandbox=adb("shell","getprop","debug.amazon.sandboxmode")
    with tempfile.TemporaryDirectory() as tmp:
        backup=Path(tmp)/"original.json"
        adb("pull","/sdcard/amazon.sdktester.json",backup)
        catalog=json.loads(backup.read_text("utf-8-sig"))
        catalog["possessory.premium.lifetime"]=dict(itemType="ENTITLED",price=4.99,title="Possessory Premium",description="One-time upgrade")
        merged=Path(tmp)/"test.json"
        merged.write_text(json.dumps(catalog),"utf-8")
        try:
            for s,k,v in settings: adb("shell","settings","put",s,k,v)
            adb("push",merged,"/sdcard/amazon.sdktester.json")
            adb("shell","setprop","debug.amazon.sandboxmode","debug")
            adb("shell","am","force-stop","com.amazon.sdktestclient")
            adb("shell","am","start","-n","com.amazon.sdktestclient/.MainActivity")
            for tag in selected:
                out=ROOT/"artifacts"/f"marketing-captures-{args.version}"/tag
                out.mkdir(exist_ok=True,parents=True)
                if not args.ocr_only:
                    log=adb("shell","am","instrument","-w","-e","class","app.possessory.MarketingCaptureTest","-e","marketingLocale",tag,PACKAGE+".test/androidx.test.runner.AndroidJUnitRunner")
                    (out/"capture-log.txt").write_text(log+"\n","utf-8")
                    assert "OK (1 test)" in log, f"Navigation failed: {tag}"
                    adb("pull",f"{REMOTE}/{tag}/.",out)
                    nav=json.loads((out/"navigation-evidence.json").read_text("utf-8"))
                    assert nav["locale"]==tag and nav["screens"]==7 and nav["offlineActiveNetworkNull"] and nav["sandboxPremium"]
                ocr(tag,out)
                if not args.ocr_only:
                    (out/"evidence.json").write_text(json.dumps(dict(nav,screens=8,realOcrTextVerified=True)),"utf-8")
                print("Captured",tag,flush=True)
        finally:
            adb("push",backup,"/sdcard/amazon.sdktester.json")
            check=Path(tmp)/"restored.json"
            adb("pull","/sdcard/amazon.sdktester.json",check)
            adb("shell","setprop","debug.amazon.sandboxmode",sandbox or "none")
            adb("shell","am","force-stop","com.amazon.sdktestclient")
            adb("shell","svc","wifi","enable" if wifi=="1" else "disable")
            for (s,k,_),v in zip(settings,original): adb("shell","settings","put",s,k,v)
            assert backup.read_bytes()==check.read_bytes()


if __name__=="__main__":
    main()
