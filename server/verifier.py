"""Owner-deployed receipt verifier. No inventory content is accepted or stored.

Configure AMAZON_SHARED_SECRET, AMAZON_PREMIUM_SKU, LICENSE_PRIVATE_KEY_PEM.
AMAZON_RVS_SANDBOX=true is for App Tester only; never use its licenses for release.
Serve behind HTTPS, rate limiting and a request-body limit. No developer secret goes in APK.
"""
import base64, json, os, time, urllib.error, urllib.parse, urllib.request
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field, ConfigDict

app=FastAPI(docs_url=None,redoc_url=None,openapi_url=None)

class ReceiptRequest(BaseModel):
    model_config=ConfigDict(extra='forbid')
    userId: str=Field(min_length=1,max_length=512,pattern=r'^\S+$')
    receiptId: str=Field(min_length=1,max_length=1024,pattern=r'^\S+$')

def claims_from_receipt(receipt, user, receipt_id, expected_sku, sandbox, now):
    if receipt.get('productId')!=expected_sku or receipt.get('productType')!='ENTITLED' or receipt.get('receiptId')!=receipt_id:
        raise ValueError('Receipt does not match the configured entitlement')
    if receipt.get('testTransaction') is not False and not sandbox:
        raise ValueError('Test or unidentified receipt cannot grant production access')
    cancel=receipt.get('cancelDate')
    if cancel is not None and (not isinstance(cancel,(int,float)) or isinstance(cancel,bool)):
        raise ValueError('Invalid cancellation date')
    active=cancel is None or cancel>now*1000
    return dict(sku=expected_sku,userId=user,receiptId=receipt_id,active=active,issuedAt=now,validUntil=now+90*86400,environment='sandbox' if sandbox else 'production')

@app.middleware('http')
async def bounded_requests(request:Request,call_next):
    if request.method=='POST':
        size=0;chunks=[]
        async for chunk in request.stream():
            size+=len(chunk)
            if size>4096:return JSONResponse({'detail':'Request too large'},status_code=413,headers={'Cache-Control':'no-store'})
            chunks.append(chunk)
        request._body=b''.join(chunks)
    response=await call_next(request)
    response.headers['Cache-Control']='no-store'
    response.headers['X-Content-Type-Options']='nosniff'
    return response

@app.get('/health')
def health():
    return {'service':'Possessory receipt verification','configured':all(os.environ.get(key) for key in ['AMAZON_SHARED_SECRET','AMAZON_PREMIUM_SKU','LICENSE_PRIVATE_KEY_PEM'])}

def sign_claims(claims, pem):
    key=serialization.load_pem_private_key(pem,password=None)
    if not isinstance(key,rsa.RSAPrivateKey) or key.key_size<2048:raise ValueError('RSA key of at least 2048 bits required')
    payload=json.dumps(claims,separators=(',',':'),sort_keys=True).encode()
    signature=key.sign(payload,padding.PKCS1v15(),hashes.SHA256())
    b64=lambda b:base64.urlsafe_b64encode(b).decode().rstrip('=')
    return dict(payload=b64(payload),signature=b64(signature))

def verify_with_amazon(user,receipt,secret,sandbox):
    prefix='https://appstore-sdk.amazon.com/'+('sandbox/' if sandbox else '')+'version/1.0/verifyReceiptId/developer/'
    quote=lambda value:urllib.parse.quote(value,safe='')
    url=prefix+quote(secret)+'/user/'+quote(user)+'/receiptId/'+quote(receipt)
    # Never log this URL: it contains the developer secret.
    with urllib.request.urlopen(urllib.request.Request(url,headers={'Accept':'application/json'}),timeout=15) as response:
        body=response.read(65537)
        if len(body)>65536:raise ValueError('RVS response too large')
        return json.loads(body)

@app.post('/verify')
def verify(request:ReceiptRequest):
    secret=os.environ.get('AMAZON_SHARED_SECRET');sku=os.environ.get('AMAZON_PREMIUM_SKU');pem=os.environ.get('LICENSE_PRIVATE_KEY_PEM')
    sandbox=os.environ.get('AMAZON_RVS_SANDBOX','false').lower()=='true'
    if not secret or not sku or not pem:raise HTTPException(503,'Verification is not configured')
    try:
        receipt=verify_with_amazon(request.userId,request.receiptId,secret,sandbox)
        claims=claims_from_receipt(receipt,request.userId,request.receiptId,sku,sandbox,int(time.time()))
        return sign_claims(claims,pem.encode())
    except urllib.error.HTTPError as exc:
        if exc.code==410:
            claims=dict(sku=sku,userId=request.userId,receiptId=request.receiptId,active=False,issuedAt=int(time.time()),validUntil=int(time.time())+90*86400,environment='sandbox' if sandbox else 'production')
            return sign_claims(claims,pem.encode())
        raise HTTPException(422 if exc.code in [400,497] else 503,'Receipt could not be verified') from None
    except (ValueError,TypeError,KeyError):raise HTTPException(422,'Receipt did not match the configured entitlement') from None
    except Exception:raise HTTPException(503,'Verification temporarily unavailable') from None
