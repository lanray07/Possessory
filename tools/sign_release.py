"""Sign a release APK with the dedicated, privately stored Possessory key.

Never prints passwords or copies signing material into the repository.
Back up the external signing directory securely before distributing updates.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    if not args.input.is_file():
        parser.error("Build the release APK first")
    signing = Path.home() / ".android" / "possessory-signing"
    if not signing.is_dir():
        parser.error("Create a private, access-restricted signing directory first")
    config_path = signing / "credentials.json"
    key = signing / "possessory-release.jks"
    java = Path(os.environ["JAVA_HOME"]) / "bin"
    sdk = Path(os.environ.get("ANDROID_HOME", str(Path.home() / "AppData/Local/Android/Sdk")))
    build_tools = sdk / "build-tools" / "36.0.0"
    if not config_path.exists():
        if key.exists():
            raise RuntimeError("Existing key lacks credentials; refusing to replace it")
        config = {"alias": "possessory-release", "password": secrets.token_urlsafe(36)}
        with config_path.open("x", encoding="utf-8") as out:
            json.dump(config, out)
    config = json.loads(config_path.read_text(encoding="utf-8"))
    env = dict(os.environ, POSSESSORY_SIGNING_PASSWORD=config["password"])
    if not key.exists():
        subprocess.run([
            str(java / "keytool.exe"), "-genkeypair", "-keystore", str(key),
            "-storetype", "JKS", "-alias", config["alias"], "-keyalg", "RSA",
            "-keysize", "4096", "-validity", "10000",
            "-dname", "CN=O. Bankole, OU=Possessory",
            "-storepass:env", "POSSESSORY_SIGNING_PASSWORD",
            "-keypass:env", "POSSESSORY_SIGNING_PASSWORD",
        ], check=True, env=env)
    subprocess.run([str(build_tools / "zipalign.exe"), "-c", "-P", "16", "4", str(args.input)], check=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # Invoke the signing JAR directly so no shell expansion can touch secrets.
    signer = [str(java / "java.exe"), "-jar", str(build_tools / "lib/apksigner.jar")]
    subprocess.run(signer + [
        "sign", "--ks", str(key), "--ks-key-alias", config["alias"],
        "--ks-pass", "env:POSSESSORY_SIGNING_PASSWORD",
        "--key-pass", "env:POSSESSORY_SIGNING_PASSWORD",
        "--out", str(args.output), str(args.input),
    ], check=True, env=env)
    subprocess.run(signer + ["verify", "--verbose", "--print-certs", str(args.output)], check=True)
    print("APK SHA-256:", hashlib.sha256(args.output.read_bytes()).hexdigest())
    print("Dedicated signing material:", signing)


if __name__ == "__main__":
    main()
