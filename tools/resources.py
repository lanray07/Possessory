"""Validate the canonical English XML catalogue without rewriting it.

Edit app/src/main/res/values/strings.xml directly, then regenerate translations.
Keeping a second dictionary here previously risked overwriting newer messages.
"""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def main():
    path=ROOT/'app/src/main/res/values/strings.xml'
    tree=ET.parse(path)
    strings={node.attrib['name'] for node in tree.findall('string')}
    assert len(strings)==len(tree.findall('string')), 'Duplicate string keys'
    refs=set()
    for source in (ROOT/'app/src').rglob('*.kt'):
        refs.update(re.findall(r'R\.string\.([a-zA-Z0-9_]+)',source.read_text(encoding='utf-8')))
    assert not refs-strings, f'Missing strings: {refs-strings}'
    print(f'{len(strings)} English strings validated; canonical XML unchanged')

if __name__=='__main__':main()
