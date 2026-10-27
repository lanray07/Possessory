"""Development-only Argos/CTranslate2 translation. Only the generated text ships.

Uses the official Argos package index and data mirror; no translation API or key.
Generated drafts are not human-reviewed or release-approved.
"""
import argparse, concurrent.futures, gc, hashlib, json, re, urllib.request, zipfile
from pathlib import Path
from localize import ROOT, CONFIG, extract, resources, validate

MODELS=ROOT/'localization/cache/models'
LANGUAGES={'es-ES':'es','es-419':'es','fr-FR':'fr','fr-CA':'fr','de-DE':'de','it-IT':'it','pt-BR':'pb','pt-PT':'pt','nl-NL':'nl','pl-PL':'pl','sv-SE':'sv','ja-JP':'ja','hi-IN':'hi','ar':'ar','tr-TR':'tr','id-ID':'id'}
PROTECT=re.compile(r'%(?:\d+\$)?[dsf]|Possessory|\\n|YYYY-MM-DD|\b(?:PDF|CSV|JSON|ZIP|AES-GCM|AES|GCM|SDK|OCR|MiB)\b|\d+(?:[.,]\d+)*|“[^”]+”')

def index():
    path=ROOT/'localization/model-index.json'
    if not path.exists():urllib.request.urlretrieve('https://raw.githubusercontent.com/argosopentech/argospm-index/main/index.json',path)
    return json.loads(path.read_text(encoding='utf-8'))

def acquire(code):
    options=[p for p in index() if p.get('from_code')=='en' and p.get('to_code')==code]
    if not options:raise ValueError('No English model for '+code)
    metadata=max(options,key=lambda p:tuple(map(int,p['package_version'].split('.'))))
    filename=next(u for u in metadata['links'] if u.startswith('https://')).rsplit('/',1)[-1]
    assert re.fullmatch(r'translate-en_[a-z]+-[0-9_]+\.argosmodel',filename)
    MODELS.mkdir(parents=True,exist_ok=True);archive=MODELS/f'en-{code}.argosmodel';destination=MODELS/f'en-{code}'
    if not archive.exists():
        partial=archive.with_suffix('.partial')
        urllib.request.urlretrieve('https://data.argosopentech.com/argospm/v1/'+filename,partial)
        partial.replace(archive)
    if not destination.exists():
        with zipfile.ZipFile(archive) as z:
            for entry in z.infolist():
                resolved=(destination/entry.filename).resolve()
                assert destination.resolve() in resolved.parents or destination.resolve()==resolved
            z.extractall(destination)
    path=next(destination.rglob('metadata.json')).parent
    provenance=dict(language=code,package_version=metadata['package_version'],source='https://data.argosopentech.com/argospm/v1/'+filename,sha256=hashlib.sha256(archive.read_bytes()).hexdigest())
    return path,provenance

def pieces(text):
    result=[];last=0
    for match in PROTECT.finditer(text):
        result.append((False,text[last:match.start()]));result.append((True,match.group()));last=match.end()
    result.append((False,text[last:]));return result

