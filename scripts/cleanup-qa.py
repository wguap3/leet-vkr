#!/usr/bin/env python3
"""Archive only generated QA/smoke resources; retain submissions and users."""
import subprocess
from pathlib import Path
if (Path(__file__).resolve().parent.parent / ".demo-validation.lock").exists():
    print("QA cleanup deferred until editorial verification finishes.")
    raise SystemExit(0)
subprocess.run(['docker','compose','exec','-T','postgres','psql','-v','ON_ERROR_STOP=1','-U','stackr','-d','stackr_db','-c',"""
BEGIN;
UPDATE problems SET visible=false WHERE author_id IN (SELECT id FROM users WHERE username ~ '^(qa_|smoke_)');
UPDATE contests SET is_public=false, end_time=LEAST(end_time,CURRENT_TIMESTAMP-INTERVAL '1 day'), status='FINISHED'
 WHERE organizer_id IN (SELECT id FROM users WHERE username ~ '^(qa_|smoke_)');
DELETE FROM problem_tags WHERE tag_id IN (SELECT id FROM tags WHERE name ~ '^qa_');
DELETE FROM tags WHERE name ~ '^qa_';
UPDATE users SET active=false WHERE username ~ '^(qa_|smoke_)';
COMMIT;
"""],check=True,stdout=subprocess.DEVNULL)
print('Generated QA resources archived; existing submissions retained.')
