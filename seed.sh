#!/bin/bash
# Stackr seed: admin + 25 задач + 3 контеста
set -e
BASE="http://localhost:8080/api"
GREEN='\033[0;32m'; BLUE='\033[0;34m'; RED='\033[0;31m'; NC='\033[0m'
ok()  { echo -e "  ${GREEN}✓${NC} $1"; }
hdr() { echo -e "\n${BLUE}▶ $1${NC}"; }
err() { echo -e "  ${RED}✗${NC} $1"; }

echo ""
echo "╔══════════════════════════════════════╗"
echo "║     Stackr — заполнение базы данных  ║"
echo "╚══════════════════════════════════════╝"

# ── Проверяем jq ──────────────────────────────────────────────────────────────
if ! command -v jq &>/dev/null; then
  hdr "Устанавливаем jq..."
  brew install jq || { err "Установите jq вручную: brew install jq"; exit 1; }
fi

# ── Ждём бэкенд ───────────────────────────────────────────────────────────────
hdr "Ожидание бэкенда..."
for i in $(seq 1 30); do
  if curl -sf "$BASE/tags" -o /dev/null 2>&1 || \
     curl -sf "$BASE/auth/login" -X POST -H "Content-Type: application/json" -d '{}' -o /dev/null 2>&1; then
    ok "Бэкенд доступен"; break
  fi
  echo "  ожидание... ($i/30)"; sleep 3
done

# ── 1. Регистрируем admin ─────────────────────────────────────────────────────
hdr "Создаём пользователя admin..."
curl -sf -X POST "$BASE/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@stackr.io","password":"Admin123!","role":"CONTESTANT"}' \
  -o /dev/null 2>&1 || true
ok "Пользователь создан (или уже существует)"

hdr "Выставляем роль ADMIN в БД..."
docker-compose exec -T postgres \
  psql -U postgres -d stackr_db \
  -c "UPDATE users SET role='ADMIN' WHERE username='admin';" > /dev/null
ok "Роль ADMIN выставлена"

# ── 2. Логинимся ──────────────────────────────────────────────────────────────
hdr "Получаем токен..."
TOKEN=$(curl -sf -X POST "$BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin123!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
ok "Токен получен"

# ── Вспомогательные функции ───────────────────────────────────────────────────
# mkproblem TITLE DESC DIFF TL ML TEMPLATE → id
mkproblem() {
  jq -n \
    --arg title "$1" --arg desc "$2" --arg diff "$3" \
    --argjson tl "$4" --argjson ml "$5" --arg tmpl "$6" \
    '{title:$title,description:$desc,difficulty:$diff,timeLimitMs:$tl,memoryLimitMb:$ml,tagIds:[],codeTemplate:$tmpl}' \
  | curl -sf -X POST "$BASE/problems" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @- \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])"
}

# addtc PID INPUT EXPECTED IS_PUBLIC ORDER_INDEX
addtc() {
  local PUB="false"; [[ "$4" == "true" ]] && PUB="true"
  jq -n --arg inp "$2" --arg out "$3" --argjson pub "$PUB" --argjson idx "$5" \
    '{inputData:$inp,expectedOutput:$out,isPublic:$pub,orderIndex:$idx}' \
  | curl -sf -X POST "$BASE/problems/$1/testcases" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @- > /dev/null
}

publish() {
  curl -sf -X PATCH "$BASE/problems/$1/visibility?visible=true" \
    -H "Authorization: Bearer $TOKEN" > /dev/null
}

# mkcontest TITLE DESC START END "id1,id2,..."  → id
mkcontest() {
  jq -n \
    --arg title "$1" --arg desc "$2" \
    --arg start "$3" --arg end "$4" \
    --argjson ids "[$5]" \
    '{title:$title,description:$desc,startTime:$start,endTime:$end,problemIds:$ids,isPublic:true}' \
  | curl -sf -X POST "$BASE/contests" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d @- \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])"
}

# ═════════════════════════════════════════════════════════════════════════════
hdr "Создаём 25 задач..."

