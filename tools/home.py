#!/usr/bin/env python3
"""Choose the TV's home screen: this repo's launcher (CleanYourTV) or Free My TV.

    TV_HOST=<tv ip> tools/home.py status
    TV_HOST=<tv ip> tools/home.py cleanyourtv [--input-key-service] [--home-key-service]
    TV_HOST=<tv ip> tools/home.py freemytv [--src DIR] [--update] [--city NAME --lat N --lon N]
                                          [--region CC] [--tmdb-key KEY] [--no-crypto]

Windows: tools\\venv\\Scripts\\python.exe -I tools\\home.py ... (with $env:TV_HOST set).

cleanyourtv: if Free My TV is the home screen, it is turned off (pm disable-user: still installed, its settings kept)
so its boot check cannot take Home back, and the TV's own screensaver returns. Then CleanYourTV becomes Home: switched
on if it is installed, built and installed with launcher/build-and-install otherwise.

freemytv: builds Free My TV (github.com/simbasjungles/free-my-tv, cloned next to this repo unless --src says
otherwise) with its own build.sh, installs it and opens its settings. The switch itself happens on the TV (Free My TV
-> Home Screen -> Free My TV), because Free My TV records what it turns off with its own ADB key, which the TV must
allow: that record is what its Original button restores. If CleanYourTV is Home at that moment, Original brings it back.

Both need port 5555 (TV_HOST), like every tool here. So does Free My TV's Original button: on TVs that close the port
at every restart, run `adb tcpip 5555` again first.
"""
import argparse
import os
import platform
import re
import shutil
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tv  # noqa: E402

TOOLS = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(TOOLS)
CLEAN = "com.gathin.tvhome"
CLEAN_HOME = CLEAN + "/.MainActivity"
FMT = "io.freemytv"
FMT_HOME = FMT + "/.HomeActivity"
FMT_SETTINGS = FMT + "/.SettingsActivity"
FMT_REPO = "https://github.com/simbasjungles/free-my-tv.git"
# The screensaver Free My TV replaces on Google TV; it turns the app off and Original turns it back on.
GOOGLE_DREAM_PKG = "com.google.android.apps.tv.dreamx"
GOOGLE_DREAM = GOOGLE_DREAM_PKG + "/.service.Backdrop"


def sh(dev, cmd):
    return dev.shell(cmd, read_timeout_s=120.0).strip()


def packages(dev, flag=""):
    return {line[len("package:"):].strip() for line in sh(dev, f"pm list packages {flag}").splitlines()
            if line.startswith("package:")}


