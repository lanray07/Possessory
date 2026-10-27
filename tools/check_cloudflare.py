"""Public endpoint smoke checks; never reads deployment secrets or customer receipts."""
import json
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    config = json.loads((ROOT / 'server/cloudflare/android-public-config.json').read_text(encoding='utf-8'))
    results = {}
    for environment, public in config.items():
        endpoint = public['verifierUrl']
        probes = [
            ('health', endpoint.removesuffix('/verify') + '/health', None, 200),
            ('invalid_receipt', endpoint, {'userId': 'possessory-health-probe', 'receiptId': 'invalid-receipt-probe'}, 422),
            ('inventory_rejected', endpoint, {'userId': 'probe', 'receiptId': 'probe', 'inventory': []}, 422),
        ]
        checks = []
        for name, url, body, expected in probes:
            request = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(), headers={'Content-Type': 'application/json', 'User-Agent': 'Possessory-Deployment-Check/1.0'})
            try:
                response = urllib.request.urlopen(request, timeout=20)
            except urllib.error.HTTPError as error:
                response = error
            with response:
                assert response.status == expected, f'{environment} {name}: unexpected HTTP status {response.status}'
                payload = json.loads(response.read(65536))
                assert response.headers.get('Cache-Control') == 'no-store'
                assert response.headers.get('X-Content-Type-Options') == 'nosniff'
                if name == 'health':
                    assert payload.get('configured') is True
                checks.append({'check': name, 'status': response.status, 'passed': True})
        results[environment] = {'endpoint': endpoint, 'checks': checks}
    output = ROOT / 'artifacts/cloudflare-endpoint-checks.json'
    output.write_text(json.dumps({'checked_at': datetime.now(timezone.utc).isoformat(), 'environments': results}, indent=2) + '\n', encoding='utf-8')
    print('Six live endpoint checks passed; results: artifacts/cloudflare-endpoint-checks.json')


if __name__ == '__main__':
    main()
