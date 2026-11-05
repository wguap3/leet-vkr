"""Build reproducible test data. Execute only our own reference Java sources, never submissions."""
import json,random,subprocess,tempfile
from pathlib import Path
from problems import TASKS
rng=random.Random(20261105)
positive={9,10,11,13,14}
def oracle(k,a):
 n=len(a)
 if k==0:return max(sum(a[i:j]) for i in range(n) for j in range(i+1,n+1))
 if k==1:return max([0]+[a[j]-a[i] for i in range(n) for j in range(i+1,n)])
 if k==2:
  import math
  return [math.prod(a[:i]+a[i+1:]) for i in range(n)]
 if k==3:return [x for x in a if x]+[0]*a.count(0)
 if k==4:return sorted(set(a))
 if k==5:
  s=set(a);return max(next((j for j in range(1,n+1) if x+j not in s),n) for x in s)
 if k==6:return next(i for i in range(1,n+2) if i not in a)
 if k==7:return [next((v for v in a[i+1:] if v>x),-1) for i,x in enumerate(a)]
 if k==8:return [next((j-i for j in range(i+1,n) if a[j]>x),0) for i,x in enumerate(a)]
 if k==9:return sum(max(0,min(max(a[:i+1]),max(a[i:]))-a[i]) for i in range(n))
 if k==10:return max([0]+[(j-i)*min(a[i],a[j]) for i in range(n) for j in range(i+1,n)])
 if k==11:
  d=[0,0]
  for x in a:d.append(max(d[-1],d[-2]+x))
  return d[-1]
 if k==12:
  d=[1]*n
  for i in range(n):d[i]=1+max([0]+[d[j] for j in range(i) if a[j]<a[i]])
  return max(d)
 if k in (13,14):
  d=[n+1]*n;d[0]=0
  for i in range(n):
   for j in range(i+1,min(n,i+a[i]+1)):d[j]=min(d[j],d[i]+1)
  return ('YES' if d[-1]<=n else 'NO') if k==13 else (d[-1] if d[-1]<=n else -1)
 if k==15:return next((i for i in range(n) if sum(a[:i])==sum(a[i+1:])),-1)
 if k==16:return sum(sum(a[i:j])==0 for i in range(n) for j in range(i+1,n+1))
 if k==17:return sum(a[i]>a[j] for i in range(n) for j in range(i+1,n))
 if k==18:
  best=cur=1
  for i in range(1,n):cur=cur+1 if a[i]>a[i-1] else 1;best=max(best,cur)
  return best
 if k==19:return next((x for x in a if a.count(x)>n//2),'NONE')
def output(x):return ' '.join(map(str,x)) if isinstance(x,list) else str(x)
catalog=[]
with tempfile.TemporaryDirectory(prefix='stackr-editorial-') as temp:
 root=Path(temp)
 for k,(title,diff,tag,text,sample,body) in enumerate(TASKS):
  source='import java.util.*; public class Ref'+str(k)+' { public static void main(String[] args) {Scanner s=new Scanner(System.in);int n=s.nextInt();long[] a=new long[n];for(int i=0;i<n;i++)a[i]=s.nextLong();'+body+'}}'
  (root/f'Ref{k}.java').write_text(source)
 subprocess.run(['javac',*[str(p) for p in root.glob('*.java')]],check=True)
 for k,(title,diff,tag,text,sample,body) in enumerate(TASKS):
  cases=[sample,[0],[1],[1,1,1],[3,2,1],[0,0,0],[1,0,2,0,3]]
  if k not in positive:cases.extend([[-1,-2,-3],[-1,0,1],[-3,-3,2,2]])
  cases += [[rng.randint(0 if k in positive else -3,3) for _ in range(rng.randint(2,12))] for _ in range(8)]
  tests=[]
  for i,a in enumerate(cases):
   stdin=str(len(a))+'\n'+' '.join(map(str,a))+'\n';expected=output(oracle(k,a))
   actual=subprocess.check_output(['java','-cp',temp,f'Ref{k}'],input=stdin,text=True,timeout=5).strip()
   assert actual.split()==expected.split(),(title,a,expected,actual)
   tests.append({'inputData':stdin,'expectedOutput':expected,'isPublic':i<2,'orderIndex':i})
  maximum=12 if k==2 else 2000
  bounds='−3 ≤ a[i] ≤ 3' if k==2 else ('0 ≤ a[i] ≤ 10 000' if k in positive else '−10 000 ≤ a[i] ≤ 10 000')
  desc=f'## Условие\n\n{text}\n\n## Формат ввода\n\nВ первой строке записано n — количество элементов. Во второй строке записаны n целых чисел a[i], разделённых пробелами.\n\n## Формат вывода\n\nВыведите ответ на задачу. Если ответ — массив, выведите его элементы через пробел.\n\n## Ограничения\n\n- 1 ≤ n ≤ {maximum}\n- {bounds}\n- Для сумм и произведений используйте long.\n\n## Пример\n\nВвод:\n```text\n{tests[0]["inputData"]}```\nВывод:\n```text\n{tests[0]["expectedOutput"]}\n```\n'
  catalog.append({'slug':f'algorithm-{k+1:02d}','title':title,'difficulty':diff,'tag':tag,'description':desc,'tests':tests,'reference':'import java.util.*; public class Solution { public static void main(String[] args) {Scanner s=new Scanner(System.in);int n=s.nextInt();long[] a=new long[n];for(int i=0;i<n;i++)a[i]=s.nextLong();'+body+'}}'})
Path(__file__).with_name('catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2))
print('Validated Java reference against independent Python oracle:',len(catalog),'tasks;',sum(len(p['tests']) for p in catalog),'cases')