def translate_catalogue(code,source):
    import ctranslate2, sentencepiece
    path,provenance=acquire(code)
    tokenizer_file=path/'sentencepiece.model'
    if tokenizer_file.exists():
        tokenizer=sentencepiece.SentencePieceProcessor(model_file=str(tokenizer_file))
        encode=lambda text:tokenizer.encode(text,out_type=str)
        decode=lambda tokens:tokenizer.decode(tokens).replace('▁',' ')
    else:
        from sacremoses import MosesTokenizer,MosesDetokenizer,MosesPunctNormalizer
        from subword_nmt.apply_bpe import BPE
        normalizer=MosesPunctNormalizer('en');tokenizer=MosesTokenizer('en');detokenizer=MosesDetokenizer('pt' if code=='pb' else code)
        with (path/'bpe.model').open(encoding='utf-8') as model:bpe=BPE(model)
        encode=lambda text:bpe.segment_tokens(tokenizer.tokenize(normalizer.normalize(text)))
        decode=lambda tokens:detokenizer.detokenize(' '.join(tokens).replace('@@ ','').split(' '))
    metadata=json.loads((path/'metadata.json').read_text(encoding='utf-8'))
    translator=ctranslate2.Translator(str(path/'model'),device='cpu',compute_type='int8',intra_threads=4)
    fragments=sorted({fragment.strip() for text in source.values() for protected,fragment in pieces(text.replace("\\'", "'")) if not protected and any(c.isalpha() for c in fragment)})
    tokens=[encode(text) for text in fragments]
    prefix=metadata.get('target_prefix','')
    results=translator.translate_batch(tokens,target_prefix=[[prefix]]*len(tokens) if prefix else None,beam_size=2,replace_unknowns=True,max_batch_size=256,batch_type='tokens',length_penalty=.2,max_decoding_length=512)
    translations={text:decode(result.hypotheses[0]).removeprefix(prefix).strip() for text,result in zip(fragments,results)}
    result={}
    for key,text in source.items():
        output=[]
        for protected,part in pieces(text.replace("\\'", "'")):
            core=part.strip()
            if protected or core not in translations:output.append(part)
            else:output.append(part[:len(part)-len(part.lstrip())]+translations[core]+part[len(part.rstrip()):])
        result[key]=''.join(output)
    validate(source,result)
    del translator;gc.collect()
    return result,provenance

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--download-only',action='store_true');parser.add_argument('--languages',nargs='*');args=parser.parse_args()
    targets=args.languages or sorted(set(LANGUAGES.values()))
    if args.download_only:
        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as executor:
            for path,info in executor.map(acquire,targets):print('Downloaded',info['language'],flush=True)
        return
    source=extract();status_path=ROOT/'localization/status.json';status=json.loads(status_path.read_text(encoding='utf-8'))
    provenance=[]
    for code in targets:
        cache=ROOT/f'localization/cache/catalogue-{code}.json'
        if cache.exists() and json.loads(cache.read_text(encoding='utf-8'))['source_sha256']==hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest():
            saved=json.loads(cache.read_text(encoding='utf-8'));translated=saved['strings'];info=saved['model']
        else:
            print('Translating',code,flush=True);translated,info=translate_catalogue(code,source)
            cache.write_text(json.dumps(dict(source_sha256=hashlib.sha256(json.dumps(source,sort_keys=True).encode()).hexdigest(),strings=translated,model=info),ensure_ascii=False),encoding='utf-8')
        provenance.append(info)
        overrides=ROOT/'localization/overrides.json'
        if overrides.exists():translated.update(json.loads(overrides.read_text(encoding='utf-8')).get(code,{}))
        validate(source,translated)
        for locale,language in LANGUAGES.items():
            if language!=code:continue
            resources(locale,translated)
            status[locale]=dict(state='machine_translated',human_reviewer=None,release_approved=False,provider='Argos/CTranslate2 (development only)',issues=['Human translation, regional terminology, plurals, RTL and layout review pending'],bundled_in_preview=True)
        status_path.write_text(json.dumps(status,indent=2)+'\n')
        print('Generated',code,flush=True)
    for locale in ['en-GB','en-US','en-AU']:
        strings=dict(source)
        if locale=='en-US':strings={k:v.replace('organised','organized').replace('organise','organize').replace('localisation','localization').replace('Jewellery','Jewelry') for k,v in strings.items()}
        resources(locale,strings)
        status[locale]=dict(state='validated',human_reviewer=None,release_approved=False,provider='English source with regional spelling',issues=['Regional and release review pending'],bundled_in_preview=True)
    status_path.write_text(json.dumps(status,indent=2)+'\n')
    (ROOT/'localization/model-provenance.json').write_text(json.dumps(provenance,indent=2)+'\n')
if __name__=='__main__':main()
