#!/usr/bin/env python3
"""Capture clean live UI in Chromium; local credentials never enter Git or stdout."""
from pathlib import Path
import os,subprocess,shutil
root=Path(__file__).resolve().parent.parent
os.chdir(root)
preview=root/'web/e2e/.demo-preview.json'
shutil.copy2(root/'.demo-content.json',preview);preview.chmod(0o600)
container=None
try:
 container=subprocess.check_output(['docker','create','--init','--ipc=host','--network','stackr-local_default','--workdir','/app','mcr.microsoft.com/playwright:v1.64.1-noble','node','/preview-demo.cjs'],text=True).strip()
 env=dict(os.environ,COPYFILE_DISABLE='1')
 tar=subprocess.Popen(['tar','--no-xattrs','-C','web','--exclude=test-results','--exclude=playwright-report','-cf','-','.'],stdout=subprocess.PIPE,env=env)
 subprocess.run(['docker','cp','-',container+':/app'],stdin=tar.stdout,check=True);tar.stdout.close();assert tar.wait()==0
 subprocess.run(['docker','cp','scripts/preview-demo.cjs',container+':/preview-demo.cjs'],check=True)
 subprocess.run(['docker','start','-a',container],check=True)
 status=int(subprocess.check_output(['docker','inspect','-f','{{.State.ExitCode}}',container],text=True));assert status==0
 out=root/'web/test-results/presentation'
 out.parent.mkdir(exist_ok=True)
 subprocess.run(['docker','cp',container+':/app/test-results/presentation',str(out.parent)],check=True)
 print('Screenshots saved to web/test-results/presentation')
finally:
 preview.unlink(missing_ok=True)
 if container:subprocess.run(['docker','rm','-f',container],stdout=subprocess.DEVNULL)
