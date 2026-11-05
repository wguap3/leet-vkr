#!/usr/bin/env python3
"""Idempotent local editorial content import; existing submissions are never deleted."""
import json,secrets,subprocess,urllib.error
from pathlib import Path
from datetime import datetime,timedelta,timezone
from smoke import request
root=Path(__file__).resolve().parent.parent
statefile=root/'.demo-content.json'
state=json.loads(statefile.read_text()) if statefile.exists() else {}
if not state:
 password=secrets.token_urlsafe(24)
 name='stackr_editor_'+secrets.token_hex(3)
 u=request('POST','/auth/register',{'username':name,'email':name+'@example.test','password':password,'role':'ORGANIZER'})
 state={'username':name,'password':password,'userId':u['id'],'problems':{}}
 statefile.write_text(json.dumps(state));statefile.chmod(0o600)
uid=int(state['userId'])
def sql(text):
 subprocess.run(['docker','compose','exec','-T','postgres','psql','-v','ON_ERROR_STOP=1','-U','stackr','-d','stackr_db','-c',text],check=True,stdout=subprocess.DEVNULL)
# Elevation only for the generated local editorial account, reverted even on failure.
sql(f"UPDATE users SET role='ADMIN' WHERE id={uid}")
try:
 token=request('POST','/auth/login',{'username':state['username'],'password':state['password']})['accessToken']
 tags={t['name']:t['id'] for t in request('GET','/tags')}
 catalog=json.loads((root/'scripts/content/catalog.json').read_text())
 for item in catalog:
  if item['tag'] not in tags:tags[item['tag']]=request('POST','/tags',{'name':item['tag']},token)['id']
  body={k:item[k] for k in ['title','description','difficulty']};body.update(timeLimitMs=3000,memoryLimitMb=256,tagIds=[tags[item['tag']]],codeTemplate='import java.util.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner in = new Scanner(System.in);\n        int n = in.nextInt();\n        long[] a = new long[n];\n        for (int i = 0; i < n; i++) a[i] = in.nextLong();\n        // Ваше решение\n    }\n}\n')
  pid=state['problems'].get(item['slug'])
  if pid:
   request('PUT',f'/problems/{pid}',body,token)
  else:
   pid=request('POST','/problems',body,token)['id'];state['problems'][item['slug']]=pid
   statefile.write_text(json.dumps(state));statefile.chmod(0o600)
   for test in item['tests']:request('POST',f'/problems/{pid}/testcases',test,token)
  request('PATCH',f'/problems/{pid}/visibility?visible=true',token=token)
 ids=[state['problems'][f'algorithm-{i:02d}'] for i in [2,4,8,13,10]]
 if not state.get('contestId'):
  c=request('POST','/contests',{'title':'Алгоритмический старт','description':'Бессрочная тренировка из пяти задач: массивы, два указателя, стек и динамическое программирование. Начните с простых задач и переходите к сложным. Можно присоединиться в любой момент. Место определяется числом решённых задач, затем штрафом; это тренировочная таблица, а не соревнование на скорость.','startTime':(datetime.now(timezone.utc)-timedelta(minutes=1)).isoformat(),'endTime':'9999-12-31T23:59:59Z','problemIds':ids,'isPublic':True},token)
  state['contestId']=c['id'];statefile.write_text(json.dumps(state));statefile.chmod(0o600)
 sql(f"""BEGIN;
 UPDATE problems SET visible=false WHERE author_id IN (SELECT id FROM users WHERE username ~ '^(qa_|smoke_)') OR title ~ '^(qa_|Smoke:)';
 UPDATE contests SET is_public=false, end_time=LEAST(end_time, CURRENT_TIMESTAMP - INTERVAL '1 day'), status='FINISHED' WHERE id<>{int(state['contestId'])} AND (is_public=true OR organizer_id IN (SELECT id FROM users WHERE username ~ '^(qa_|smoke_)'));
 DELETE FROM problem_tags WHERE tag_id IN (SELECT id FROM tags WHERE name ~ '^qa_');
 DELETE FROM tags WHERE name ~ '^qa_';
 COMMIT;""")
 print(f"Editorial catalog ready: {len(catalog)} tasks, one public training contest #{state['contestId']}. Existing submissions preserved.")
finally:sql(f"UPDATE users SET role='ORGANIZER' WHERE id={uid}")