# ── 1 ── EASY: Сумма двух чисел ───────────────────────────────────────────────
echo -n "  1/25 Сумма двух чисел... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        long a = sc.nextLong(), b = sc.nextLong();
        System.out.println(a + b);
    }
}
EOF
)
P1=$(mkproblem "Сумма двух чисел" \
"## Задача
Даны два целых числа a и b. Выведите их сумму.

## Формат ввода
Два целых числа через пробел.

## Формат вывода
Одно целое число — сумма a и b.

## Ограничения
-10^9 <= a, b <= 10^9" \
"EASY" 1000 64 "$TMPL")
addtc $P1 "3 5" "8" "true" 0
addtc $P1 "100 200" "300" "false" 1
addtc $P1 "-5 10" "5" "false" 2
addtc $P1 "0 0" "0" "false" 3
addtc $P1 "1000000000 1000000000" "2000000000" "false" 4
publish $P1; ok "id=$P1"

# ── 2 ── EASY: Максимум из трёх ───────────────────────────────────────────────
echo -n "  2/25 Максимум из трёх... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int a = sc.nextInt(), b = sc.nextInt(), c = sc.nextInt();
        System.out.println(Math.max(a, Math.max(b, c)));
    }
}
EOF
)
P2=$(mkproblem "Максимум из трёх чисел" \
"## Задача
Даны три целых числа. Выведите наибольшее из них." \
"EASY" 1000 64 "$TMPL")
addtc $P2 "1 2 3" "3" "true" 0
addtc $P2 "10 5 8" "10" "false" 1
addtc $P2 "-1 -2 -3" "-1" "false" 2
addtc $P2 "7 7 7" "7" "false" 3
publish $P2; ok "id=$P2"

# ── 3 ── EASY: Чётное/нечётное ────────────────────────────────────────────────
echo -n "  3/25 Чётное или нечётное... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        long n = sc.nextLong();
        System.out.println(Math.abs(n) % 2 == 0 ? "EVEN" : "ODD");
    }
}
EOF
)
P3=$(mkproblem "Чётное или нечётное" \
"## Задача
Дано целое число. Определите, чётное оно или нечётное.

## Формат вывода
EVEN если чётное, ODD если нечётное." \
"EASY" 1000 64 "$TMPL")
addtc $P3 "4" "EVEN" "true" 0
addtc $P3 "7" "ODD" "false" 1
addtc $P3 "0" "EVEN" "false" 2
addtc $P3 "-3" "ODD" "false" 3
publish $P3; ok "id=$P3"

# ── 4 ── EASY: Сумма цифр ─────────────────────────────────────────────────────
echo -n "  4/25 Сумма цифр числа... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        String s = new Scanner(System.in).next();
        int sum = 0;
        for (char c : s.toCharArray()) sum += c - '0';
        System.out.println(sum);
    }
}
EOF
)
P4=$(mkproblem "Сумма цифр числа" \
"## Задача
Дано натуральное число. Найдите сумму его цифр.

## Ограничения
1 <= n <= 10^18" \
"EASY" 1000 64 "$TMPL")
addtc $P4 "123" "6" "true" 0
addtc $P4 "9999" "36" "false" 1
addtc $P4 "100" "1" "false" 2
addtc $P4 "1000000007" "8" "false" 3
publish $P4; ok "id=$P4"

# ── 5 ── EASY: Факториал ──────────────────────────────────────────────────────
echo -n "  5/25 Факториал... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();
        long f = 1;
        for (int i = 2; i <= n; i++) f *= i;
        System.out.println(f);
    }
}
EOF
)
P5=$(mkproblem "Факториал" \
"## Задача
Вычислите n! для заданного n.

## Ограничения
0 <= n <= 20" \
"EASY" 1000 64 "$TMPL")
addtc $P5 "5" "120" "true" 0
addtc $P5 "0" "1" "false" 1
addtc $P5 "10" "3628800" "false" 2
addtc $P5 "20" "2432902008176640000" "false" 3
publish $P5; ok "id=$P5"

# ── 6 ── EASY: Разворот строки ────────────────────────────────────────────────
echo -n "  6/25 Разворот строки... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        System.out.println(new StringBuilder(new Scanner(System.in).next()).reverse());
    }
}
EOF
)
P6=$(mkproblem "Разворот строки" \
"## Задача
Дана строка. Выведите её символы в обратном порядке.

## Ограничения
Длина строки до 100 000 символов, без пробелов." \
"EASY" 1000 128 "$TMPL")
addtc $P6 "hello" "olleh" "true" 0
addtc $P6 "abcde" "edcba" "false" 1
addtc $P6 "a" "a" "false" 2
addtc $P6 "racecar" "racecar" "false" 3
publish $P6; ok "id=$P6"

# ── 7 ── EASY: Минимум и максимум массива ─────────────────────────────────────
echo -n "  7/25 Мин и макс массива... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int n = Integer.parseInt(br.readLine().trim());
        StringTokenizer st = new StringTokenizer(br.readLine());
        long mn = Long.MAX_VALUE, mx = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long x = Long.parseLong(st.nextToken());
            if (x < mn) mn = x;
            if (x > mx) mx = x;
        }
        System.out.println(mn + " " + mx);
    }
}
EOF
)
P7=$(mkproblem "Минимум и максимум массива" \
"## Задача
Дан массив целых чисел. Найдите минимальный и максимальный элементы.

## Формат ввода
Первая строка — n (1 <= n <= 100000).
Вторая строка — n целых чисел через пробел.

## Формат вывода
Два числа через пробел: минимум и максимум." \
"EASY" 1000 128 "$TMPL")
addtc $P7 "5
3 1 4 1 5" "1 5" "true" 0
addtc $P7 "1
42" "42 42" "false" 1
addtc $P7 "4
-10 0 10 -5" "-10 10" "false" 2
publish $P7; ok "id=$P7"

# ── 8 ── EASY: Степень числа по модулю ───────────────────────────────────────
echo -n "  8/25 Степень числа (mod)... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    static final long MOD = 1_000_000_007;
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        long a = sc.nextLong(), b = sc.nextLong(), res = 1;
        a %= MOD;
        while (b > 0) {
            if ((b & 1) == 1) res = res * a % MOD;
            a = a * a % MOD; b >>= 1;
        }
        System.out.println(res);
    }
}
EOF
)
P8=$(mkproblem "Быстрое возведение в степень" \
"## Задача
Вычислите a^b mod (10^9+7).

## Формат ввода
Два числа a и b (0 <= a <= 10^9, 0 <= b <= 10^18)." \
"EASY" 1000 64 "$TMPL")
addtc $P8 "2 10" "1024" "true" 0
addtc $P8 "3 0" "1" "false" 1
addtc $P8 "2 1000000000000000000" "457701813" "false" 2
addtc $P8 "0 5" "0" "false" 3
publish $P8; ok "id=$P8"

# ── 9 ── EASY: Подсчёт слов ───────────────────────────────────────────────────
echo -n "  9/25 Подсчёт слов... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        String line = new Scanner(System.in).nextLine().trim();
        System.out.println(line.isEmpty() ? 0 : line.split("\\s+").length);
    }
}
EOF
)
P9=$(mkproblem "Подсчёт слов в строке" \
"## Задача
Дана строка. Посчитайте количество слов в ней (слова разделены пробелами)." \
"EASY" 1000 64 "$TMPL")
addtc $P9 "hello world" "2" "true" 0
addtc $P9 "one two three four five" "5" "false" 1
addtc $P9 "single" "1" "false" 2
publish $P9; ok "id=$P9"

# ── 10 ── EASY: Сортировка трёх чисел ────────────────────────────────────────
echo -n "  10/25 Сортировка трёх чисел... "
TMPL=$(cat <<'EOF'
import java.util.*;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int[] a = {sc.nextInt(), sc.nextInt(), sc.nextInt()};
        Arrays.sort(a);
        System.out.println(a[0] + " " + a[1] + " " + a[2]);
    }
}
EOF
)
P10=$(mkproblem "Сортировка трёх чисел" \
"## Задача
Даны три числа. Выведите их в неубывающем порядке." \
"EASY" 1000 64 "$TMPL")
addtc $P10 "3 1 2" "1 2 3" "true" 0
addtc $P10 "5 5 5" "5 5 5" "false" 1
addtc $P10 "-1 0 1" "-1 0 1" "false" 2
addtc $P10 "100 -100 0" "-100 0 100" "false" 3
publish $P10; ok "id=$P10"

# ── 11 ── MEDIUM: Числа Фибоначчи ─────────────────────────────────────────────
echo -n "  11/25 N-е число Фибоначчи... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    static final long MOD = 1_000_000_007;
    static long[][] mul(long[][] A, long[][] B) {
        long[][] C = new long[2][2];
        for (int i=0;i<2;i++) for (int k=0;k<2;k++) if(A[i][k]!=0)
            for (int j=0;j<2;j++) C[i][j]=(C[i][j]+A[i][k]*B[k][j])%MOD;
        return C;
    }
    static long[][] pow(long[][] M, long p) {
        long[][] R={{1,0},{0,1}};
        while(p>0){if((p&1)==1)R=mul(R,M);M=mul(M,M);p>>=1;}
        return R;
    }
    public static void main(String[] args) {
        long n = new Scanner(System.in).nextLong();
        if(n<=2){System.out.println(1);return;}
        long[][] M={{1,1},{1,0}};
        System.out.println(pow(M,n-1)[0][0]);
    }
}
EOF
)
P11=$(mkproblem "N-е число Фибоначчи" \
"## Задача
Найдите n-е число Фибоначчи по модулю 10^9+7.
F(1)=1, F(2)=1, F(n)=F(n-1)+F(n-2).

## Ограничения
1 <= n <= 10^18" \
"MEDIUM" 2000 128 "$TMPL")
addtc $P11 "1" "1" "true" 0
addtc $P11 "10" "55" "true" 1
addtc $P11 "50" "12586269025" "false" 2
addtc $P11 "1000000000000000000" "209783453" "false" 3
publish $P11; ok "id=$P11"

# ── 12 ── MEDIUM: Палиндром ───────────────────────────────────────────────────
echo -n "  12/25 Проверка палиндрома... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine().toLowerCase().replaceAll("[^a-z0-9]","");
        System.out.println(s.equals(new StringBuilder(s).reverse().toString()) ? "YES" : "NO");
    }
}
EOF
)
P12=$(mkproblem "Проверка палиндрома" \
"## Задача
Дана строка. Определите, является ли она палиндромом (игнорируя регистр и все символы, кроме букв и цифр).

## Формат вывода
YES или NO." \
"MEDIUM" 1000 128 "$TMPL")
addtc $P12 "racecar" "YES" "true" 0
addtc $P12 "hello" "NO" "false" 1
addtc $P12 "A man a plan a canal Panama" "YES" "false" 2
addtc $P12 "Was it a car or a cat I saw" "YES" "false" 3
publish $P12; ok "id=$P12"

# ── 13 ── MEDIUM: Бинарный поиск ──────────────────────────────────────────────
echo -n "  13/25 Бинарный поиск... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st = new StringTokenizer(br.readLine());
        int n = Integer.parseInt(st.nextToken());
        long x = Long.parseLong(st.nextToken());
        long[] a = new long[n];
        st = new StringTokenizer(br.readLine());
        for (int i=0;i<n;i++) a[i]=Long.parseLong(st.nextToken());
        int lo=0,hi=n-1,ans=-1;
        while(lo<=hi){int m=(lo+hi)/2;if(a[m]==x){ans=m;hi=m-1;}else if(a[m]<x)lo=m+1;else hi=m-1;}
        System.out.println(ans);
    }
}
EOF
)
P13=$(mkproblem "Бинарный поиск" \
"## Задача
Дан отсортированный массив и число x. Найдите индекс (с нуля) первого вхождения x, или -1 если не найдено.

## Формат ввода
Первая строка — n и x.
Вторая строка — n целых чисел в неубывающем порядке." \
"MEDIUM" 2000 256 "$TMPL")
addtc $P13 "5 3
1 2 3 4 5" "2" "true" 0
addtc $P13 "5 6
1 2 3 4 5" "-1" "false" 1
addtc $P13 "6 2
1 2 2 3 4 5" "1" "false" 2
addtc $P13 "1 1
1" "0" "false" 3
publish $P13; ok "id=$P13"

# ── 14 ── MEDIUM: Скобочная последовательность ────────────────────────────────
echo -n "  14/25 Скобочная последовательность... "
TMPL=$(cat <<'EOF'
import java.util.*;
public class Solution {
    public static void main(String[] args) {
        String s = new Scanner(System.in).nextLine();
        Deque<Character> st = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c=='('||c=='['||c=='{') st.push(c);
            else {
                if (st.isEmpty()){System.out.println("NO");return;}
                char t=st.pop();
                if(c==')'&&t!='('||c==']'&&t!='['||c=='}'&&t!='{'){System.out.println("NO");return;}
            }
        }
        System.out.println(st.isEmpty()?"YES":"NO");
    }
}
EOF
)
P14=$(mkproblem "Правильная скобочная последовательность" \
"## Задача
Дана строка из символов (, ), [, ], {, }.
Определите, является ли она правильной скобочной последовательностью.

## Формат вывода
YES или NO." \
"MEDIUM" 1000 128 "$TMPL")
addtc $P14 "()[]{}" "YES" "true" 0
addtc $P14 "([)]" "NO" "false" 1
addtc $P14 "{[]}" "YES" "false" 2
addtc $P14 "(((" "NO" "false" 3
publish $P14; ok "id=$P14"

# ── 15 ── MEDIUM: НОД и НОК ───────────────────────────────────────────────────
echo -n "  15/25 НОД и НОК... "
TMPL=$(cat <<'EOF'
import java.math.BigInteger;
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BigInteger a = sc.nextBigInteger(), b = sc.nextBigInteger();
        BigInteger g = a.gcd(b);
        System.out.println(g + " " + a.divide(g).multiply(b));
    }
}
EOF
)
P15=$(mkproblem "НОД и НОК" \
"## Задача
Даны два натуральных числа. Найдите их наибольший общий делитель (НОД) и наименьшее общее кратное (НОК).

## Формат ввода
Два натуральных числа a и b (1 <= a, b <= 10^18).

## Формат вывода
Два числа через пробел: НОД и НОК." \
"MEDIUM" 1000 64 "$TMPL")
addtc $P15 "12 18" "6 36" "true" 0
addtc $P15 "7 13" "1 91" "false" 1
addtc $P15 "100 75" "25 300" "false" 2
publish $P15; ok "id=$P15"

# ── 16 ── MEDIUM: Анаграммы ───────────────────────────────────────────────────
echo -n "  16/25 Анаграммы... "
TMPL=$(cat <<'EOF'
import java.util.*;
public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        char[] a = sc.nextLine().toLowerCase().toCharArray();
        char[] b = sc.nextLine().toLowerCase().toCharArray();
        Arrays.sort(a); Arrays.sort(b);
        System.out.println(Arrays.equals(a,b) ? "YES" : "NO");
    }
}
EOF
)
P16=$(mkproblem "Анаграммы" \
"## Задача
Даны две строки. Определите, являются ли они анаграммами (одни и те же символы в разном порядке, регистр игнорируется).

## Формат ввода
Две строки на отдельных строках.

## Формат вывода
YES или NO." \
"MEDIUM" 1000 128 "$TMPL")
addtc $P16 "listen
silent" "YES" "true" 0
addtc $P16 "hello
world" "NO" "false" 1
addtc $P16 "Triangle
Integral" "YES" "false" 2
addtc $P16 "abc
ab" "NO" "false" 3
publish $P16; ok "id=$P16"

# ── 17 ── MEDIUM: Простое число ───────────────────────────────────────────────
echo -n "  17/25 Простое число... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        long n = new Scanner(System.in).nextLong();
        if (n < 2) { System.out.println("NO"); return; }
        for (long i = 2; i * i <= n; i++)
            if (n % i == 0) { System.out.println("NO"); return; }
        System.out.println("YES");
    }
}
EOF
)
P17=$(mkproblem "Проверка простоты числа" \
"## Задача
Дано число n. Определите, простое ли оно.

## Ограничения
2 <= n <= 10^12

## Формат вывода
YES если простое, NO иначе." \
"MEDIUM" 2000 64 "$TMPL")
addtc $P17 "7" "YES" "true" 0
addtc $P17 "4" "NO" "false" 1
addtc $P17 "999999999989" "YES" "false" 2
addtc $P17 "2" "YES" "false" 3
publish $P17; ok "id=$P17"

# ── 18 ── MEDIUM: RLE-сжатие ──────────────────────────────────────────────────
echo -n "  18/25 RLE-сжатие строки... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        String s = new Scanner(System.in).next();
        StringBuilder sb = new StringBuilder();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i); int j = i;
            while (j < n && s.charAt(j) == c) j++;
            sb.append(c);
            if (j - i > 1) sb.append(j - i);
            i = j;
        }
        System.out.println(sb);
    }
}
EOF
)
P18=$(mkproblem "Сжатие строки (RLE)" \
"## Задача
Выполните RLE-сжатие строки: замените последовательности одинаковых символов на символ и его количество.
Если количество равно 1 — число не пишется.

## Пример
aaabbc -> a3b2c
abc -> abc

## Ограничения
Строка из латинских букв, длина до 100 000." \
"MEDIUM" 1000 128 "$TMPL")
addtc $P18 "aaabbc" "a3b2c" "true" 0
addtc $P18 "abc" "abc" "false" 1
addtc $P18 "aaaaaa" "a6" "false" 2
addtc $P18 "aabbccdd" "a2b2c2d2" "false" 3
publish $P18; ok "id=$P18"

# ── 19 ── MEDIUM: Количество инверсий ─────────────────────────────────────────
echo -n "  19/25 Количество инверсий... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    static long inv = 0;
    static void merge(int[] a, int l, int m, int r) {
        int[] tmp = new int[r-l]; int i=l,j=m,k=0;
        while(i<m&&j<r){if(a[i]<=a[j])tmp[k++]=a[i++];else{inv+=m-i;tmp[k++]=a[j++];}}
        while(i<m)tmp[k++]=a[i++]; while(j<r)tmp[k++]=a[j++];
        System.arraycopy(tmp,0,a,l,r-l);
    }
    static void sort(int[] a, int l, int r){if(r-l<2)return;int m=(l+r)/2;sort(a,l,m);sort(a,m,r);merge(a,l,m,r);}
    public static void main(String[] args) throws Exception {
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        int n=Integer.parseInt(br.readLine().trim());
        StringTokenizer st=new StringTokenizer(br.readLine());
        int[] a=new int[n]; for(int i=0;i<n;i++)a[i]=Integer.parseInt(st.nextToken());
        sort(a,0,n); System.out.println(inv);
    }
}
EOF
)
P19=$(mkproblem "Количество инверсий" \
"## Задача
Найдите количество инверсий в массиве — пар (i, j) таких, что i < j и a[i] > a[j].

## Формат ввода
Первая строка — n (1 <= n <= 100000).
Вторая строка — n целых чисел." \
"MEDIUM" 2000 256 "$TMPL")
addtc $P19 "4
3 1 2 4" "2" "true" 0
addtc $P19 "3
1 2 3" "0" "false" 1
addtc $P19 "3
3 2 1" "3" "false" 2
addtc $P19 "5
2 4 1 3 5" "3" "false" 3
publish $P19; ok "id=$P19"

# ── 20 ── MEDIUM: Спираль матрицы ─────────────────────────────────────────────
echo -n "  20/25 Обход матрицы по спирали... "
TMPL=$(cat <<'EOF'
import java.util.*;
public class Solution {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt(),m=sc.nextInt();
        int[][] a=new int[n][m];
        for(int i=0;i<n;i++) for(int j=0;j<m;j++) a[i][j]=sc.nextInt();
        int t=0,b=n-1,l=0,r=m-1;
        StringBuilder sb=new StringBuilder();
        while(t<=b&&l<=r){
            for(int j=l;j<=r;j++) sb.append(a[t][j]).append(' '); t++;
            for(int i=t;i<=b;i++) sb.append(a[i][r]).append(' '); r--;
            if(t<=b) for(int j=r;j>=l;j--) sb.append(a[b][j]).append(' '); b--;
            if(l<=r) for(int i=b;i>=t;i--) sb.append(a[i][l]).append(' '); l++;
        }
        System.out.println(sb.toString().trim());
    }
}
EOF
)
P20=$(mkproblem "Обход матрицы по спирали" \
"## Задача
Дана матрица n*m. Выведите её элементы в порядке обхода по спирали (по часовой стрелке, начиная с верхнего левого угла).

## Формат ввода
Первая строка — n и m (1 <= n, m <= 100).
Следующие n строк — по m чисел.

## Формат вывода
Все элементы через пробел в порядке спирального обхода." \
"MEDIUM" 1000 128 "$TMPL")
addtc $P20 "3 3
1 2 3
4 5 6
7 8 9" "1 2 3 6 9 8 7 4 5" "true" 0
addtc $P20 "1 4
1 2 3 4" "1 2 3 4" "false" 1
addtc $P20 "2 3
1 2 3
4 5 6" "1 2 3 6 5 4" "false" 2
publish $P20; ok "id=$P20"

# ── 21 ── HARD: Максимальный подмассив (Кадане) ───────────────────────────────
echo -n "  21/25 Макс сумма подмассива... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    public static void main(String[] args) throws Exception {
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        int n=Integer.parseInt(br.readLine().trim());
        StringTokenizer st=new StringTokenizer(br.readLine());
        long max=Long.MIN_VALUE,cur=0;
        for(int i=0;i<n;i++){long x=Long.parseLong(st.nextToken());cur+=x;if(cur>max)max=cur;if(cur<0)cur=0;}
        System.out.println(max);
    }
}
EOF
)
P21=$(mkproblem "Максимальная сумма подмассива" \
"## Задача
Дан массив целых чисел (может содержать отрицательные).
Найдите максимальную сумму непрерывного подмассива (алгоритм Кадане).

## Ограничения
1 <= n <= 10^6, -10^9 <= a[i] <= 10^9" \
"HARD" 2000 256 "$TMPL")
addtc $P21 "8
-2 -3 4 -1 -2 1 5 -3" "7" "true" 0
addtc $P21 "5
-2 1 -3 4 -1" "4" "true" 1
addtc $P21 "3
-5 -3 -1" "-1" "false" 2
addtc $P21 "4
1 2 3 4" "10" "false" 3
publish $P21; ok "id=$P21"

# ── 22 ── HARD: LCS ───────────────────────────────────────────────────────────
echo -n "  22/25 LCS (общая подпоследовательность)... "
TMPL=$(cat <<'EOF'
import java.util.Scanner;
public class Solution {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        String a=sc.nextLine(), b=sc.nextLine();
        int n=a.length(),m=b.length();
        int[][] dp=new int[n+1][m+1];
        for(int i=1;i<=n;i++) for(int j=1;j<=m;j++)
            dp[i][j]=a.charAt(i-1)==b.charAt(j-1)?dp[i-1][j-1]+1:Math.max(dp[i-1][j],dp[i][j-1]);
        System.out.println(dp[n][m]);
    }
}
EOF
)
P22=$(mkproblem "Наибольшая общая подпоследовательность (LCS)" \
"## Задача
Даны две строки. Найдите длину их наибольшей общей подпоследовательности.

## Формат ввода
Две строки на отдельных строках (только латинские буквы, длина до 1000 каждая)." \
"HARD" 3000 256 "$TMPL")
addtc $P22 "ABCBDAB
BDCAB" "4" "true" 0
addtc $P22 "abcde
ace" "3" "false" 1
addtc $P22 "abc
abc" "3" "false" 2
addtc $P22 "abc
def" "0" "false" 3
publish $P22; ok "id=$P22"

# ── 23 ── HARD: Рюкзак (0/1) ──────────────────────────────────────────────────
echo -n "  23/25 Задача о рюкзаке... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    public static void main(String[] args) throws Exception {
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st=new StringTokenizer(br.readLine());
        int n=Integer.parseInt(st.nextToken()),W=Integer.parseInt(st.nextToken());
        int[] w=new int[n],v=new int[n];
        for(int i=0;i<n;i++){st=new StringTokenizer(br.readLine());w[i]=Integer.parseInt(st.nextToken());v[i]=Integer.parseInt(st.nextToken());}
        int[] dp=new int[W+1];
        for(int i=0;i<n;i++) for(int j=W;j>=w[i];j--) dp[j]=Math.max(dp[j],dp[j-w[i]]+v[i]);
        System.out.println(dp[W]);
    }
}
EOF
)
P23=$(mkproblem "Задача о рюкзаке (0/1)" \
"## Задача
Дан рюкзак вместимостью W и n предметов, каждый с весом w_i и ценностью v_i.
Найдите максимальную суммарную ценность предметов.

## Формат ввода
Первая строка — n и W (1 <= n <= 100, 1 <= W <= 10000).
Следующие n строк — w_i и v_i." \
"HARD" 2000 256 "$TMPL")
addtc $P23 "3 50
10 60
20 100
30 120" "220" "true" 0
addtc $P23 "1 5
3 10" "10" "false" 1
addtc $P23 "1 2
3 10" "0" "false" 2
addtc $P23 "4 10
2 6
2 10
3 12
5 13" "32" "false" 3
publish $P23; ok "id=$P23"

# ── 24 ── HARD: Кратчайший путь BFS ──────────────────────────────────────────
echo -n "  24/25 Кратчайший путь (лабиринт)... "
TMPL=$(cat <<'EOF'
import java.util.*;
public class Solution {
    public static void main(String[] args) {
        Scanner sc=new Scanner(System.in);
        int n=sc.nextInt(),m=sc.nextInt(); sc.nextLine();
        char[][] g=new char[n][m];
        int sr=-1,sc2=-1,er=-1,ec=-1;
        for(int i=0;i<n;i++){
            g[i]=sc.nextLine().toCharArray();
            for(int j=0;j<m;j++){if(g[i][j]=='S'){sr=i;sc2=j;}if(g[i][j]=='E'){er=i;ec=j;}}
        }
        int[][] dist=new int[n][m]; for(int[]r:dist)Arrays.fill(r,-1);
        Queue<int[]> q=new LinkedList<>(); q.add(new int[]{sr,sc2}); dist[sr][sc2]=0;
        int[]dx={0,0,1,-1},dy={1,-1,0,0};
        while(!q.isEmpty()){int[]p=q.poll();for(int d=0;d<4;d++){int nx=p[0]+dx[d],ny=p[1]+dy[d];
            if(nx>=0&&nx<n&&ny>=0&&ny<m&&g[nx][ny]!='#'&&dist[nx][ny]==-1){dist[nx][ny]=dist[p[0]][p[1]]+1;q.add(new int[]{nx,ny});}}}
        System.out.println(dist[er][ec]);
    }
}
EOF
)
P24=$(mkproblem "Кратчайший путь в лабиринте" \
"## Задача
Дан лабиринт n*m. Клетка . — свободна, # — стена.
Найдите кратчайший путь (в шагах) от S до E, или -1 если пути нет.
Движение: вверх/вниз/влево/вправо.

## Формат ввода
Первая строка — n и m.
Следующие n строк — карта лабиринта." \
"HARD" 3000 256 "$TMPL")
addtc $P24 "3 5
S...E
#####
....." "-1" "true" 0
addtc $P24 "3 5
S....
.###.
....E" "8" "false" 1
addtc $P24 "1 3
S.E" "2" "false" 2
publish $P24; ok "id=$P24"

# ── 25 ── HARD: LIS ───────────────────────────────────────────────────────────
echo -n "  25/25 LIS (возрастающая подпоследовательность)... "
TMPL=$(cat <<'EOF'
import java.util.*;
import java.io.*;
public class Solution {
    public static void main(String[] args) throws Exception {
        BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
        int n=Integer.parseInt(br.readLine().trim());
        StringTokenizer st=new StringTokenizer(br.readLine());
        List<Integer> dp=new ArrayList<>();
        for(int i=0;i<n;i++){
            int x=Integer.parseInt(st.nextToken());
            int pos=Collections.binarySearch(dp,x);
            if(pos<0) pos=-(pos+1);
            if(pos==dp.size()) dp.add(x); else dp.set(pos,x);
        }
        System.out.println(dp.size());
    }
}
EOF
)
P25=$(mkproblem "Наибольшая возрастающая подпоследовательность (LIS)" \
"## Задача
Дан массив целых чисел. Найдите длину его наибольшей строго возрастающей подпоследовательности.

## Ограничения
1 <= n <= 100 000" \
"HARD" 2000 256 "$TMPL")
addtc $P25 "6
10 9 2 5 3 7" "3" "true" 0
addtc $P25 "8
0 1 0 3 2 3" "4" "false" 1
addtc $P25 "5
5 4 3 2 1" "1" "false" 2
addtc $P25 "4
1 2 3 4" "4" "false" 3
publish $P25; ok "id=$P25"

# ═════════════════════════════════════════════════════════════════════════════
hdr "Создаём 3 контеста..."

START1=$(python3 -c "from datetime import datetime,timedelta; print((datetime.now()-timedelta(minutes=5)).strftime('%Y-%m-%dT%H:%M:%S'))")
END1=$(python3  -c "from datetime import datetime,timedelta; print((datetime.now()+timedelta(hours=2)).strftime('%Y-%m-%dT%H:%M:%S'))")

echo -n "  1/3 Новичков #1 (RUNNING)... "
C1=$(mkcontest "Контест для новичков #1" \
  "Пять лёгких задач для тех, кто только начинает. Удачи!" \
  "$START1" "$END1" "$P1,$P2,$P3,$P5,$P6")
ok "id=$C1"

START2=$(python3 -c "from datetime import datetime,timedelta; print((datetime.now()+timedelta(minutes=30)).strftime('%Y-%m-%dT%H:%M:%S'))")
END2=$(python3  -c "from datetime import datetime,timedelta; print((datetime.now()+timedelta(hours=4)).strftime('%Y-%m-%dT%H:%M:%S'))")

echo -n "  2/3 Открытый чемпионат (UPCOMING)... "
C2=$(mkcontest "Открытый чемпионат Stackr" \
  "Смешанный контест: Easy + Medium + Hard. Покажи на что способен!" \
  "$START2" "$END2" "$P4,$P7,$P11,$P14,$P17,$P21,$P23")
ok "id=$C2"

START3=$(python3 -c "from datetime import datetime,timedelta; print((datetime.now()-timedelta(hours=5)).strftime('%Y-%m-%dT%H:%M:%S'))")
END3=$(python3  -c "from datetime import datetime,timedelta; print((datetime.now()-timedelta(hours=1)).strftime('%Y-%m-%dT%H:%M:%S'))")

echo -n "  3/3 Алгоритмический марафон (FINISHED)... "
C3=$(mkcontest "Алгоритмический марафон" \
  "Hard-only контест. Только для тех, кто уверен в своих силах." \
  "$START3" "$END3" "$P21,$P22,$P23,$P24,$P25")
ok "id=$C3"

# ═════════════════════════════════════════════════════════════════════════════
echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║                 ✅ Всё готово!                       ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  Логин: admin  |  Пароль: Admin123!                  ║"
echo "║  Сайт:  http://localhost:5173                         ║"
echo "╠══════════════════════════════════════════════════════╣"
echo "║  25 задач: 10 Easy, 10 Medium, 5 Hard                ║"
echo "║  Контест #$C1  — сейчас идёт     (RUNNING)          ║"
echo "║  Контест #$C2  — через 30 мин   (UPCOMING)          ║"
echo "║  Контест #$C3  — завершён       (FINISHED)          ║"
echo "╚══════════════════════════════════════════════════════╝"
