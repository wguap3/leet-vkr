#!/usr/bin/env python3
"""Local operational acceptance. Uses only QA users from prepare-e2e.py.
Temporarily stops this project's runner, always starts it again, then tests the queue quota.
"""
import concurrent.futures
import json
import subprocess
import time
import urllib.error
from pathlib import Path
from smoke import request

state = json.loads((Path(__file__).resolve().parent.parent / 'web/e2e/.state.json').read_text())
actor = state['users']['student']; token = actor['token']; pid = state['problem']['id']
code = 'public class Solution { public static void main(String[] a) { System.out.println(5); } }'

def wait(sid, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        s = request('GET', f'/submissions/{sid}', token=token)
        if s['verdict'] != 'PENDING': return s['verdict']
        time.sleep(.3)
    raise RuntimeError('Submission remained pending past deadline')

rating = request('GET', '/users/me', token=token)['rating']
try:
    subprocess.run(['docker', 'compose', 'stop', 'runner'], check=True, stdout=subprocess.DEVNULL)
    s = request('POST', '/submissions', {'problemId': pid, 'sourceCode': code}, token)
    assert wait(s['id']) == 'ERROR'
    assert request('GET', '/users/me', token=token)['rating'] == rating
    print('Runner outage: ERROR, rating preserved: OK', flush=True)
finally:
    subprocess.run(['docker', 'compose', 'up', '--detach', '--wait', 'runner'], check=True, stdout=subprocess.DEVNULL)

loop = 'public class Solution { public static void main(String[] a) { while(true) {} } }'
def submit(_):
    try:
        return 202, request('POST', '/submissions', {'problemId': pid, 'sourceCode': loop}, token)['id']
    except urllib.error.HTTPError as e:
        return e.code, None
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    results = list(pool.map(submit, range(6)))
assert sorted(status for status, _ in results) == [202, 202, 202, 202, 202, 422], results
for _, sid in results:
    if sid is not None:
        verdict = wait(sid)
        assert verdict == 'TLE', (sid, verdict)
correct = 'import java.util.*; public class Solution { public static void main(String[] a) { Scanner s = new Scanner(System.in); System.out.println(s.nextInt()+s.nextInt()); } }'
recovered = request('POST', '/submissions', {'problemId': pid, 'sourceCode': correct}, token)
assert wait(recovered['id']) == 'AC'
print('Runner recovery without restarting API: real AC: OK', flush=True)
print('Concurrent queue quota: 5 accepted, 1 rejected; all accepted jobs finalized: OK', flush=True)