def home(dev):
    out = sh(dev, "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
    return out.splitlines()[-1].strip() if out else ""


def describe(pkg, installed, disabled):
    if pkg not in installed:
        return "not installed"
    return "installed, disabled" if pkg in disabled else "installed, enabled"


def status(dev, _args=None):
    installed, disabled = packages(dev), packages(dev, "-d")
    current = home(dev)
    name = {CLEAN: "CleanYourTV", FMT: "Free My TV"}.get(current.split("/")[0], "another app")
    print(f"Home:        {current or '?'} ({name})")
    print(f"Screensaver: {sh(dev, 'settings get secure screensaver_components')}")
    print(f"CleanYourTV: {describe(CLEAN, installed, disabled)}")
    print(f"Free My TV:  {describe(FMT, installed, disabled)}")


def restore_screensaver(dev, installed):
    """Free My TV's screensaver goes with it: back to Google TV's, or to the TV's default when there is none."""
    if not sh(dev, "settings get secure screensaver_components").startswith(FMT + "/"):
        return
    if GOOGLE_DREAM_PKG in installed:
        sh(dev, f"pm enable {GOOGLE_DREAM_PKG}")
        sh(dev, f"settings put secure screensaver_components {GOOGLE_DREAM}")
    else:
        sh(dev, "settings delete secure screensaver_components")


def cleanyourtv(dev, args):
    installed, disabled = packages(dev), packages(dev, "-d")
    fmt_on = FMT in installed and FMT not in disabled
    if fmt_on:
        restore_screensaver(dev, installed)
    if CLEAN not in installed:
        dev.close()
        # The installer turns off whatever owns Home after the new launcher has started (and puts it back if Home
        # does not end up on the launcher): with Free My TV as Home, that is Free My TV.
        stock = FMT if fmt_on else ""
        if platform.system() == "Windows":
            cmd = ["powershell", "-ExecutionPolicy", "Bypass", "-File",
                   os.path.join(REPO, "launcher", "build-and-install.ps1"), "-TvHost", tv.HOST]
            if stock:
                cmd += ["-StockLauncher", stock]
            if args.input_key_service:
                cmd.append("-InputKeyService")
            if args.home_key_service:
                cmd.append("-HomeKeyService")
            env = dict(os.environ)
        else:
            cmd = ["bash", os.path.join(REPO, "launcher", "build-and-install.sh")]
            env = dict(os.environ, TV_HOST=tv.HOST)
            if stock:
                env["STOCK_LAUNCHER"] = stock
            if args.input_key_service:
                env["INPUT_KEY_SERVICE"] = "1"
            if args.home_key_service:
                env["HOME_KEY_SERVICE"] = "1"
        sys.exit(subprocess.call(cmd, env=env))

    sh(dev, f"pm enable {CLEAN}")
    sh(dev, f"cmd package set-home-activity --user 0 {CLEAN_HOME}")
    if fmt_on:
        # Off, not uninstalled: Free My TV keeps its settings and its record of what it turned off, and with the app
        # off its boot check and watchdog cannot hand Home back to it.
        sh(dev, f"pm disable-user --user 0 {FMT}")
    now = home(dev)
    if now != CLEAN_HOME:
        sys.exit(f"Home resolves to {now}, not CleanYourTV. Run `tools/home.py status`, and see README, Troubleshooting.")
    sh(dev, f"am start -n {CLEAN_HOME}")
    print("CleanYourTV is the home screen." + (" Free My TV is off (still installed); "
                                               "`tools/home.py freemytv` brings it back." if fmt_on else ""))


def bash_path():
    found = shutil.which("bash")
    if platform.system() == "Windows":
        # WSL's bash.exe in System32 cannot see this Windows checkout's tools: prefer Git Bash.
        for candidate in (r"C:\Program Files\Git\bin\bash.exe", r"C:\Program Files (x86)\Git\bin\bash.exe"):
            if os.path.exists(candidate):
                return candidate
    return found


def write_config(src, args):
    """config.properties (kept out of git by Free My TV): created from its example, then the values given here."""
    path = os.path.join(src, "config.properties")
    if not os.path.exists(path):
        shutil.copyfile(os.path.join(src, "config.example.properties"), path)
    values = {"city": args.city, "latitude": args.lat, "longitude": args.lon, "tmdb_region": args.region,
              "tmdb_key": args.tmdb_key, "show_crypto": "false" if args.no_crypto else None}
    with open(path, encoding="utf-8") as f:
        text = f.read()
    for key, value in values.items():
        if value is None:
            continue
        line = f"{key}={value}"
        text, n = re.subn(rf"(?m)^\s*{key}\s*=.*$", lambda _: line, text)
        if n == 0:
            text += ("" if text.endswith("\n") else "\n") + line + "\n"
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    shown = {k: ("<set>" if k == "tmdb_key" else v) for k, v in values.items() if v is not None}
    if shown:
        print("config.properties: " + ", ".join(f"{k}={v}" for k, v in shown.items()))


def freemytv(dev, args):
    src = os.path.abspath(args.src or os.path.join(os.path.dirname(REPO), "free-my-tv"))
    if not os.path.isdir(os.path.join(src, ".git")):
        subprocess.check_call(["git", "clone", FMT_REPO, src])
    elif args.update:
        subprocess.check_call(["git", "-C", src, "pull", "--ff-only"])
    write_config(src, args)
    bash = bash_path()
    if not bash:
        sys.exit("Free My TV builds with bash (build.sh): install Git for Windows (Git Bash) or run this on macOS/Linux.")
    subprocess.check_call([bash, "build.sh"], cwd=src)
    apk = os.path.join(src, "FreeMyTV.apk")

    # Same way as the launcher's installer: base64 over the shell, then pm install (no adb binary, no stuck prompts).
    dev.close()
    subprocess.check_call([sys.executable, "-I", os.path.join(TOOLS, "b64push.py"), apk, "/data/local/tmp/freemytv.apk"])
    dev = tv.connect()
    try:
        out = sh(dev, "pm install -r /data/local/tmp/freemytv.apk; rm -f /data/local/tmp/freemytv.apk")
        print(out)
        if "Success" not in out:
            sys.exit("pm install failed; nothing else changed.")
        sh(dev, f"pm enable {FMT}")   # turned off by `home.py cleanyourtv`
        if home(dev) == FMT_HOME:
            print("Free My TV is already the home screen; it was updated.")
            return
        sh(dev, f"am start -n {FMT_SETTINGS}")
    finally:
        dev.close()
    print("Free My TV is installed and its settings are open on the TV. On the TV:\n"
          "  1. Home Screen -> Free My TV.\n"
          "  2. Accept 'Allow USB debugging?' (tick Always allow): that key is Free My TV's own.\n"
          "Back to CleanYourTV later: Free My TV -> Home Screen -> Original (port 5555 open), or `tools/home.py cleanyourtv`.")


def main():
    p = argparse.ArgumentParser(description="Choose the home screen: CleanYourTV or Free My TV.")
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("status", help="show which home screen is in use")
    c = sub.add_parser("cleanyourtv", help="make CleanYourTV the home screen (installs it if needed)")
    c.add_argument("--input-key-service", action="store_true", help="passed to the launcher installer")
    c.add_argument("--home-key-service", action="store_true", help="passed to the launcher installer")
    f = sub.add_parser("freemytv", help="build and install Free My TV, then switch on the TV")
    f.add_argument("--src", help="Free My TV checkout (default: ../free-my-tv next to this repo)")
    f.add_argument("--update", action="store_true", help="git pull the checkout before building")
    f.add_argument("--city", help="name shown on the weather card")
    f.add_argument("--lat", help="latitude, for the weather and the sunset")
    f.add_argument("--lon", help="longitude")
    f.add_argument("--region", help="two-letter country for the picks (TMDB region), e.g. CO")
    f.add_argument("--tmdb-key", help="your TMDB key, for the picks (stored only in config.properties)")
    f.add_argument("--no-crypto", action="store_true", help="hide the crypto prices")
    args = p.parse_args()

    dev = tv.connect()
    try:
        {"status": status, "cleanyourtv": cleanyourtv, "freemytv": freemytv}[args.cmd](dev, args)
    finally:
        try:
            dev.close()
        except Exception:
            pass


if __name__ == "__main__":
    main()
