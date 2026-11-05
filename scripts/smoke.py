#!/usr/bin/env python3
"""Creates isolated demo users and a task, then verifies real HTTP judge verdicts.
Run after scripts/dev.sh. Intentionally leaves its demo records for inspection.
"""
import json
import os
import secrets
import time
import urllib.request

BASE = os.environ.get('STACKR_URL', 'http://localhost:3100').rstrip('/') + '/api'


def request(method, path, data=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(BASE + path, data=None if data is None else json.dumps(data).encode(),
                                 headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=130) as response:
        body = response.read()
        return json.loads(body) if body else None


def account(role):
    name = 'smoke_' + secrets.token_hex(5)
    password = secrets.token_urlsafe(20)
    request('POST', '/auth/register', {'username': name, 'email': name + '@example.test',
                                     'password': password, 'role': role})
    return request('POST', '/auth/login', {'username': name, 'password': password})['accessToken']


def main():
    organizer = account('ORGANIZER')
    contestant = account('CONTESTANT')
    problem = request('POST', '/problems', {
        'title': 'Smoke: answer 42', 'description': 'Print 42.', 'difficulty': 'EASY',
        'timeLimitMs': 3000, 'memoryLimitMb': 256,
    }, organizer)
    pid = problem['id']
    request('POST', f'/problems/{pid}/testcases', {
        'inputData': '', 'expectedOutput': '42', 'isPublic': False, 'orderIndex': 0,
    }, organizer)
    request('PATCH', f'/problems/{pid}/visibility?visible=true', token=organizer)
    cases = {
        'AC': 'System.out.println(42);',
        'WA': 'System.out.println(1);',
        'CE': 'syntax error',
        'RE': 'throw new RuntimeException();',
        'TLE': 'while(true) {}',
    }
    for expected, body in cases.items():
        source = 'public class Solution { public static void main(String[] a) { ' + body + ' } }'
        submission = request('POST', '/submissions', {'problemId': pid, 'sourceCode': source}, contestant)
        deadline = time.monotonic() + 90
        while submission['verdict'] == 'PENDING' and time.monotonic() < deadline:
            time.sleep(0.5)
            submission = request('GET', '/submissions/' + str(submission['id']), token=contestant)
        if submission['verdict'] != expected:
            raise RuntimeError(f"Expected {expected}, got {submission['verdict']} for submission {submission['id']}")
        print(expected + ': OK')
    history = request('GET', '/submissions/my?problemId=' + str(pid), token=contestant)
    if history['totalElements'] != 5:
        raise RuntimeError('Submission history is incomplete')
    print(f'HTTP smoke passed; demo problem ID: {pid}')


if __name__ == '__main__':
    main()
