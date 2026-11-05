#!/usr/bin/env python3
"""Create fresh, clearly named QA data in the local Stackr installation; never alter existing users."""
import json, secrets, subprocess, time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from smoke import request

stamp = 'qa_' + secrets.token_hex(4)
def user(kind):
    name = stamp + '_' + kind
    password = secrets.token_urlsafe(18)
    role = 'ORGANIZER' if kind in ('org', 'otherorg') else 'CONTESTANT'
    profile = request('POST', '/auth/register', {'username': name, 'email': name + '@example.test', 'password': password, 'role': role})
    if kind == 'admin':
        # Promote only the test user just created above, using its numeric returned ID.
        subprocess.run(['docker', 'compose', 'exec', '-T', 'postgres', 'psql', '-U', 'stackr', '-d', 'stackr_db', '-c',
                        f"UPDATE users SET role='ADMIN' WHERE id={int(profile['id'])}"], check=True, stdout=subprocess.DEVNULL)
    token = request('POST', '/auth/login', {'username': name, 'password': password})
    return {'name': name, 'password': password, 'token': token['accessToken'], 'refresh': token['refreshToken'], 'id': profile['id']}

users = {k: user(k) for k in ['org', 'otherorg', 'student', 'victim', 'admin', 'password', 'opponent']}
admin = users['admin']['token']; org = users['org']['token']
tag = request('POST', '/tags', {'name': stamp + '_arrays'}, admin)
def problem(suffix, published=True):
    p = request('POST', '/problems', {'title': stamp + ' ' + suffix, 'description': '## Сумма двух чисел\n\nПрочитайте два целых числа и выведите их сумму.',
        'difficulty': 'EASY', 'timeLimitMs': 3000, 'memoryLimitMb': 256, 'tagIds': [tag['id']]}, org)
    request('POST', f"/problems/{p['id']}/testcases", {'inputData': '2 3', 'expectedOutput': '5', 'isPublic': True, 'orderIndex': 0}, org)
    request('POST', f"/problems/{p['id']}/testcases", {'inputData': '-10 7', 'expectedOutput': '-3', 'isPublic': False, 'orderIndex': 1}, org)
    request('POST', f"/problems/{p['id']}/hints", {'text': 'QA: прочитайте оба числа одним Scanner', 'orderIndex': 0, 'unlocksAfterAttempts': 1}, org)
    if published: request('PATCH', f"/problems/{p['id']}/visibility?visible=true", token=org)
    return p
p = problem('Сумма'); draft = problem('Черновик', False)
now = datetime.now(timezone.utc)
def contest(suffix, public, upcoming):
    start = now + timedelta(hours=1) if upcoming else now - timedelta(minutes=2)
    return request('POST', '/contests', {'title': stamp + ' ' + suffix, 'description': 'Тестовый контест. Рейтинг: решённые задачи, затем штраф.',
        'startTime': start.isoformat(), 'endTime': (now + timedelta(hours=3)).isoformat(), 'problemIds': [p['id']], 'isPublic': public}, org)
active = contest('Открытый', True, False)
private = contest('Закрытый', False, False)
future = contest('Будущий', True, True)
state = {'stamp': stamp, 'users': users, 'problem': p, 'draft': draft, 'tag': tag, 'active': active, 'private': private, 'future': future}
path = Path(__file__).resolve().parent.parent / 'web/e2e/.state.json'
path.write_text(json.dumps(state, ensure_ascii=False, indent=2)); path.chmod(0o600)
print('QA fixture ready:', stamp, '; generated credentials stored locally in ignored e2e/.state.json')
