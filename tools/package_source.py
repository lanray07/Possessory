"""Create a source archive without local SDK paths, API keys, caches or signing files."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT=Path(__file__).resolve().parents[1]
def main():
    files=[]
    for folder in ['app/src','app/schemas','gradle','docs','assets','store','support']:
        files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
    files.extend((ROOT/'tools').glob('*.py'))
    files.extend((ROOT/'tools').glob('requirements*.txt'))
    files.extend((ROOT/'server').glob('*.py'));files.extend((ROOT/'server').glob('*.txt'));files.append(ROOT/'server/README.md')
    files.extend((ROOT/'localization').glob('*.json'))
    files.append(ROOT/'localization/README.md')
    files.extend(p for p in (ROOT/'localization/generated').rglob('*') if p.is_file())
    for name in ['README.md','.gitignore','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','app/build.gradle.kts','app/proguard-rules.pro']:
        files.append(ROOT/name)
    output=ROOT/'artifacts/Possessory-source.zip';output.parent.mkdir(exist_ok=True)
    with ZipFile(output,'w',ZIP_DEFLATED,compresslevel=9) as archive:
        for path in sorted(set(files)):
            assert path.suffix not in {'.jks','.keystore'},'Signing material must never enter source archives'
            archive.write(path,path.relative_to(ROOT))
    print(f'{output}: {len(set(files))} files')
if __name__=='__main__':main()
