#!/usr/bin/env python3
"""Verify each editorial Java solution through the real HTTP -> Docker judge."""
import json,time
from pathlib import Path
from smoke import request,account
root=Path(__file__).resolve().parent.parent
state=json.loads((root/'.demo-content.json').read_text())
token=account('CONTESTANT')
items=json.loads((root/'scripts/content/catalog.json').read_text())
for p in items:
 result=request('POST','/submissions',{'problemId':state['problems'][p['slug']],'sourceCode':p['reference']},token)
 deadline=time.monotonic()+120
 while time.monotonic()<deadline:
  result=request('GET',f"/submissions/{result['id']}",token=token)
  if result['verdict']!='PENDING':break
  time.sleep(.3)
 assert result['verdict']=='AC',(p['title'],result['verdict'],result.get('errorMessage'))
 print('AC:',p['title'],flush=True)
public=request('GET','/contests',token=token)
assert len(public)==1 and public[0]['id']==state['contestId'], public
contest=request('GET',f"/contests/{state['contestId']}",token=token)
assert len(contest['problems'])==5
catalog=request('GET','/problems?size=100',token=token)
assert all(not p['title'].startswith(('qa_','Smoke:')) for p in catalog['content'])
print('Verified all 20 tasks, 5-task training contest, clean public catalog.',flush=True)
